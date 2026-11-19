package com.nebula.browser.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.FrameLayout
import com.nebula.browser.BrowserUiState
import com.nebula.browser.core.BrowserTab
import com.nebula.browser.engine.BrowserSession
import com.nebula.browser.util.createQrCode

@Composable
fun AddressBar(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
) {
    var text by remember { mutableStateOf(state.currentUrl) }
    var menuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(state.currentUrl, state.activeTabId) { text = state.currentUrl }
    Column {
        if (state.loading) LinearProgressIndicator(
            progress = { state.progress / 100f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = state.canGoBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = if (state.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f))
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).height(52.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("Search or enter address", maxLines = 1, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                leadingIcon = {
                    Icon(
                        if (state.currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                        "Security",
                        tint = if (state.currentUrl.startsWith("https://")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) IconButton(onClick = { text = "" }) { Icon(Icons.Default.Close, "Clear") }
                },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = NebulaCyan,
                    focusedBorderColor = NebulaPurple,
                    unfocusedBorderColor = NebulaOutline,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onNavigate(text) }),
            )
            if (state.currentUrl.isNotBlank()) IconButton(onClick = { onNavigate(state.currentUrl) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Refresh, "Reload", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Tab, "${state.tabs.size} tabs")
                }
                if (state.tabs.isNotEmpty()) Text(
                    state.tabs.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Center).clickable(onClick = onTabs),
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Show tabs") }, onClick = { menuExpanded = false; onTabs() })
                    DropdownMenuItem(text = { Text("Browser menu") }, onClick = { menuExpanded = false; onMenu() })
                }
            }
            IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, "Menu", tint = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
fun WebSessionView(session: BrowserSession?) {
    if (session == null) return
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context -> FrameLayout(context).also(session::attach) },
        update = { container -> session.attach(container) },
    )
    DisposableEffect(session) { onDispose { session.detach() } }
}

@Composable
private fun NebulaMark(modifier: Modifier = Modifier, pulse: () -> Float = { 1f }) {
    val purple = MaterialTheme.colorScheme.primary
    val cyan = MaterialTheme.colorScheme.secondary
    androidx.compose.foundation.Canvas(modifier) {
        val glow = pulse()
        val radius = size.minDimension / 2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        drawCircle(Brush.radialGradient(listOf(purple.copy(alpha = .36f * glow), Color.Transparent)), radius = radius * 1.45f, center = center)
        withTransform({
            rotate(-17f, pivot = center)
            scale(1f, .42f, pivot = center)
        }) {
            drawOval(
                Brush.linearGradient(listOf(purple, cyan, Color(0xFF4E7CFF), purple)),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * .03f, size.height * .14f),
                size = androidx.compose.ui.geometry.Size(size.width * .94f, size.height * .72f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx()),
            )
        }
        drawCircle(
            Brush.radialGradient(
                colors = listOf(Color(0xFFE8D9FF), Color(0xFFB168FF), Color(0xFF5A1EA8), Color(0xFF120625)),
                center = androidx.compose.ui.geometry.Offset(center.x - radius * .24f, center.y - radius * .30f),
                radius = radius * 1.12f,
            ),
            radius = radius * .66f, center = center,
        )
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = .24f), Color.Transparent)),
            radius = radius * .42f,
            center = androidx.compose.ui.geometry.Offset(center.x - radius * .22f, center.y - radius * .30f),
        )
        drawArc(
            Brush.linearGradient(listOf(purple, cyan, Color(0xFF6688FF))),
            startAngle = 12f, sweepAngle = 166f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * .03f, size.height * .32f),
            size = androidx.compose.ui.geometry.Size(size.width * .94f, size.height * .36f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        drawCircle(Color.White, radius = 1.4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .88f, size.height * .10f))
    }
}

@Composable
private fun BrandIcon(id: String, fallback: String, color: Color) {
    val resource = when (id) {
        "google" -> com.nebula.browser.R.drawable.brand_google
        "youtube" -> com.nebula.browser.R.drawable.brand_youtube
        "wikipedia" -> com.nebula.browser.R.drawable.brand_wikipedia
        "github" -> com.nebula.browser.R.drawable.brand_github
        "gmail" -> com.nebula.browser.R.drawable.brand_gmail
        "drive" -> com.nebula.browser.R.drawable.brand_googledrive
        "facebook" -> com.nebula.browser.R.drawable.brand_facebook
        "duckduckgo" -> com.nebula.browser.R.drawable.brand_duckduckgo
        else -> null
    }
    if (resource != null) Image(painterResource(resource), null, modifier = Modifier.size(38.dp))
    else Text(fallback.take(2), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onVoice: () -> Unit,
    onScanQr: () -> Unit,
    onExtensions: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onAddShortcut: (String, String) -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onMoveShortcut: (String, Int) -> Unit,
    onDeleteSearch: (String) -> Unit,
    onCommandCenter: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var addDialog by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf<String?>(null) }
    var tipVisible by remember { mutableStateOf(true) }
    var searchFocused by remember { mutableStateOf(false) }
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 5 -> "Still up?"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 22 -> "Good evening"
            else -> "Good night"
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val markPulse = com.nebula.browser.ui.motion.rememberPulse(periodMillis = 3_600)
        val iconSize = when {
            maxWidth <= 360.dp -> 64.dp
            maxWidth <= 400.dp -> 72.dp
            maxWidth <= 480.dp -> 76.dp
            else -> 80.dp
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onProfile, modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .7f), RoundedCornerShape(50))) {
                    Icon(Icons.Default.Person, "Profile and workspaces", tint = NebulaPurple)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    Text("${state.tabs.size} tabs · ${state.bookmarks.size} bookmarks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onCommandCenter, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.Bolt, "Command center", tint = NebulaCyan)
                }
                IconButton(onClick = onNotifications, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.NotificationsNone, "Downloads and activity", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                NebulaMark(Modifier.size(98.dp), pulse = { markPulse.value })
                Text("NEBULA", fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("Browse beyond boundaries", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().height(78.dp).onFocusChanged { searchFocused = it.isFocused },
                placeholder = { Text("Search or enter address…", fontSize = 16.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(24.dp), tint = NebulaPurple) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onVoice, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Mic, "Voice search", tint = NebulaPurple) }
                        IconButton(onClick = onScanQr, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.QrCodeScanner, "Scan QR", tint = NebulaCyan) }
                    }
                },
                singleLine = true, shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NebulaPurple, unfocusedBorderColor = NebulaPurple.copy(alpha = .75f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = NebulaCyan,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .82f),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onNavigate(query) }),
            )
            if (searchFocused) {
                val suggestions = state.recentSearches.filter { query.isBlank() || it.query.contains(query, true) }.sortedWith(compareByDescending<com.nebula.browser.core.SearchQuery> { it.count }.thenByDescending { it.lastSearchedAt }).take(5)
                if (suggestions.isNotEmpty()) GlassCard(Modifier.fillMaxWidth().padding(top = 6.dp), radius = 18.dp) {
                    suggestions.forEach { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion.query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${suggestion.count} searches") },
                            leadingContent = { Icon(Icons.Default.History, null, tint = NebulaPurple) },
                            trailingContent = { IconButton(onClick = { onDeleteSearch(suggestion.query) }) { Icon(Icons.Default.Close, "Remove suggestion") } },
                            modifier = Modifier.clickable { query = suggestion.query; searchFocused = false; onNavigate(suggestion.query) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.weight(1f).height(46.dp), radius = 16.dp, onClick = { onNavigate("10 km to mi") }) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Calculate, null, tint = NebulaPurple); Spacer(Modifier.width(7.dp)); Text("Smart Calculator", fontSize = 13.sp, maxLines = 1)
                    }
                }
                GlassCard(Modifier.weight(1f).height(46.dp), radius = 16.dp, onClick = onExtensions) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Extension, null, tint = NebulaCyan); Spacer(Modifier.width(7.dp)); Text("Extension Lab", fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
            if (state.settings.homeShowFrequent && state.history.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                SectionTitle("Your orbit", "Most visited websites, ranked from your real history")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.history.sortedByDescending { it.visitCount }.distinctBy { it.url.substringAfter("://").substringBefore('/') }.take(5), key = { "frequent_${it.id}" }) { entry ->
                        GlassCard(Modifier.width(156.dp).height(72.dp), radius = 18.dp, onClick = { onNavigate(entry.url) }) {
                            Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                    Text(entry.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(9.dp))
                                Column { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text("${entry.visitCount} visits", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            val shortcuts = state.settings.shortcuts
            val itemCount = shortcuts.size + 1
            val rows = (itemCount + 3) / 4
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), modifier = Modifier.fillMaxWidth().height((rows * 116).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false,
            ) {
                items(shortcuts, key = { it.id }) { shortcut ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(iconSize).clip(RoundedCornerShape(22.dp)).background(
                                Brush.radialGradient(listOf(Color(shortcut.color).copy(alpha = .24f), Color(0xFF171022)))
                            ).border(1.dp, NebulaOutline, RoundedCornerShape(22.dp)).combinedClickable(
                                onClick = { onNavigate(shortcut.url) }, onLongClick = { editId = shortcut.id },
                            ), contentAlignment = Alignment.Center,
                        ) {
                            BrandIcon(shortcut.id, shortcut.symbol, Color(shortcut.color))
                        }
                        Text(shortcut.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                    }
                }
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(iconSize).clip(RoundedCornerShape(22.dp)).background(Color(0x801A112A)).border(1.dp, NebulaOutline, RoundedCornerShape(22.dp)).clickable { addDialog = true },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Add, "Add website", tint = NebulaPurple, modifier = Modifier.size(30.dp)) }
                        Text("Add Site", fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
            val activeDownloads = state.downloads.filter {
                it.status == com.nebula.browser.core.DownloadStatus.RUNNING ||
                    it.status == com.nebula.browser.core.DownloadStatus.QUEUED ||
                    it.status == com.nebula.browser.core.DownloadStatus.PAUSED
            }
            if (activeDownloads.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                GlassCard(Modifier.fillMaxWidth(), radius = 18.dp, onClick = onNotifications) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(NebulaCyan, RoundedCornerShape(50)))
                            Spacer(Modifier.width(7.dp))
                            Text("Live downloads · ${activeDownloads.size}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            Text("Open", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        activeDownloads.take(2).forEach { item ->
                            val animated = androidx.compose.animation.core.animateFloatAsState(
                                targetValue = item.progress,
                                animationSpec = androidx.compose.animation.core.tween(600),
                                label = "downloadProgress",
                            ).value
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.fileName, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(5.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                                Text("${(animated * 100).toInt()}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (tipVisible && state.settings.homeShowTips) GlassCard(Modifier.fillMaxWidth(), radius = 18.dp) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, null, tint = NebulaPurple)
                    Text("Try ‘gh android browser’, ‘45*12’, or ‘10 kg to lb’. Long-press a shortcut to arrange it.", modifier = Modifier.weight(1f).padding(horizontal = 10.dp), fontSize = 12.sp)
                    IconButton(onClick = { tipVisible = false }) { Icon(Icons.Default.Close, "Dismiss tip", modifier = Modifier.size(18.dp)) }
                }
            }
            if (state.settings.showRecentCard && state.history.isNotEmpty()) {
                Spacer(Modifier.height(12.dp)); SectionTitle("Continue browsing")
                GlassCard(Modifier.fillMaxWidth()) {
                    state.history.take(3).forEach { entry ->
                        ListItem(
                            headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp) },
                            supportingContent = { Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                            leadingContent = { Icon(Icons.Default.History, null, tint = NebulaPurple) },
                            modifier = Modifier.clickable { onNavigate(entry.url) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (addDialog) {
        var title by remember { mutableStateOf("") }; var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { addDialog = false }, title = { Text("Add website shortcut") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("Website URL") }, singleLine = true)
            } },
            confirmButton = { Button(onClick = { onAddShortcut(title, url); addDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } },
        )
    }
    editId?.let { id ->
        AlertDialog(
            onDismissRequest = { editId = null }, title = { Text("Arrange shortcut") },
            text = { Text("Move this shortcut or remove it from your home screen.") },
            confirmButton = {
                Row {
                    TextButton(onClick = { onMoveShortcut(id, -1) }) { Text("Move left") }
                    TextButton(onClick = { onMoveShortcut(id, 1) }) { Text("Move right") }
                    TextButton(onClick = { onRemoveShortcut(id); editId = null }) { Text("Remove") }
                }
            },
            dismissButton = { TextButton(onClick = { editId = null }) { Text("Done") } },
        )
    }
}

@Composable
fun NebulaBottomBar(state: BrowserUiState, onBack: () -> Unit, onForward: () -> Unit, onHome: () -> Unit, onReload: () -> Unit, onTabs: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(58.dp), radius = 25.dp) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = state.canGoBack) { Icon(Icons.Default.ArrowBack, "Back", tint = if (state.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)) }
            IconButton(onClick = onForward, enabled = state.canGoForward) { Icon(Icons.Default.ArrowForward, "Forward", tint = if (state.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)) }
            Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).clickable(onClick = onHome), contentAlignment = Alignment.Center) { Icon(Icons.Default.Home, "Home", tint = MaterialTheme.colorScheme.onPrimary) }
            IconButton(onClick = onReload) { Icon(if (state.loading) Icons.Default.Close else Icons.Default.Refresh, "Reload or stop", tint = MaterialTheme.colorScheme.onSurface) }
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = onTabs) { Icon(Icons.Default.Tab, "Tabs", tint = MaterialTheme.colorScheme.onSurface) }
                Text(state.tabs.size.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSwitcher(
    state: BrowserUiState,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNew: (Boolean) -> Unit,
    onWorkspace: (String) -> Unit,
    onPin: (String) -> Unit,
    onCloseOthers: (String) -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    onRename: (String, String) -> Unit,
    onGroup: (String, String?) -> Unit,
    onMove: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onCloseAll: () -> Unit,
    onGroupTogether: (String, String) -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
) {
    var actionTab by remember { mutableStateOf<BrowserTab?>(null) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var tabQuery by remember { mutableStateOf("") }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val cardBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val workspaceTabs = state.tabs.filter { it.workspaceId == state.activeWorkspaceId }
    val groups = workspaceTabs.mapNotNull { it.groupName }.distinct()
    val tabs = workspaceTabs.filter {
        (selectedGroup == null || it.groupName == selectedGroup) &&
            (tabQuery.isBlank() || it.title.contains(tabQuery, true) || it.url.contains(tabQuery, true) || it.groupName?.contains(tabQuery, true) == true)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = if (state.canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)) }
            Text("Workspaces & tabs", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onRestore) { Text("Restore") }
            IconButton(onClick = { onNew(false) }) { Icon(Icons.Default.Add, "New tab", tint = NebulaPurple) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.workspaces) { workspace ->
                FilterChip(
                    selected = state.activeWorkspaceId == workspace.id, onClick = { onWorkspace(workspace.id); selectedGroup = null },
                    label = { Text(workspace.name, fontSize = 13.sp) }, modifier = Modifier.height(44.dp),
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary, selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton("New tab", { onNew(false) }, Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)) }
            GlassCard(Modifier.weight(1f).height(50.dp), radius = 18.dp, onClick = { onNew(true) }) {
                Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, tint = NebulaPurple); Spacer(Modifier.width(7.dp)); Text("Private tab") }
            }
        }
        if (groups.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { FilterChip(selected = selectedGroup == null, onClick = { selectedGroup = null }, label = { Text("All groups") }) }
            items(groups) { group -> FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group }, label = { Text(group) }) }
        }
        OutlinedTextField(
            tabQuery, { tabQuery = it }, Modifier.fillMaxWidth().padding(vertical = 4.dp).height(50.dp),
            placeholder = { Text("Search open tabs, URLs and groups", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) }, singleLine = true,
            shape = RoundedCornerShape(18.dp), textStyle = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${tabs.size} tabs · drag the handle onto another tab to group", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onCloseAll) { Text("Close unpinned", fontSize = 12.sp) }
        }
        LazyVerticalGrid(
            modifier = Modifier.weight(1f), columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tabs, key = BrowserTab::id) { tab ->
                GlassCard(
                    Modifier.fillMaxWidth().height(242.dp)
                        .onGloballyPositioned { cardBounds[tab.id] = it.boundsInRoot() }
                        .graphicsLayer {
                            if (draggedId == tab.id) { translationX = dragOffset.x; translationY = dragOffset.y; alpha = .88f }
                        }
                        .zIndex(if (draggedId == tab.id) 2f else 0f)
                        .combinedClickable(onClick = { onSelect(tab.id) }, onLongClick = { actionTab = tab }),
                    active = tab.id == state.activeTabId || dropTargetId == tab.id, radius = 20.dp,
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(if (tab.isPrivate) NebulaPurple else NebulaCyan, RoundedCornerShape(50)))
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            tab.groupName?.let { Text(it, fontSize = 10.sp, color = NebulaPurple, maxLines = 1) }
                        }
                        IconButton(onClick = { onClose(tab.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp)) }
                    }
                    val preview = remember(tab.previewPath) { tab.previewPath?.let { android.graphics.BitmapFactory.decodeFile(it) } }
                    Box(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF090612)), contentAlignment = Alignment.Center) {
                        if (preview != null) Image(preview.asImageBitmap(), "Preview of ${tab.title}", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NebulaMark(Modifier.size(52.dp))
                            Text(tab.url.substringAfter("://").substringBefore('/').ifBlank { "New tab" }, fontSize = 11.sp, maxLines = 1, color = Color.White)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onPin(tab.id) }) { Icon(Icons.Default.PushPin, if (tab.isPinned) "Unpin" else "Pin", tint = if (tab.isPinned) NebulaPurple else MaterialTheme.colorScheme.onSurfaceVariant) }
                        Icon(
                            Icons.Default.DragIndicator, "Drag to group with another tab",
                            modifier = Modifier.size(24.dp).pointerInput(tab.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedId = tab.id; dragOffset = Offset.Zero; dropTargetId = null },
                                    onDrag = { change, amount ->
                                        change.consume(); dragOffset += amount
                                        val origin = cardBounds[tab.id]?.center ?: return@detectDragGesturesAfterLongPress
                                        val pointer = origin + dragOffset
                                        dropTargetId = cardBounds.entries.firstOrNull { it.key != tab.id && it.value.contains(pointer) }?.key
                                    },
                                    onDragEnd = {
                                        val target = dropTargetId
                                        if (target != null) onGroupTogether(tab.id, target)
                                        draggedId = null; dropTargetId = null; dragOffset = Offset.Zero
                                    },
                                    onDragCancel = { draggedId = null; dropTargetId = null; dragOffset = Offset.Zero },
                                )
                            },
                        )
                        TextButton(onClick = { onCloseOthers(tab.id) }, modifier = Modifier.weight(1f)) { Text("Close others", fontSize = 11.sp) }
                    }
                }
            }
        }
        NebulaBottomBar(state, onBack, onForward, onBack, onReload) { }
    }
    actionTab?.let { tab ->
        var title by remember(tab.id) { mutableStateOf(tab.title) }
        var group by remember(tab.id) { mutableStateOf(tab.groupName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { actionTab = null }, title = { Text("Tab & group actions") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Tab name") }, singleLine = true)
                    OutlinedTextField(group, { group = it }, label = { Text("Group name (optional)") }, singleLine = true)
                    Text("Move to workspace", fontWeight = FontWeight.SemiBold)
                    state.workspaces.forEach { workspace -> TextButton(onClick = { onMove(tab.id, workspace.id); actionTab = null }) { Text(workspace.name) } }
                }
            },
            confirmButton = { Button(onClick = { onRename(tab.id, title); onGroup(tab.id, group); actionTab = null }) { Text("Save") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { onDuplicate(tab.id); actionTab = null }) { Text("Duplicate") }
                    TextButton(onClick = { onClose(tab.id); actionTab = null }) { Text("Close") }
                }
            },
        )
    }
}

@Composable
fun TabGroupDock(state: BrowserUiState, onSelect: (String) -> Unit, onNewInGroup: () -> Unit) {
    val active = state.activeTab ?: return
    val group = active.groupName ?: return
    var minimized by remember(group) { mutableStateOf(false) }
    val tabs = state.tabs.filter { it.workspaceId == active.workspaceId && it.groupName == group }
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 8.dp), radius = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            TextButton(onClick = { minimized = !minimized }) { Text(if (minimized) "$group (${tabs.size})" else "$group · Minimize", maxLines = 1, fontSize = 12.sp) }
            if (!minimized) LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(tabs, key = { "dock_${it.id}" }) { tab ->
                    FilterChip(selected = tab.id == active.id, onClick = { onSelect(tab.id) }, label = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) })
                }
            } else Spacer(Modifier.weight(1f))
            IconButton(onClick = onNewInGroup, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "New tab in group", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun FindBar(onFind: (String) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            query,
            { query = it; onFind(it) },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Find in page") },
            singleLine = true,
        )
        IconButton(onClick = { onFind(""); onClose() }) { Icon(Icons.Default.Close, "Close") }
    }
}

@Composable
fun QrCodeDialog(content: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(content) { createQrCode(content) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page QR code") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bitmap.asImageBitmap(), content, modifier = Modifier.size(220.dp))
                Text(content, maxLines = 2, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("URL", content))
                }) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy URL") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
fun BookmarkEditorDialog(
    title: String,
    url: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
) {
    var name by remember(url) { mutableStateOf(title) }
    var folder by remember { mutableStateOf("Bookmarks") }
    var tags by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to your library") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Bookmark name") }, singleLine = true)
                OutlinedTextField(url, {}, label = { Text("Website") }, readOnly = true, singleLine = true)
                OutlinedTextField(folder, { folder = it }, label = { Text("Folder (new or existing)") }, singleLine = true)
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma separated") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Private note") }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { onSave(name, url, folder, tags, notes); onDismiss() }, enabled = name.isNotBlank() && url.isNotBlank()) { Text("Save bookmark") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
