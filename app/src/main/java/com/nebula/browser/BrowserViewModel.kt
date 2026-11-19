package com.nebula.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.browser.browser.DownloadController
import com.nebula.browser.core.Bookmark
import com.nebula.browser.core.BookmarkRepository
import com.nebula.browser.core.BrowserSettings
import com.nebula.browser.core.BrowserTab
import com.nebula.browser.core.DefaultSearchEngines
import com.nebula.browser.core.DefaultWorkspaces
import com.nebula.browser.core.DownloadItem
import com.nebula.browser.core.ExtensionReport
import com.nebula.browser.core.HistoryEntry
import com.nebula.browser.core.HistoryRepository
import com.nebula.browser.core.NebulaDatabase
import com.nebula.browser.core.PasswordCredential
import com.nebula.browser.core.PasswordVault
import com.nebula.browser.core.SearchEngine
import com.nebula.browser.core.SecretStore
import com.nebula.browser.core.SettingsRepository
import com.nebula.browser.core.SmartQueryResolver
import com.nebula.browser.core.SmartResult
import com.nebula.browser.core.Workspace
import com.nebula.browser.engine.BrowserSession
import com.nebula.browser.engine.EngineEvent
import com.nebula.browser.engine.EnginePrivacyConfig
import com.nebula.browser.engine.WebViewEngine
import com.nebula.browser.feature.ai.AiAssistant
import com.nebula.browser.feature.extensions.ExtensionCompatibilityAnalyzer
import com.nebula.browser.util.bookmarksFromHtml
import com.nebula.browser.util.bookmarksToHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PendingAiRequest(
    val pageTitle: String,
    val pageUrl: String,
    val pageText: String,
    val question: String,
)

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTabId: String = "",
    val activeWorkspaceId: String = "personal",
    val workspaces: List<Workspace> = DefaultWorkspaces.all,
    val currentUrl: String = "",
    val currentTitle: String = "New tab",
    val progress: Int = 0,
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val desktopMode: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val downloads: List<DownloadItem> = emptyList(),
    val settings: BrowserSettings = BrowserSettings(),
    val passwords: List<PasswordCredential> = emptyList(),
    val extensionReports: List<ExtensionReport> = emptyList(),
    val blockedRequests: Int = 0,
    val readerVisible: Boolean = false,
    val readerText: String = "",
    val aiLoading: Boolean = false,
    val aiResponse: String = "",
    val aiConsent: PendingAiRequest? = null,
) {
    val activeTab: BrowserTab? get() = tabs.firstOrNull { it.id == activeTabId }
    val isHome: Boolean get() = currentUrl.isBlank()
    val engines: List<SearchEngine> get() = DefaultSearchEngines.all + settings.customSearchEngines
}

sealed interface UiEffect {
    data class Message(val text: String) : UiEffect
    data class OpenExternal(val intent: Intent) : UiEffect
    data class FileChooser(val request: EngineEvent.FileChooserRequested) : UiEffect
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NebulaDatabase.get(application)
    private val bookmarkRepository = BookmarkRepository(database.bookmarks())
    private val historyRepository = HistoryRepository(database.history())
    private val settingsRepository = SettingsRepository(application)
    private val secrets = SecretStore(application)
    private val vault = PasswordVault(secrets)
    private val downloadController = DownloadController(application, database)
    private val extensionAnalyzer = ExtensionCompatibilityAnalyzer(application)
    val aiAssistant = AiAssistant(secrets)

    private val engine = WebViewEngine(application) {
        val settings = _state.value.settings
        EnginePrivacyConfig(settings.adBlockEnabled, settings.blockThirdPartyCookies, settings.javaScriptEnabled)
    }
    private val sessions = mutableMapOf<String, BrowserSession>()
    private val recentlyClosed = mutableListOf<BrowserTab>()

    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()
    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _state.update { it.copy(settings = settings) }
                sessions.values.forEach { it.setJavaScriptEnabled(settings.javaScriptEnabled) }
            }
        }
        viewModelScope.launch { bookmarkRepository.bookmarks.collectLatest { list -> _state.update { it.copy(bookmarks = list) } } }
        viewModelScope.launch { historyRepository.history.collectLatest { list -> _state.update { it.copy(history = list) } } }
        viewModelScope.launch {
            downloadController.downloads.collectLatest { list ->
                _state.update { it.copy(downloads = list) }
            }
        }
        createTab()
        refreshDownloads()
    }

    fun activeSession(): BrowserSession? = sessions[_state.value.activeTabId]

    fun createTab(privateMode: Boolean = false, url: String = "", workspaceId: String = _state.value.activeWorkspaceId) {
        val tab = BrowserTab(isPrivate = privateMode, workspaceId = workspaceId)
        sessions[tab.id] = engine.createSession(tab.id, privateMode).also(::observeSession)
        _state.update {
            it.copy(
                tabs = it.tabs + tab,
                activeTabId = tab.id,
                currentUrl = "",
                currentTitle = if (privateMode) "Private tab" else "New tab",
                progress = 0,
                loading = false,
                canGoBack = false,
                canGoForward = false,
                desktopMode = false,
            )
        }
        if (url.isNotBlank()) navigate(url)
    }

    fun selectTab(id: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        val session = sessions[id]
        _state.update {
            it.copy(
                activeTabId = id,
                activeWorkspaceId = tab.workspaceId,
                currentUrl = tab.url,
                currentTitle = tab.title,
                canGoBack = session?.canGoBack == true,
                canGoForward = session?.canGoForward == true,
                readerVisible = false,
            )
        }
    }

    fun closeTab(id: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        sessions.remove(id)?.destroy()
        if (tab.url.isNotBlank()) recentlyClosed += tab
        val remaining = _state.value.tabs.filterNot { it.id == id }
        if (remaining.isEmpty()) {
            _state.update { it.copy(tabs = emptyList(), activeTabId = "") }
            createTab()
            return
        }
        val next = if (_state.value.activeTabId == id) remaining.last() else _state.value.activeTab
        _state.update { it.copy(tabs = remaining) }
        next?.let { selectTab(it.id) }
    }

    fun closeOtherTabs(id: String) {
        _state.value.tabs.filterNot { it.id == id || it.isPinned }.forEach { closeTab(it.id) }
        selectTab(id)
    }

    fun duplicateTab(id: String = _state.value.activeTabId) {
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        createTab(tab.isPrivate, tab.url, tab.workspaceId)
    }

    fun togglePin(id: String) {
        _state.update { state -> state.copy(tabs = state.tabs.map { if (it.id == id) it.copy(isPinned = !it.isPinned) else it }) }
    }

    fun restoreClosedTab() {
        val tab = recentlyClosed.removeLastOrNull()
        if (tab == null) message("No recently closed tabs") else createTab(tab.isPrivate, tab.url, tab.workspaceId)
    }

    fun selectWorkspace(id: String) {
        _state.update { it.copy(activeWorkspaceId = id) }
        _state.value.tabs.firstOrNull { it.workspaceId == id }?.let { selectTab(it.id) }
    }

    fun navigate(input: String) {
        val settings = _state.value.settings
        val resolver = SmartQueryResolver(_state.value.engines, settings.searchEngineId, settings.httpsFirst)
        runCatching { resolver.resolve(input) }
            .onSuccess { result ->
                when (result) {
                    is SmartResult.Navigate -> {
                        val id = _state.value.activeTabId
                        activeSession()?.loadUrl(result.url)
                        updateTab(id) { it.copy(url = result.url) }
                        _state.update { it.copy(currentUrl = result.url, loading = true, readerVisible = false) }
                    }
                    is SmartResult.Answer -> message(result.text)
                }
            }
            .onFailure { message(it.message ?: "Invalid address") }
    }

    fun goBack() { activeSession()?.goBack() }
    fun goForward() { activeSession()?.goForward() }
    fun reload() = activeSession()?.reload() ?: Unit
    fun stop() = activeSession()?.stopLoading() ?: Unit

    fun newHomeTab() = createTab()

    fun toggleDesktopMode() {
        val enabled = !_state.value.desktopMode
        activeSession()?.setDesktopMode(enabled)
        activeSession()?.reload()
        _state.update { it.copy(desktopMode = enabled) }
    }

    fun findInPage(query: String) {
        if (query.isBlank()) activeSession()?.clearFind() else activeSession()?.findInPage(query)
    }

    fun printPage() = activeSession()?.printPage() ?: Unit

    fun addBookmark(title: String = _state.value.currentTitle, url: String = _state.value.currentUrl, folder: String = "Bookmarks") {
        if (url.isBlank() || _state.value.activeTab?.isPrivate == true) return message("This page cannot be bookmarked")
        viewModelScope.launch {
            bookmarkRepository.save(Bookmark(title = title.ifBlank { url }, url = url, folder = folder))
            message("Bookmark saved")
        }
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch { bookmarkRepository.delete(id) }

    fun exportBookmarks(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                it.write(bookmarksToHtml(_state.value.bookmarks))
            } ?: error("Cannot create export file")
        }.onSuccess { message("Bookmarks exported") }.onFailure { message(it.message ?: "Export failed") }
    }

    fun importBookmarks(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val html = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Cannot open bookmark file")
            val imported = bookmarksFromHtml(html)
            require(imported.isNotEmpty()) { "No bookmarks found" }
            imported.forEach { bookmarkRepository.save(it) }
            imported.size
        }.onSuccess { message("$it bookmarks imported") }.onFailure { message(it.message ?: "Import failed") }
    }
    fun deleteHistory(id: Long) = viewModelScope.launch { historyRepository.delete(id) }
    fun clearHistory() = viewModelScope.launch {
        historyRepository.clear()
        message("Browsing history cleared")
    }

    fun updateSettings(transform: (BrowserSettings) -> BrowserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun addCustomSearchEngine(name: String, keyword: String, queryUrl: String) {
        if (name.isBlank() || keyword.isBlank() || !queryUrl.contains("%s")) {
            return message("Enter a name, keyword, and URL containing %s")
        }
        val id = "custom_${System.currentTimeMillis()}"
        updateSettings { it.copy(customSearchEngines = it.customSearchEngines + SearchEngine(id, name.trim(), queryUrl.trim(), keyword.trim())) }
    }

    fun requestDownload(url: String, fileName: String, mimeType: String? = null) {
        viewModelScope.launch {
            downloadController.enqueue(url, fileName, mimeType, null, _state.value.settings.downloadWifiOnly)
                .onSuccess { message("Download queued: $fileName") }
                .onFailure { message(it.message ?: "Download failed") }
        }
    }

    fun refreshDownloads() = viewModelScope.launch { runCatching { downloadController.refresh() } }
    fun cancelDownload(id: Long) = viewModelScope.launch { downloadController.cancel(id) }
    fun removeDownload(id: Long) = viewModelScope.launch { downloadController.remove(id) }
    fun retryDownload(item: DownloadItem) = viewModelScope.launch {
        downloadController.retry(item, _state.value.settings.downloadWifiOnly)
            .onSuccess { message("Download restarted") }
            .onFailure { message(it.message ?: "Could not retry") }
    }

    fun openDownload(id: Long) {
        val intent = downloadController.openIntent(id)
        if (intent == null) message("The downloaded file is not available") else _effects.tryEmit(UiEffect.OpenExternal(intent))
    }

    fun refreshPasswords() {
        _state.update { it.copy(passwords = vault.all().sortedByDescending(PasswordCredential::updatedAt)) }
    }

    fun savePassword(credential: PasswordCredential) {
        vault.save(credential)
        refreshPasswords()
        message("Credential encrypted and saved")
    }

    fun deletePassword(id: String) {
        vault.delete(id)
        refreshPasswords()
    }

    fun generatePassword(): String = vault.generatePassword()

    fun importExtension(uri: Uri, fileName: String) {
        viewModelScope.launch {
            extensionAnalyzer.analyze(uri, fileName)
                .onSuccess { report ->
                    _state.update { it.copy(extensionReports = listOf(report) + it.extensionReports) }
                    message("Compatibility report ready")
                }
                .onFailure { message(it.message ?: "Could not inspect extension") }
        }
    }

    fun clearExtensionReports() = _state.update { it.copy(extensionReports = emptyList()) }

    fun showReader() {
        extractPageText { text ->
            if (text.isNullOrBlank()) message("No readable article text found") else _state.update {
                it.copy(readerVisible = true, readerText = text)
            }
        }
    }

    fun hideReader() = _state.update { it.copy(readerVisible = false) }

    fun prepareAi(question: String) {
        val settings = _state.value.settings
        if (!settings.aiEnabled) return message("Enable AI and add your provider settings first")
        if (_state.value.activeTab?.isPrivate == true) return message("AI page sharing is disabled in private tabs")
        extractPageText { text ->
            if (text.isNullOrBlank()) message("No page text is available") else _state.update {
                it.copy(aiConsent = PendingAiRequest(it.currentTitle, it.currentUrl, text, question))
            }
        }
    }

    fun dismissAiConsent() = _state.update { it.copy(aiConsent = null) }

    fun confirmAiSend() {
        val pending = _state.value.aiConsent ?: return
        val settings = _state.value.settings
        _state.update { it.copy(aiConsent = null, aiLoading = true, aiResponse = "") }
        viewModelScope.launch {
            aiAssistant.askAboutPage(
                settings.aiEndpoint,
                settings.aiModel,
                settings.aiTemperature,
                pending.pageTitle,
                pending.pageUrl,
                pending.pageText,
                pending.question,
            ).onSuccess { answer -> _state.update { it.copy(aiLoading = false, aiResponse = answer) } }
                .onFailure { error ->
                    _state.update { it.copy(aiLoading = false) }
                    message(error.message ?: "AI request failed")
                }
        }
    }

    fun clearAiResponse() = _state.update { it.copy(aiResponse = "") }

    fun clearBrowsingData() {
        viewModelScope.launch {
            historyRepository.clear()
            sessions.values.forEach { it.destroy() }
            sessions.clear()
            engine.clearCache()
            _state.update { it.copy(tabs = emptyList(), activeTabId = "") }
            createTab()
            message("History and WebView sessions cleared")
        }
    }

    fun message(text: String) { _effects.tryEmit(UiEffect.Message(text)) }

    private fun observeSession(session: BrowserSession) {
        viewModelScope.launch {
            session.events.collectLatest { event ->
                if (eventTabId(event) != session.tabId) return@collectLatest
                when (event) {
                    is EngineEvent.UrlChanged -> {
                        updateTab(event.tabId) { it.copy(url = event.url) }
                        ifActive(event.tabId) { copy(currentUrl = event.url) }
                        val tab = _state.value.tabs.firstOrNull { it.id == event.tabId }
                        if (tab?.isPrivate == false) historyRepository.record(event.url, tab.title)
                    }
                    is EngineEvent.TitleChanged -> {
                        updateTab(event.tabId) { it.copy(title = event.title) }
                        ifActive(event.tabId) { copy(currentTitle = event.title) }
                    }
                    is EngineEvent.ProgressChanged -> ifActive(event.tabId) { copy(progress = event.progress) }
                    is EngineEvent.LoadingChanged -> ifActive(event.tabId) { copy(loading = event.loading) }
                    is EngineEvent.NavigationStateChanged -> ifActive(event.tabId) {
                        copy(canGoBack = event.canBack, canGoForward = event.canForward)
                    }
                    is EngineEvent.PageStarted -> {
                        updateTab(event.tabId) { it.copy(url = event.url) }
                        ifActive(event.tabId) { copy(currentUrl = event.url, loading = true) }
                    }
                    is EngineEvent.Error -> {
                        ifActive(event.tabId) { copy(loading = false) }
                        message(event.message)
                    }
                    is EngineEvent.DownloadRequested -> requestDownload(event.url, event.fileName, event.mimeType)
                    is EngineEvent.BlockedRequest -> _state.update { it.copy(blockedRequests = it.blockedRequests + 1) }
                    is EngineEvent.FileChooserRequested -> _effects.tryEmit(UiEffect.FileChooser(event))
                    is EngineEvent.ExternalRequest -> {
                        runCatching { Intent(Intent.ACTION_VIEW, Uri.parse(event.url)) }
                            .onSuccess { _effects.tryEmit(UiEffect.OpenExternal(it)) }
                            .onFailure { message("No app can open this link") }
                    }
                }
            }
        }
    }

    private fun eventTabId(event: EngineEvent): String = when (event) {
        is EngineEvent.UrlChanged -> event.tabId
        is EngineEvent.TitleChanged -> event.tabId
        is EngineEvent.ProgressChanged -> event.tabId
        is EngineEvent.LoadingChanged -> event.tabId
        is EngineEvent.NavigationStateChanged -> event.tabId
        is EngineEvent.PageStarted -> event.tabId
        is EngineEvent.Error -> event.tabId
        is EngineEvent.DownloadRequested -> event.tabId
        is EngineEvent.FileChooserRequested -> event.tabId
        is EngineEvent.ExternalRequest -> event.tabId
        is EngineEvent.BlockedRequest -> event.tabId
    }

    private fun extractPageText(callback: (String?) -> Unit) {
        activeSession()?.evaluateJavaScript(
            "(function(){return document.body ? document.body.innerText.slice(0,60000) : '';})()",
            callback,
        ) ?: callback(null)
    }

    private fun updateTab(id: String, transform: (BrowserTab) -> BrowserTab) {
        _state.update { state -> state.copy(tabs = state.tabs.map { if (it.id == id) transform(it) else it }) }
    }

    private fun ifActive(id: String, transform: BrowserUiState.() -> BrowserUiState) {
        if (_state.value.activeTabId == id) _state.update(transform)
    }

    override fun onCleared() {
        sessions.values.forEach { it.destroy() }
        sessions.clear()
        super.onCleared()
    }
}
