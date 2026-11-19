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
import com.nebula.browser.core.SearchQuery
import com.nebula.browser.core.SearchRepository
import com.nebula.browser.core.UserScript
import com.nebula.browser.core.UserScriptRepository
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
import com.nebula.browser.feature.ai.AgentAction
import com.nebula.browser.feature.ai.AgentStep
import com.nebula.browser.feature.ai.AgentStepStatus
import com.nebula.browser.feature.ai.ReportExporter
import com.nebula.browser.feature.extensions.ExtensionCompatibilityAnalyzer
import com.nebula.browser.feature.extensions.UserScriptParser
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
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
    val recentSearches: List<SearchQuery> = emptyList(),
    val userScripts: List<UserScript> = emptyList(),
    val toolbarVisible: Boolean = true,
    val agentPlanning: Boolean = false,
    val agentRunning: Boolean = false,
    val agentPlan: List<AgentAction> = emptyList(),
    val agentSteps: List<AgentStep> = emptyList(),
    val agentProgress: Float = 0f,
    val agentReport: String = "",
) {
    val activeTab: BrowserTab? get() = tabs.firstOrNull { it.id == activeTabId }
    val isHome: Boolean get() = currentUrl.isBlank()
    val engines: List<SearchEngine> get() = DefaultSearchEngines.all + settings.customSearchEngines
}

sealed interface UiEffect {
    data class Message(val text: String) : UiEffect
    data class OpenExternal(val intent: Intent) : UiEffect
    data class FileChooser(val request: EngineEvent.FileChooserRequested) : UiEffect
    data object RequestDownloadNotifications : UiEffect
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NebulaDatabase.get(application)
    private val bookmarkRepository = BookmarkRepository(database.bookmarks())
    private val historyRepository = HistoryRepository(database.history())
    private val tabRepository = TabRepository(database.tabs())
    private val searchRepository = SearchRepository(database.searchQueries())
    private val userScriptRepository = UserScriptRepository(database.userScripts())
    private val settingsRepository = SettingsRepository(application)
    private val secrets = SecretStore(application)
    private val vault = PasswordVault(secrets)
    private val downloadController = DownloadController(application, database)
    private val extensionAnalyzer = ExtensionCompatibilityAnalyzer(application)
    val aiAssistant = AiAssistant(secrets)

    private val engine = WebViewEngine(context = application, privacyConfig = {
        val settings = _state.value.settings
        EnginePrivacyConfig(settings.adBlockEnabled, settings.blockThirdPartyCookies, settings.javaScriptEnabled)
    }, scriptProvider = { _state.value.userScripts })
    private val sessions = mutableMapOf<String, BrowserSession>()
    private val recentlyClosed = mutableListOf<BrowserTab>()
    private var agentJob: Job? = null

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
        viewModelScope.launch { searchRepository.topSearches.collectLatest { list -> _state.update { it.copy(recentSearches = list) } } }
        viewModelScope.launch { userScriptRepository.scripts.collectLatest { list -> _state.update { it.copy(userScripts = list) } } }
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
                        if (!input.trim().startsWith("http", true) && (!input.trim().contains('.') || input.contains(' ') || input.trim().startsWith("view-source:"))) {
                            viewModelScope.launch(Dispatchers.IO) { searchRepository.record(input) }
                        }
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

    fun groupTabsTogether(sourceId: String, targetId: String) {
        if (sourceId == targetId) return
        val target = _state.value.tabs.firstOrNull { it.id == targetId } ?: return
        val group = target.groupName ?: "Group ${target.title.take(16).ifBlank { "Tabs" }}"
        updateTab(sourceId) { it.copy(groupName = group, workspaceId = target.workspaceId) }
        updateTab(targetId) { it.copy(groupName = group) }
        message("Tabs grouped in $group")
    }

    fun createTabInActiveGroup() {
        val group = _state.value.activeTab?.groupName ?: return createTab()
        createTab()
        setTabGroup(_state.value.activeTabId, group)
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

    fun addBookmark(title: String = _state.value.currentTitle, url: String = _state.value.currentUrl, folder: String = "Bookmarks", tags: String = "", notes: String = "") {
        if (url.isBlank() || _state.value.activeTab?.isPrivate == true) return message("This page cannot be bookmarked")
        viewModelScope.launch {
            bookmarkRepository.save(Bookmark(title = title.ifBlank { url }, url = url, folder = folder.ifBlank { "Bookmarks" }, tags = tags, notes = notes))
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

    fun clearSearchHistory() = viewModelScope.launch { searchRepository.clear(); message("Search suggestions cleared") }
    fun deleteSearchQuery(query: String) = viewModelScope.launch { searchRepository.delete(query) }

    fun importUserScript(uri: Uri, fileName: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val code = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Cannot open userscript")
            UserScriptParser.parse(code, fileName)
        }.onSuccess { script ->
            userScriptRepository.save(script)
            message(if (script.enabled) "Userscript installed for its declared sites" else "Userscript saved disabled: unsupported GM grants require review")
        }.onFailure { message(it.message ?: "Invalid userscript") }
    }

    fun toggleUserScript(script: UserScript) = viewModelScope.launch(Dispatchers.IO) {
        if (!script.enabled && script.grants.any { it != "none" }) return@launch message("Only @grant none userscripts can run in this version")
        userScriptRepository.save(script.copy(enabled = !script.enabled))
    }

    fun deleteUserScript(id: String) = viewModelScope.launch(Dispatchers.IO) { userScriptRepository.delete(id) }

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
        if (_state.value.settings.downloadNotifications && android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _effects.tryEmit(UiEffect.RequestDownloadNotifications)
        }
        viewModelScope.launch {
            downloadController.enqueue(url, fileName, mimeType, null, _state.value.settings.downloadWifiOnly, _state.value.settings.downloadNotifications)
                .onSuccess { message("Download queued: $fileName") }
                .onFailure { message(it.message ?: "Download failed") }
        }
    }

    fun refreshDownloads() = viewModelScope.launch { runCatching { downloadController.refresh() } }
    fun cancelDownload(id: Long) = viewModelScope.launch { downloadController.cancel(id) }
    fun removeDownload(id: Long) = viewModelScope.launch { downloadController.remove(id) }
    fun retryDownload(item: DownloadItem) = viewModelScope.launch {
        downloadController.retry(item, _state.value.settings.downloadWifiOnly, _state.value.settings.downloadNotifications)
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

    fun startAgent(task: String) {
        if (agentJob?.isActive == true || _state.value.agentPlanning) return message("An agent task is already running")
        val settings = _state.value.settings
        if (!settings.aiEnabled) return message("Enable AI and configure your provider first")
        _state.update { it.copy(agentPlanning = true, agentPlan = emptyList(), agentSteps = emptyList(), agentReport = "") }
        viewModelScope.launch {
            aiAssistant.createBrowserPlan(settings.aiEndpoint, settings.aiModel, settings.aiTemperature, task)
                .onSuccess { plan ->
                    if (plan.isEmpty()) {
                        _state.update { it.copy(agentPlanning = false) }; message("The planner returned no safe steps")
                    } else _state.update { it.copy(agentPlanning = false, agentPlan = plan) }
                }.onFailure { error ->
                    _state.update { it.copy(agentPlanning = false) }
                    message(error.message ?: "Agent planning failed")
                }
        }
    }

    fun dismissAgentPlan() = _state.update { it.copy(agentPlan = emptyList()) }

    fun confirmAgentPlan(task: String) {
        val plan = _state.value.agentPlan
        if (plan.isEmpty() || _state.value.agentRunning) return
        val steps = plan.mapIndexed { index, action -> AgentStep("step_$index", action.title) }
        _state.update { it.copy(agentPlan = emptyList(), agentRunning = true, agentSteps = steps, agentProgress = 0f, agentReport = "") }
        agentJob = viewModelScope.launch { executeAgent(task, plan, steps) }
    }

    fun cancelAgent() {
        agentJob?.cancel()
        _state.update { state -> state.copy(agentRunning = false, agentSteps = state.agentSteps.map {
            if (it.status == AgentStepStatus.RUNNING) it.copy(status = AgentStepStatus.CANCELLED, detail = "Cancelled by user") else it
        }) }
    }

    private suspend fun executeAgent(task: String, plan: List<AgentAction>, steps: List<AgentStep>) {
        val report = StringBuilder("Nebula safe browser agent
Created: ${java.util.Date()}
Task: $task

")
        val sourceTabs = mutableListOf<String>()
        try {
            plan.forEachIndexed { index, action ->
                val step = steps[index]
                updateAgentStep(step.id, AgentStepStatus.RUNNING, "Starting")
                when (action.action) {
                    "open_urls" -> {
                        action.urls.take(12).forEachIndexed { urlIndex, url ->
                            updateAgentStep(step.id, AgentStepStatus.RUNNING, "Opening ${urlIndex + 1}/${action.urls.size}: $url")
                            createTab(url = url)
                            sourceTabs += _state.value.activeTabId
                            var waitCycles = 0
                            while (_state.value.loading && waitCycles++ < 16) delay(500)
                            delay(1_200)
                        }
                    }
                    "go_back" -> { goBack(); delay(1_500); report.append("Went back in browser history.
") }
                    "collect_pages" -> {
                        val targets = if (sourceTabs.isEmpty()) listOf(_state.value.activeTabId) else sourceTabs.toList()
                        targets.forEachIndexed { tabIndex, tabId ->
                            selectTab(tabId)
                            updateAgentStep(step.id, AgentStepStatus.RUNNING, "Collecting ${tabIndex + 1}/${targets.size}")
                            val text = awaitActivePageText()
                            val tab = _state.value.tabs.firstOrNull { it.id == tabId }
                            report.append("
=== SOURCE ${tabIndex + 1}: ${tab?.title.orEmpty()} ===
${tab?.url.orEmpty()}
")
                            report.append(text.orEmpty().ifBlank { "[No readable text was available]" }.take(10_000)).append('
')
                            updateAgentStep(step.id, AgentStepStatus.RUNNING, "Collected ${tabIndex + 1}/${targets.size}")
                        }
                    }
                    "export_text", "export_pdf" -> {
                        updateAgentStep(step.id, AgentStepStatus.RUNNING, "Writing to Download/Nebula")
                        val name = withContext(Dispatchers.IO) {
                            if (action.action == "export_pdf") ReportExporter.exportPdf(getApplication(), task, report.toString())
                            else ReportExporter.exportText(getApplication(), task, report.toString())
                        }
                        report.append("
Exported: $name
")
                        message("Report saved: Download/Nebula/$name")
                    }
                }
                updateAgentStep(step.id, AgentStepStatus.COMPLETE, "Completed")
                _state.update { it.copy(agentProgress = (index + 1).toFloat() / plan.size, agentReport = report.toString()) }
            }
            _state.update { it.copy(agentRunning = false, agentProgress = 1f, agentReport = report.toString()) }
            message("Agent task completed")
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            val running = _state.value.agentSteps.firstOrNull { it.status == AgentStepStatus.RUNNING }
            running?.let { updateAgentStep(it.id, AgentStepStatus.FAILED, error.message ?: "Failed") }
            _state.update { it.copy(agentRunning = false, agentReport = report.toString()) }
            message(error.message ?: "Agent task failed")
        }
    }

    private fun updateAgentStep(id: String, status: AgentStepStatus, detail: String) {
        _state.update { state -> state.copy(agentSteps = state.agentSteps.map { if (it.id == id) it.copy(status = status, detail = detail) else it }) }
    }

    private suspend fun awaitActivePageText(): String? = withTimeoutOrNull(10_000) {
        suspendCancellableCoroutine { continuation ->
            activeSession()?.evaluateJavaScript("(function(){return document.body ? document.body.innerText.slice(0,12000) : '';})()") { text ->
                if (continuation.isActive) continuation.resume(text)
            } ?: continuation.resume(null)
        }
    }

    fun showToolbar() = _state.update { it.copy(toolbarVisible = true) }

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
                        ifActive(event.tabId) { copy(currentUrl = event.url, loading = true, toolbarVisible = true) }
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
                    is EngineEvent.ScrollChanged -> if (_state.value.settings.autoHideToolbar && event.tabId == _state.value.activeTabId) _state.update {
                        it.copy(toolbarVisible = event.scrollY < 100 || event.deltaY < 0)
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
        is EngineEvent.ScrollChanged -> event.tabId
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
