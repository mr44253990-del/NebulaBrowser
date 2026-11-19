package com.nebula.browser.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.webkit.ValueCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nebula.browser.BrowserViewModel
import com.nebula.browser.UiEffect
import com.nebula.browser.ui.components.AddressBar
import com.nebula.browser.ui.components.FindBar
import com.nebula.browser.ui.components.HomePage
import com.nebula.browser.ui.components.QrCodeDialog
import com.nebula.browser.ui.components.TabSwitcher
import com.nebula.browser.ui.components.WebSessionView
import com.nebula.browser.ui.screens.AiAssistantSheet
import com.nebula.browser.ui.screens.AiConsentDialog
import com.nebula.browser.ui.screens.BookmarksScreen
import com.nebula.browser.ui.screens.DownloadsScreen
import com.nebula.browser.ui.screens.ExtensionsScreen
import com.nebula.browser.ui.screens.HistoryScreen
import com.nebula.browser.ui.screens.PasswordsScreen
import com.nebula.browser.ui.screens.PrivacyScreen
import com.nebula.browser.ui.screens.ReaderScreen
import com.nebula.browser.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class AppScreen { BROWSER, TABS, HISTORY, BOOKMARKS, DOWNLOADS, EXTENSIONS, PASSWORDS, PRIVACY, SETTINGS }

@Composable
fun NebulaApp(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NebulaTheme(darkTheme = state.settings.darkTheme) {
        NebulaAppContent(viewModel, state)
    }
}

@Composable
private fun NebulaAppContent(viewModel: BrowserViewModel, state: com.nebula.browser.BrowserUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var screen by remember { mutableStateOf(AppScreen.BROWSER) }
    var showFind by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showAi by remember { mutableStateOf(false) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var allowMultipleFiles by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val result = if (allowMultipleFiles) uris.toTypedArray() else uris.take(1).toTypedArray()
        fileCallback?.onReceiveValue(result)
        fileCallback = null
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::navigate)
        }
    }
    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(viewModel::navigate)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is UiEffect.Message -> snackbar.showSnackbar(effect.text)
                is UiEffect.OpenExternal -> try { context.startActivity(effect.intent) } catch (_: ActivityNotFoundException) {
                    snackbar.showSnackbar("No application can open this content")
                }
                is UiEffect.FileChooser -> {
                    fileCallback = effect.request.callback
                    allowMultipleFiles = effect.request.allowMultiple
                    val accepted = effect.request.acceptTypes.filter { it.isNotBlank() && it.contains('/') }.toTypedArray()
                    fileLauncher.launch(if (accepted.isEmpty()) arrayOf("*/*") else accepted)
                }
            }
        }
    }

    LaunchedEffect(screen) { if (screen == AppScreen.DOWNLOADS) viewModel.refreshDownloads() }

    fun openScreen(destination: AppScreen) {
        screen = destination
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = screen == AppScreen.BROWSER,
        drawerContent = {
            ModalDrawerSheet {
                Text("NEBULA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(22.dp), color = MaterialTheme.colorScheme.primary)
                DrawerItem("New tab", Icons.Default.Add) { viewModel.createTab(); openScreen(AppScreen.BROWSER) }
                DrawerItem("New private tab", Icons.Default.Lock) { viewModel.createTab(privateMode = true); openScreen(AppScreen.BROWSER) }
                DrawerItem("Tabs & workspaces", Icons.Default.Tab) { openScreen(AppScreen.TABS) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerItem("Bookmarks", Icons.Default.Bookmark) { openScreen(AppScreen.BOOKMARKS) }
                DrawerItem("History", Icons.Default.History) { openScreen(AppScreen.HISTORY) }
                DrawerItem("Downloads", Icons.Default.Download) { openScreen(AppScreen.DOWNLOADS) }
                DrawerItem("Password vault", Icons.Default.Security) { openScreen(AppScreen.PASSWORDS) }
                DrawerItem("Extension lab", Icons.Default.Extension) { openScreen(AppScreen.EXTENSIONS) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DrawerItem("Add bookmark", Icons.Default.BookmarkAdd) { viewModel.addBookmark(); openScreen(AppScreen.BROWSER) }
                DrawerItem("Reader mode", Icons.Default.FindInPage) { viewModel.showReader(); openScreen(AppScreen.BROWSER) }
                DrawerItem("AI page assistant", Icons.Default.SmartToy) { showAi = true; openScreen(AppScreen.BROWSER) }
                DrawerItem("Find in page", Icons.Default.FindInPage) { showFind = true; openScreen(AppScreen.BROWSER) }
                DrawerItem("Desktop site: ${if (state.desktopMode) "on" else "off"}", Icons.Default.DesktopWindows) { viewModel.toggleDesktopMode(); openScreen(AppScreen.BROWSER) }
                DrawerItem("Save / print PDF", Icons.Default.PictureAsPdf) { viewModel.printPage(); openScreen(AppScreen.BROWSER) }
                DrawerItem("Share page", Icons.Default.Share) {
                    val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.currentUrl) }
                    context.startActivity(Intent.createChooser(share, "Share page"))
                    openScreen(AppScreen.BROWSER)
                }
                DrawerItem("Page QR code", Icons.Default.QrCode) { if (state.currentUrl.isNotBlank()) showQr = true; openScreen(AppScreen.BROWSER) }
                DrawerItem("Privacy controls", Icons.Default.PrivacyTip) { openScreen(AppScreen.PRIVACY) }
                DrawerItem("Settings", Icons.Default.Settings) { openScreen(AppScreen.SETTINGS) }
            }
        },
    ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { rootPadding ->
            Box(Modifier.fillMaxSize().padding(rootPadding)) {
                when {
                    state.readerVisible -> ReaderScreen(state.currentTitle, state.readerText, viewModel::hideReader) { showAi = true }
                    screen == AppScreen.TABS -> TabSwitcher(
                        state = state,
                        onSelect = { viewModel.selectTab(it); screen = AppScreen.BROWSER },
                        onClose = viewModel::closeTab,
                        onNew = { viewModel.createTab(it); screen = AppScreen.BROWSER },
                        onWorkspace = viewModel::selectWorkspace,
                        onPin = viewModel::togglePin,
                        onCloseOthers = viewModel::closeOtherTabs,
                        onRestore = { viewModel.restoreClosedTab(); screen = AppScreen.BROWSER },
                        onBack = { screen = AppScreen.BROWSER },
                    )
                    screen == AppScreen.HISTORY -> HistoryScreen(state, { screen = AppScreen.BROWSER }, { viewModel.navigate(it); screen = AppScreen.BROWSER }, viewModel::deleteHistory, viewModel::clearHistory)
                    screen == AppScreen.BOOKMARKS -> BookmarksScreen(state, { screen = AppScreen.BROWSER }, { viewModel.navigate(it); screen = AppScreen.BROWSER }, viewModel::deleteBookmark, viewModel::importBookmarks, viewModel::exportBookmarks)
                    screen == AppScreen.DOWNLOADS -> DownloadsScreen(state, { screen = AppScreen.BROWSER }, viewModel::refreshDownloads, viewModel::openDownload, viewModel::cancelDownload, viewModel::removeDownload, viewModel::retryDownload)
                    screen == AppScreen.EXTENSIONS -> ExtensionsScreen(state, { screen = AppScreen.BROWSER }, viewModel::importExtension, viewModel::clearExtensionReports)
                    screen == AppScreen.PASSWORDS -> PasswordsScreen(state, { screen = AppScreen.BROWSER }, viewModel::refreshPasswords, viewModel::savePassword, viewModel::deletePassword, viewModel::generatePassword)
                    screen == AppScreen.PRIVACY -> PrivacyScreen(state, { screen = AppScreen.BROWSER }, viewModel::updateSettings, viewModel::clearBrowsingData)
                    screen == AppScreen.SETTINGS -> SettingsScreen(state, { screen = AppScreen.BROWSER }, viewModel::updateSettings, viewModel::addCustomSearchEngine, viewModel.aiAssistant.apiKey, { viewModel.aiAssistant.apiKey = it })
                    else -> BrowserContent(
                        state = state,
                        viewModel = viewModel,
                        showFind = showFind,
                        onFindClose = { showFind = false },
                        onTabs = { screen = AppScreen.TABS },
                        onMenu = { scope.launch { drawerState.open() } },
                        onVoice = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Nebula")
                            }
                            runCatching { voiceLauncher.launch(intent) }
                        },
                        onScanQr = {
                            qrLauncher.launch(ScanOptions().apply {
                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                setPrompt("Scan a QR code")
                                setBeepEnabled(false)
                                setOrientationLocked(false)
                            })
                        },
                        onExtensions = { screen = AppScreen.EXTENSIONS },
                    )
                }
            }
        }
    }

    if (showQr && state.currentUrl.isNotBlank()) QrCodeDialog(state.currentUrl) { showQr = false }
    if (showAi) AiAssistantSheet(
        loading = state.aiLoading,
        response = state.aiResponse,
        enabled = state.settings.aiEnabled,
        onDismiss = { showAi = false },
        onAsk = viewModel::prepareAi,
        onClear = viewModel::clearAiResponse,
        onOpenSettings = { showAi = false; screen = AppScreen.SETTINGS },
    )
    state.aiConsent?.let { AiConsentDialog(it, viewModel::confirmAiSend, viewModel::dismissAiConsent) }

    BackHandler {
        when {
            showAi -> showAi = false
            state.readerVisible -> viewModel.hideReader()
            screen != AppScreen.BROWSER -> screen = AppScreen.BROWSER
            showFind -> { showFind = false; viewModel.findInPage("") }
            state.canGoBack -> viewModel.goBack()
            state.tabs.size > 1 -> viewModel.closeTab(state.activeTabId)
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp),
    )
}

@Composable
private fun BrowserContent(
    state: com.nebula.browser.BrowserUiState,
    viewModel: BrowserViewModel,
    showFind: Boolean,
    onFindClose: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    onVoice: () -> Unit,
    onScanQr: () -> Unit,
    onExtensions: () -> Unit,
) {
    Scaffold(
        topBar = {
            if (!state.settings.bottomToolbar) AddressBar(state, viewModel::navigate, viewModel::goBack, onTabs, onMenu)
        },
        bottomBar = {
            Column {
                if (state.settings.bottomToolbar) AddressBar(state, viewModel::navigate, viewModel::goBack, onTabs, onMenu)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = viewModel::goBack, enabled = state.canGoBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    IconButton(onClick = viewModel::goForward, enabled = state.canGoForward) { Icon(Icons.Default.ArrowForward, "Forward") }
                    IconButton(onClick = viewModel::newHomeTab) { Icon(Icons.Default.Home, "Home") }
                    IconButton(onClick = if (state.loading) viewModel::stop else viewModel::reload) { Icon(Icons.Default.Refresh, "Reload or stop") }
                    IconButton(onClick = onTabs) { Icon(Icons.Default.Tab, "Tabs") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showFind) FindBar(viewModel::findInPage, onFindClose)
            Box(Modifier.fillMaxSize()) {
                if (state.isHome) HomePage(state, viewModel::navigate, onVoice, onScanQr, onExtensions)
                else WebSessionView(viewModel.activeSession())
            }
        }
    }
}
