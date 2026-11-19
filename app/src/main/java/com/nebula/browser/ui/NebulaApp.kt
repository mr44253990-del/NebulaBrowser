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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
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
import com.nebula.browser.ui.components.NebulaBottomBar
import com.nebula.browser.ui.components.SpaceBackground
import com.nebula.browser.ui.components.TabGroupDock
import com.nebula.browser.ui.components.BookmarkEditorDialog
import com.nebula.browser.ui.components.QrCodeDialog
import com.nebula.browser.ui.components.TabSwitcher
import com.nebula.browser.ui.components.WebSessionView
import com.nebula.browser.ui.screens.AiAssistantSheet
import com.nebula.browser.ui.screens.AiConsentDialog
import com.nebula.browser.ui.screens.BookmarksScreen
import com.nebula.browser.ui.screens.DownloadsScreen
import com.nebula.browser.ui.screens.ExtensionsScreen
import com.nebula.browser.ui.screens.HistoryScreen
import com.nebula.browser.ui.screens.FileViewerScreen
import com.nebula.browser.ui.screens.DeveloperScreen
import com.nebula.browser.ui.screens.PasswordsScreen
import com.nebula.browser.ui.screens.PrivacyScreen
import com.nebula.browser.ui.screens.ReaderScreen
import com.nebula.browser.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class AppScreen { BROWSER, TABS, HISTORY, BOOKMARKS, DOWNLOADS, EXTENSIONS, PASSWORDS, PRIVACY, DEVELOPER, SETTINGS }

@Composable
fun NebulaApp(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NebulaTheme(
        darkTheme = state.settings.darkTheme,
        dynamicColors = state.settings.dynamicColors,
        accentColor = state.settings.accentColor,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.nebula.browser.ui.motion.LocalReduceMotion provides state.settings.reduceMotion,
        ) {
            NebulaAppContent(viewModel, state)
        }
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
    var showBookmarkEditor by remember { mutableStateOf(false) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var allowMultipleFiles by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val locationOnly = viewModel.state.value.pendingPermission?.resources == listOf("location")
        viewModel.resolveSitePermission(if (locationOnly) grants.values.any { it } else grants.values.all { it })
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(state.settings.immersiveMode, state.settings.autoHideToolbar, state.toolbarVisible, screen) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val hide = screen == AppScreen.BROWSER && !state.isHome &&
            (state.settings.immersiveMode || (state.settings.autoHideToolbar && !state.toolbarVisible))
        if (hide) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
    }

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
                UiEffect.RequestDownloadNotifications -> if (android.os.Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

    LaunchedEffect(screen) {
        if (screen == AppScreen.DOWNLOADS) while (true) {
            viewModel.refreshDownloads()
            kotlinx.coroutines.delay(2_000)
        }
    }

    fun openScreen(destination: AppScreen) {
        screen = destination
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false, // Prevent accidental browser-navigation swipes from opening the drawer.
        drawerContent = {
            ModalDrawerSheet(modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
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
                DrawerItem("Add bookmark", Icons.Default.BookmarkAdd) { showBookmarkEditor = true; openScreen(AppScreen.BROWSER) }
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
                if (state.settings.developerMode) DrawerItem("Developer console", Icons.Default.Code) { openScreen(AppScreen.DEVELOPER) }
                DrawerItem("Settings", Icons.Default.Settings) { openScreen(AppScreen.SETTINGS) }
            }
        },
    ) {
        SpaceBackground(
            modifier = if (state.settings.threeFingerMenu) Modifier.pointerInput(state.settings.threeFingerMenu) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                    // A parent-level observer sees multi-touch without consuming webpage gestures.
                    var pointerCount = 1
                    while (pointerCount < 3) {
                        val next = awaitPointerEvent(pass = PointerEventPass.Initial)
                        pointerCount = next.changes.count { it.pressed }
                        if (pointerCount == 0) break
                    }
                    if (pointerCount >= 3) {
                        viewModel.showToolbar()
                        scope.launch { drawerState.open() }
                    }
                }
            } else Modifier,
            style = state.settings.backgroundStyle,
            liquidEffects = state.settings.liquidEffects,
        ) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = androidx.compose.ui.graphics.Color.Transparent) { rootPadding ->
            Box(Modifier.fillMaxSize().padding(rootPadding)) {
                when {
                    state.openedFile != null -> FileViewerScreen(state.openedFile!!, viewModel::closeFileViewer, { viewModel.openDownloadExternally(state.openedFile!!.id) }, { viewModel.shareDownload(state.openedFile!!.id) })
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
                        onRename = viewModel::renameTab,
                        onGroup = viewModel::setTabGroup,
                        onMove = viewModel::moveTabToWorkspace,
                        onDuplicate = viewModel::duplicateTab,
                        onCloseAll = viewModel::closeAllInWorkspace,
                        onGroupTogether = viewModel::groupTabsTogether,
                        onForward = viewModel::goForward,
                        onReload = viewModel::reload,
                    )
                    screen == AppScreen.HISTORY -> HistoryScreen(state, { screen = AppScreen.BROWSER }, { viewModel.navigate(it); screen = AppScreen.BROWSER }, viewModel::deleteHistory, viewModel::clearHistory, viewModel::clearSearchHistory, viewModel::deleteSearchQuery)
                    screen == AppScreen.BOOKMARKS -> BookmarksScreen(state, { screen = AppScreen.BROWSER }, { viewModel.navigate(it); screen = AppScreen.BROWSER }, viewModel::deleteBookmark, viewModel::importBookmarks, viewModel::exportBookmarks)
                    screen == AppScreen.DOWNLOADS -> DownloadsScreen(state, { screen = AppScreen.BROWSER }, viewModel::refreshDownloads, viewModel::openDownload, viewModel::cancelDownload, viewModel::removeDownload, viewModel::retryDownload, viewModel::openDownloadExternally, viewModel::shareDownload)
                    screen == AppScreen.EXTENSIONS -> ExtensionsScreen(state, { screen = AppScreen.BROWSER }, viewModel::importExtension, viewModel::clearExtensionReports, viewModel::importUserScript, viewModel::toggleUserScript, viewModel::deleteUserScript)
                    screen == AppScreen.PASSWORDS -> PasswordsScreen(state, { screen = AppScreen.BROWSER }, viewModel::refreshPasswords, viewModel::savePassword, viewModel::deletePassword, viewModel::generatePassword)
                    screen == AppScreen.PRIVACY -> PrivacyScreen(state, { screen = AppScreen.BROWSER }, viewModel::updateSettings, viewModel::clearBrowsingData)
                    screen == AppScreen.DEVELOPER -> DeveloperScreen(state, { screen = AppScreen.BROWSER }, viewModel::runDeveloperScript, viewModel::viewPageSource)
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
                        onProfile = { screen = AppScreen.TABS },
                        onNotifications = { screen = AppScreen.DOWNLOADS },
                    )
                }
            }
            if (state.settings.edgeMenuEnabled && screen == AppScreen.BROWSER) {
                Box(
                    Modifier.fillMaxHeight().width(18.dp).align(if (state.settings.edgeMenuSide == "right") Alignment.CenterEnd else Alignment.CenterStart).pointerInput(state.settings.edgeMenuSide) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if ((state.settings.edgeMenuSide == "left" && dragAmount > 12f) || (state.settings.edgeMenuSide == "right" && dragAmount < -12f)) {
                                scope.launch { drawerState.open() }
                            }
                        }
                    }
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = screen == AppScreen.BROWSER && !state.isHome,
                enter = androidx.compose.animation.scaleIn(
                    initialScale = 0.6f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                ) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.scaleOut(targetScale = 0.6f) + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { viewModel.showToolbar(); scope.launch { drawerState.open() } },
                    modifier = Modifier.padding(end = 14.dp, bottom = 76.dp).size(42.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) { Icon(Icons.Default.MoreVert, "Open browser menu", tint = MaterialTheme.colorScheme.onPrimary) }
            }
        }
        }
    }

    state.pendingPermission?.let { request ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.resolveSitePermission(false) },
            title = { Text("Website permission") },
            text = { Text("${request.origin}\n\nAllow ${request.resources.joinToString()} for this site? Android permission may also be requested.") },
            confirmButton = { androidx.compose.material3.Button(onClick = {
                if (request.androidPermissions.isEmpty()) viewModel.resolveSitePermission(true)
                else permissionLauncher.launch(request.androidPermissions.toTypedArray())
            }) { Text("Allow") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { viewModel.resolveSitePermission(false) }) { Text("Deny") } },
        )
    }
    if (showBookmarkEditor) BookmarkEditorDialog(state.currentTitle, state.currentUrl, { showBookmarkEditor = false }, { title, url, folder, tags, notes ->
        viewModel.addBookmark(title, url, folder, tags, notes)
    })
    if (showQr && state.currentUrl.isNotBlank()) QrCodeDialog(state.currentUrl) { showQr = false }
    if (showAi) AiAssistantSheet(
        state = state,
        onDismiss = { showAi = false },
        onAsk = viewModel::prepareAi,
        onClear = viewModel::clearAiResponse,
        onOpenSettings = { showAi = false; screen = AppScreen.SETTINGS },
        onStartAgent = viewModel::startAgent,
        onConfirmAgent = viewModel::confirmAgentPlan,
        onDismissAgentPlan = viewModel::dismissAgentPlan,
        onCancelAgent = viewModel::cancelAgent,
    )
    state.aiConsent?.let { AiConsentDialog(it, viewModel::confirmAiSend, viewModel::dismissAiConsent) }

    BackHandler {
        when {
            showAi -> showAi = false
            state.openedFile != null -> viewModel.closeFileViewer()
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
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            if (!state.settings.bottomToolbar && !state.settings.immersiveMode && state.toolbarVisible) AddressBar(state, viewModel::navigate, viewModel::goBack, onTabs, onMenu)
        },
        bottomBar = {
            Column {
                if (!state.isHome && state.activeTab?.groupName != null && state.toolbarVisible && !state.settings.immersiveMode) {
                    TabGroupDock(state, viewModel::selectTab, viewModel::createTabInActiveGroup)
                }
                if (state.settings.bottomToolbar && !state.isHome && !state.settings.immersiveMode && state.toolbarVisible) {
                    AddressBar(state, viewModel::navigate, viewModel::goBack, onTabs, onMenu)
                }
                if (state.isHome) {
                    NebulaBottomBar(state, viewModel::goBack, viewModel::goForward, viewModel::newHomeTab, if (state.loading) viewModel::stop else viewModel::reload, onTabs)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showFind) FindBar(viewModel::findInPage, onFindClose)
            Box(Modifier.fillMaxSize()) {
                if (state.isHome) HomePage(state, viewModel::navigate, onVoice, onScanQr, onExtensions, onProfile, onNotifications, viewModel::addShortcut, viewModel::removeShortcut, viewModel::moveShortcut, viewModel::deleteSearchQuery)
                else WebSessionView(viewModel.activeSession())
            }
        }
    }
}
