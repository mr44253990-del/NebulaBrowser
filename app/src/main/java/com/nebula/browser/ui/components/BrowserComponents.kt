package com.nebula.browser.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = state.canGoBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search or enter address") },
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onNavigate(text) }),
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
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
            IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, "Menu") }
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

private data class QuickLink(val title: String, val url: String, val symbol: String)

private val quickLinks = listOf(
    QuickLink("Google", "https://google.com", "G"),
    QuickLink("YouTube", "https://youtube.com", "▶"),
    QuickLink("Wikipedia", "https://wikipedia.org", "W"),
    QuickLink("GitHub", "https://github.com", "GH"),
    QuickLink("Gmail", "https://mail.google.com", "M"),
    QuickLink("DuckDuckGo", "https://duckduckgo.com", "D"),
    QuickLink("Facebook", "https://facebook.com", "f"),
    QuickLink("Add website", "https://www.google.com/search?q=add+custom+shortcut+help", "+"),
)

@Composable
fun HomePage(
    state: BrowserUiState,
    onNavigate: (String) -> Unit,
    onVoice: () -> Unit,
    onScanQr: () -> Unit,
    onExtensions: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("NEBULA", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Browse beyond boundaries", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search privately or type a URL") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                Row {
                    TextButton(onClick = onVoice) { Text("Voice") }
                    IconButton(onClick = onScanQr) { Icon(Icons.Default.QrCode, "Scan QR") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onNavigate(query) }),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { onNavigate("10 km to mi") }, label = { Text("Smart calculator") })
            AssistChip(onClick = onExtensions, label = { Text("Extension lab") })
        }
        Spacer(Modifier.height(22.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(state.settings.homeColumns.coerceIn(3, 5)),
            modifier = Modifier.fillMaxWidth().height(210.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
        ) {
            items(quickLinks) { link ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigate(link.url) }) {
                    Box(
                        Modifier.size(58.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(link.symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    Text(link.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (state.settings.showRecentCard && state.history.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Continue browsing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    state.history.take(4).forEach { entry ->
                        Row(Modifier.fillMaxWidth().clickable { onNavigate(entry.url) }.padding(vertical = 10.dp)) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Tip: use keywords like ‘gh android browser’, calculate ‘45*12’, or convert ‘10 kg to lb’.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("Workspaces & tabs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onRestore) { Text("Restore") }
            IconButton(onClick = { onNew(false) }) { Icon(Icons.Default.Add, "New tab") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.workspaces) { workspace ->
                FilterChip(
                    selected = state.activeWorkspaceId == workspace.id,
                    onClick = { onWorkspace(workspace.id) },
                    label = { Text(workspace.name) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onNew(false) }, modifier = Modifier.weight(1f)) { Text("New tab") }
            TextButton(onClick = { onNew(true) }, modifier = Modifier.weight(1f)) { Text("Private tab") }
        }
        val tabs = state.tabs.filter { it.workspaceId == state.activeWorkspaceId }
        LazyVerticalGrid(modifier = Modifier.weight(1f), columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tabs, key = BrowserTab::id) { tab ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(190.dp).clickable { onSelect(tab.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (tab.id == state.activeTabId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (tab.isPrivate) "●" else "○", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { onClose(tab.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp)) }
                        }
                        Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(if (tab.isPrivate) "PRIVATE" else tab.url.substringAfter("://").substringBefore('/').ifBlank { "NEW TAB" }, style = MaterialTheme.typography.labelLarge)
                        }
                        Row {
                            IconButton(onClick = { onPin(tab.id) }) { Icon(Icons.Default.PushPin, if (tab.isPinned) "Unpin" else "Pin") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onCloseOthers(tab.id) }) { Text("Close others") }
                        }
                    }
                }
            }
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
