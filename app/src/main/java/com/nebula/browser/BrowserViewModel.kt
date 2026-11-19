package com.nebula.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.os.Environment
import java.io.File
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
import com.nebula.browser.core.HomeShortcut
import com.nebula.browser.core.TabRepository
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
import com.nebula.browser.engine.SitePermissionRequest
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
    val pendingPermission: SitePermissionRequest? = null,
    val developerOutput: String = "",
    val consoleMessages: List<String> = emptyList(),
    val openedFile: DownloadItem? = null,
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
    private val tabRepository = TabRepository(database.tabs())
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
                WebView.setWebContentsDebuggingEnabled(settings.developerMode)
            }
        }
        viewModelScope.launch { bookmarkRepository.bookmarks.collectLatest { list -> _state.update { it.copy(bookmarks = list) } } }
        viewModelScope.launch { historyRepository.history.collectLatest { list -> _state.update { it.copy(history = list) } } }
        viewModelScope.launch {
            downloadController.downloads.collectLatest { list ->
                _state.update { it.copy(downloads = list) }
            }
        }
        restoreSavedTabs()
        refreshDownloads()
    }

    fun activeSession(): BrowserSession? = sessions[_state.value.activeTabId]

    fun createTab(privateMode: Boolean = false, url: String = "", workspaceId: String = _state.value.activeWorkspaceId) {
        val tab = BrowserTab(isPrivate = privateMode, workspaceId = workspaceId)
        viewModelScope.launch(Dispatchers.IO) { tabRepository.save(tab) }
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
        if (_state.value.activeTabId != id) persistSession(_state.value.activeTabId)
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        val session = sessions[id]
        updateTab(id) { it.copy(lastOpenedAt = System.currentTimeMillis()) }
        _state.update {
            it.copy(
                activeTabId = id,
                activeWorkspaceId = tab.workspaceId,
                currentUrl = tab.url,
                currentTitle = tab.title,
                canGoBack = session?.canGoBack == true,
                canGoForward = session?.canGoForward == true,
                desktopMode = tab.desktopMode,
                readerVisible = false,
            )
        }
    }

    fun closeTab(id: String) {
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        sessions.remove(id)?.destroy()
        sessionFile(id).delete()
        viewModelScope.launch(Dispatchers.IO) { tabRepository.delete(id) }
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
        _state.value.activeTabId.takeIf { it.isNotBlank() }?.let { id -> updateTab(id) { it.copy(desktopMode = enabled) } }
        _state.update { it.copy(desktopMode = enabled) }
    }

    fun renameTab(id: String, title: String) {
        if (title.isNotBlank()) updateTab(id) { it.copy(title = title.trim().take(80)) }
    }

    fun setTabGroup(id: String, group: String?) {
        updateTab(id) { it.copy(groupName = group?.trim()?.take(40)?.ifBlank { null }) }
    }

    fun moveTabToWorkspace(id: String, workspaceId: String) {
        updateTab(id) { it.copy(workspaceId = workspaceId) }
    }

    fun closeAllInWorkspace() {
        val workspace = _state.value.activeWorkspaceId
        _state.value.tabs.filter { it.workspaceId == workspace && !it.isPinned }.toList().forEach { closeTab(it.id) }
    }

    fun addShortcut(title: String, url: String) {
        if (title.isBlank() || !(url.startsWith("http://") || url.startsWith("https://"))) return message("Enter a title and a valid HTTP(S) URL")
        val shortcut = HomeShortcut("custom_${System.currentTimeMillis()}", title.trim(), url, title.trim().take(1).uppercase())
        updateSettings { it.copy(shortcuts = it.shortcuts + shortcut) }
    }

    fun removeShortcut(id: String) = updateSettings { it.copy(shortcuts = it.shortcuts.filterNot { shortcut -> shortcut.id == id }) }
    fun moveShortcut(id: String, delta: Int) = updateSettings { settings ->
        val index = settings.shortcuts.indexOfFirst { it.id == id }
        val target = (index + delta).coerceIn(0, settings.shortcuts.lastIndex)
        if (index < 0) settings else {
            val mutable = settings.shortcuts.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            settings.copy(shortcuts = mutable)
        }
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
        val item = _state.value.downloads.firstOrNull { it.id == id } ?: return
        if (item.fileName.substringAfterLast('.', "").lowercase() in BUILT_IN_FILE_TYPES) {
            _state.update { it.copy(openedFile = item) }
        } else openDownloadExternally(id)
    }

    fun openDownloadExternally(id: Long) = viewModelScope.launch {
        val intent = downloadController.openIntent(id)
        if (intent == null) message("The downloaded file is not available") else _effects.tryEmit(UiEffect.OpenExternal(intent))
    }

    fun shareDownload(id: Long) = viewModelScope.launch {
        val intent = downloadController.shareIntent(id)
        if (intent == null) message("The downloaded file is not available") else _effects.tryEmit(UiEffect.OpenExternal(intent))
    }

    fun closeFileViewer() = _state.update { it.copy(openedFile = null) }

    fun resolveSitePermission(granted: Boolean) {
        val request = _state.value.pendingPermission ?: return
        if (granted) request.grant() else request.deny()
        _state.update { it.copy(pendingPermission = null) }
    }

    fun runDeveloperScript(script: String) {
        if (!_state.value.settings.developerMode) return message("Enable Developer Mode first")
        activeSession()?.evaluateJavaScript(script) { result ->
            _state.update { it.copy(developerOutput = result ?: "No return value") }
        }
    }

    fun viewPageSource() {
        val url = _state.value.currentUrl
        if (url.startsWith("http")) navigate("view-source://$url")
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
            tabRepository.clear()
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
                    is EngineEvent.LoadingChanged -> {
                        ifActive(event.tabId) { copy(loading = event.loading) }
                        if (!event.loading) persistSession(event.tabId)
                    }
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
                    is EngineEvent.SitePermissionRequested -> _state.update { it.copy(pendingPermission = event.request) }
                    is EngineEvent.ConsoleMessage -> if (_state.value.settings.developerMode) _state.update {
                        it.copy(consoleMessages = (it.consoleMessages + "${event.source}:${event.line} — ${event.message}").takeLast(100))
                    }
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
        is EngineEvent.SitePermissionRequested -> event.request.tabId
        is EngineEvent.ConsoleMessage -> event.tabId
    }

    private fun extractPageText(callback: (String?) -> Unit) {
        activeSession()?.evaluateJavaScript(
            "(function(){return document.body ? document.body.innerText.slice(0,60000) : '';})()",
            callback,
        ) ?: callback(null)
    }

    private fun updateTab(id: String, transform: (BrowserTab) -> BrowserTab) {
        _state.update { state -> state.copy(tabs = state.tabs.map {
            if (it.id == id) transform(it).also { updated -> viewModelScope.launch(Dispatchers.IO) { tabRepository.save(updated) } } else it
        }) }
    }

    private fun restoreSavedTabs() {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { tabRepository.savedTabs() }.sortedBy { it.createdAt }
            if (saved.isEmpty()) return@launch createTab()
            saved.forEach { tab ->
                val session = engine.createSession(tab.id, false).also(::observeSession)
                sessions[tab.id] = session
                session.setDesktopMode(tab.desktopMode)
                val stateFile = sessionFile(tab.id)
                if (stateFile.exists() && stateFile.length() in 1..12 * 1024 * 1024) session.restoreState(stateFile.readBytes())
                else if (tab.url.isNotBlank()) session.loadUrl(tab.url)
            }
            _state.update { it.copy(tabs = saved) }
            saved.maxByOrNull { it.lastOpenedAt }?.let { selectTab(it.id) } ?: saved.firstOrNull()?.let { selectTab(it.id) }
        }
    }

    fun persistSessions() {
        sessions.keys.toList().forEach(::persistSession)
        sessions.values.forEach { it.flushCookies() }
    }

    private fun persistSession(id: String) {
        val session = sessions[id] ?: return
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        if (tab.isPrivate) return
        runCatching {
            session.saveState()?.let { bytes ->
                if (bytes.size <= 12 * 1024 * 1024) {
                    sessionFile(id).parentFile?.mkdirs()
                    sessionFile(id).writeBytes(bytes)
                }
            }
            session.capturePreview()?.let { bitmap ->
                val preview = previewFile(id)
                preview.parentFile?.mkdirs()
                preview.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 72, it) }
                if (tab.previewPath != preview.absolutePath) updateTab(id) { it.copy(previewPath = preview.absolutePath) }
            }
        }
        val current = _state.value.tabs.firstOrNull { it.id == id } ?: tab
        viewModelScope.launch(Dispatchers.IO) { tabRepository.save(current.copy(lastOpenedAt = if (id == _state.value.activeTabId) System.currentTimeMillis() else current.lastOpenedAt)) }
    }

    private fun sessionFile(id: String) = File(getApplication<Application>().filesDir, "sessions/$id.state")
    private fun previewFile(id: String) = File(getApplication<Application>().filesDir, "previews/$id.webp")

    private fun ifActive(id: String, transform: BrowserUiState.() -> BrowserUiState) {
        if (_state.value.activeTabId == id) _state.update(transform)
    }

    override fun onCleared() {
        sessions.values.forEach { it.flushCookies() }
        sessions.values.forEach { it.destroy() }
        sessions.clear()
        super.onCleared()
    }

    private companion object {
        val BUILT_IN_FILE_TYPES = setOf("txt", "log", "json", "xml", "csv", "md", "html", "htm", "css", "js", "kt", "java", "py", "jpg", "jpeg", "png", "gif", "webp", "bmp", "pdf", "zip", "mp3", "wav", "ogg", "m4a", "mp4", "webm", "mkv", "3gp")
    }
}
