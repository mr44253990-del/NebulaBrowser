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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SaveAlt
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.nebula.browser.BrowserViewModel
import com.nebula.browser.BuildConfig
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
    var showCommand by remember { mutableStateOf(false) }
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

    val commandActions = listOf(
        com.nebula.browser.ui.sheets.CommandAction("New tab", "Open a fresh tab", Icons.Default.Add) {
            showCommand = false; viewModel.createTab(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("New private tab", "No history, cookies cleared on exit", Icons.Default.Lock) {
            showCommand = false; viewModel.createTab(privateMode = true); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Tabs & workspaces", "Switch, group, duplicate or restore tabs", Icons.Default.Tab) {
            showCommand = false; screen = AppScreen.TABS
        },
        com.nebula.browser.ui.sheets.CommandAction("Add bookmark", "Save the current page", Icons.Default.BookmarkAdd) {
            showCommand = false; showBookmarkEditor = true; screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Bookmarks", "Your saved pages and folders", Icons.Default.Bookmark) {
            showCommand = false; screen = AppScreen.BOOKMARKS
        },
        com.nebula.browser.ui.sheets.CommandAction("History", "Everything you visited, searchable", Icons.Default.History) {
            showCommand = false; screen = AppScreen.HISTORY
        },
        com.nebula.browser.ui.sheets.CommandAction("Downloads", "Files, progress and retries", Icons.Default.Download) {
            showCommand = false; screen = AppScreen.DOWNLOADS
        },
        com.nebula.browser.ui.sheets.CommandAction("Find in page", "Search text on the current page", Icons.Default.FindInPage) {
            showCommand = false; showFind = true; screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Reader mode", "Distraction-free text with read-aloud", Icons.Default.FindInPage) {
            showCommand = false; viewModel.showReader(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("AI assistant & agent", "Ask about the page or plan a research run", Icons.Default.SmartToy) {
            showCommand = false; showAi = true; screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Desktop site", "Toggle desktop rendering for this tab", Icons.Default.DesktopWindows) {
            showCommand = false; viewModel.toggleDesktopMode(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Save as PDF", "Print or export the current page", Icons.Default.PictureAsPdf) {
            showCommand = false; viewModel.printPage(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Page QR code", "Share the current address as a QR", Icons.Default.QrCode) {
            showCommand = false; if (state.currentUrl.isNotBlank()) showQr = true; screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Screenshot", "Save the visible page as a PNG", Icons.Default.PhotoCamera) {
            showCommand = false; viewModel.captureScreenshot(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Save page text", "Export readable text into Download/Nebula", Icons.Default.SaveAlt) {
            showCommand = false; viewModel.savePageText(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Save page HTML", "Keep an offline copy of the markup", Icons.Default.Code) {
            showCommand = false; viewModel.savePageHtml(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Copy page text", "Put the whole article on the clipboard", Icons.Default.ContentCopy) {
            showCommand = false; viewModel.copyPageText(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Clear site data", "Delete cookies, storage and cache", Icons.Default.DeleteSweep) {
            showCommand = false; viewModel.clearSiteData(); screen = AppScreen.BROWSER
        },
        com.nebula.browser.ui.sheets.CommandAction("Password vault", "Encrypted credentials and generator", Icons.Default.Security) {
            showCommand = false; screen = AppScreen.PASSWORDS
        },
        com.nebula.browser.ui.sheets.CommandAction("Extension lab", "Inspect extensions and userscripts", Icons.Default.Extension) {
            showCommand = false; screen = AppScreen.EXTENSIONS
        },
        com.nebula.browser.ui.sheets.CommandAction("Privacy controls", "Blocking, cookies and browsing data", Icons.Default.PrivacyTip) {
            showCommand = false; screen = AppScreen.PRIVACY
        },
        com.nebula.browser.ui.sheets.CommandAction("Settings", "Theme, motion, search and AI", Icons.Default.Settings) {
            showCommand = false; screen = AppScreen.SETTINGS
        },
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // Swipe back to close; the page keeps its own swipes while closed.
        drawerContent = {
            var drawerQuery by remember { mutableStateOf("") }
            val sections: List<Pair<String, List<DrawerEntry>>> = listOf(
                "Navigate" to listOf(
                    DrawerEntry("Command center", Icons.Default.Bolt) { showCommand = true; scope.launch { drawerState.close() } },
                    DrawerEntry("New tab", Icons.Default.Add) { viewModel.createTab(); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("New private tab", Icons.Default.Lock) { viewModel.createTab(privateMode = true); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Tabs & workspaces", Icons.Default.Tab) { openScreen(AppScreen.TABS) },
                ),
                "Library" to listOf(
                    DrawerEntry("Bookmarks", Icons.Default.Bookmark) { openScreen(AppScreen.BOOKMARKS) },
                    DrawerEntry("History", Icons.Default.History) { openScreen(AppScreen.HISTORY) },
                    DrawerEntry("Downloads", Icons.Default.Download) { openScreen(AppScreen.DOWNLOADS) },
                    DrawerEntry("Password vault", Icons.Default.Security) { openScreen(AppScreen.PASSWORDS) },
                    DrawerEntry("Extension lab", Icons.Default.Extension) { openScreen(AppScreen.EXTENSIONS) },
                ),
                "Page tools" to listOf(
                    DrawerEntry("Add bookmark", Icons.Default.BookmarkAdd) { showBookmarkEditor = true; openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Reader mode", Icons.Default.FindInPage) { viewModel.showReader(); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("AI page assistant", Icons.Default.SmartToy) { showAi = true; openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Find in page", Icons.Default.FindInPage) { showFind = true; openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Desktop site: ${if (state.desktopMode) "on" else "off"}", Icons.Default.DesktopWindows) { viewModel.toggleDesktopMode(); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Save / print PDF", Icons.Default.PictureAsPdf) { viewModel.printPage(); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Share page", Icons.Default.Share) {
                        runCatching {
                            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.currentUrl) }
                            context.startActivity(Intent.createChooser(share, "Share page"))
                        }
                        openScreen(AppScreen.BROWSER)
                    },
                    DrawerEntry("Page QR code", Icons.Default.QrCode) { if (state.currentUrl.isNotBlank()) showQr = true; openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Screenshot", Icons.Default.PhotoCamera) { viewModel.captureScreenshot(); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Save page text", Icons.Default.SaveAlt) { viewModel.savePageText(); openScreen(AppScreen.BROWSER) },
                    DrawerEntry("Copy page text", Icons.Default.ContentCopy) { viewModel.copyPageText(); openScreen(AppScreen.BROWSER) },
                ),
                "App" to buildList<DrawerEntry> {
                    add(DrawerEntry("Privacy controls", Icons.Default.PrivacyTip) { openScreen(AppScreen.PRIVACY) })
                    if (state.settings.developerMode) {
                        add(DrawerEntry("Developer console", Icons.Default.Code) { openScreen(AppScreen.DEVELOPER) })
                    }
                    add(DrawerEntry("Settings", Icons.Default.Settings) { openScreen(AppScreen.SETTINGS) })
                },
            )
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight(),
                drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("NEBULA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("Private by default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, "Close menu", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                OutlinedTextField(
                    value = drawerQuery,
                    onValueChange = { drawerQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Find a tool or page", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    sections.forEach { (title, entries) ->
                        val visible = entries.filter { drawerQuery.isBlank() || it.label.contains(drawerQuery, true) }
                        if (visible.isNotEmpty()) {
                            Text(
                                title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 2.dp),
                            )
                            visible.forEach { entry ->
                                DrawerItem(entry.label, entry.icon) { entry.action() }
                            }
                        }
                    }
                    if (drawerQuery.isNotBlank() && sections.all { (_, entries) -> entries.none { it.label.contains(drawerQuery, true) } }) {
                        Text("No matching tool", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
                Text(
                    "Nebula ${BuildConfig.VERSION_NAME} · no account, no analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
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
                    screen == AppScreen.DOWNLOADS -> DownloadsScreen(state, { screen = AppScreen.BROWSER }, viewModel::refreshDownloads, viewModel::openDownload, viewModel::cancelDownload, viewModel::removeDownload, viewModel::retryDownload, viewModel::openDownloadExternally, viewModel::shareDownload, viewModel::copyDownloadLink)
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
                        onCommandCenter = { showCommand = true },
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
    if (showCommand) com.nebula.browser.ui.sheets.CommandCenterSheet(
        subtitle = if (state.currentUrl.isBlank()) "${state.tabs.size} tabs open · ${state.bookmarks.size} bookmarks" else state.currentTitle,
        actions = commandActions,
        onDismiss = { showCommand = false },
    )
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
            showCommand -> showCommand = false
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

private class DrawerEntry(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: () -> Unit,
)

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
    onCommandCenter: () -> Unit,
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
                if (state.isHome) HomePage(state, viewModel::navigate, onVoice, onScanQr, onExtensions, onProfile, onNotifications, viewModel::addShortcut, viewModel::removeShortcut, viewModel::moveShortcut, viewModel::deleteSearchQuery, onCommandCenter)
                else WebSessionView(viewModel.activeSession())
            }
        }
    }
}
