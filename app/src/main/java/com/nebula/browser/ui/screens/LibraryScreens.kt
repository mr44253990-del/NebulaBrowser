package com.nebula.browser.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.text.format.Formatter
import com.nebula.browser.BrowserUiState
import com.nebula.browser.core.DownloadStatus
import com.nebula.browser.core.ExtensionReport
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
        actions = { actions() },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: BrowserUiState, onBack: () -> Unit, onOpen: (String) -> Unit, onDelete: (Long) -> Unit, onClear: () -> Unit, onClearSearches: () -> Unit, onDeleteSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("All") }
    val cutoff = when (period) {
        "Today" -> java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        "7 days" -> System.currentTimeMillis() - 7L * 86_400_000
        else -> 0L
    }
    val entries = state.history.filter {
        (query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true)) &&
            it.visitedAt >= cutoff && (period != "Popular" || it.visitCount > 1)
    }.let { list -> if (period == "Popular") list.sortedByDescending { it.visitCount } else list }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        LibraryTopBar("History", onBack) { TextButton(onClick = onClear) { Text("Clear all") } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search history") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
            )
            LazyRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("All", "Today", "7 days", "Popular")) { filter ->
                    androidx.compose.material3.FilterChip(selected = period == filter, onClick = { period = filter }, label = { Text(filter) })
                }
            }
            LazyColumn {
                if (state.recentSearches.isNotEmpty() && query.isBlank()) item {
                    com.nebula.browser.ui.components.GlassCard(Modifier.fillMaxWidth().padding(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Smart search history", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                TextButton(onClick = onClearSearches) { Text("Clear searches") }
                            }
                            state.recentSearches.take(6).forEach { search ->
                                ListItem(
                                    headlineContent = { Text(search.query) },
                                    supportingContent = { Text("${search.count} searches") },
                                    leadingContent = { Icon(Icons.Default.Search, null) },
                                    trailingContent = { IconButton(onClick = { onDeleteSearch(search.query) }) { Icon(Icons.Default.Delete, "Delete search") } },
                                    modifier = Modifier.clickable { onOpen(search.query) },
                                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                )
                            }
                        }
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Column {
                                Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${DateFormat.getDateTimeInstance().format(Date(entry.visitedAt))} · ${entry.visitCount} visits")
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Search, null) },
                        trailingContent = { IconButton(onClick = { onDelete(entry.id) }) { Icon(Icons.Default.Delete, "Delete") } },
                        modifier = Modifier.clickable { onOpen(entry.url) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onExport: (android.net.Uri) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImport) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri -> uri?.let(onExport) }
    val bookmarks = state.bookmarks.filter { query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true) || it.tags.contains(query, true) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        LibraryTopBar("Bookmarks", onBack) {
            IconButton(onClick = { importLauncher.launch(arrayOf("text/html", "application/xhtml+xml")) }) { Icon(Icons.Default.FileUpload, "Import") }
            IconButton(onClick = { exportLauncher.launch("nebula-bookmarks.html") }) { Icon(Icons.Default.FileOpen, "Export") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp), placeholder = { Text("Search bookmarks, folders, tags") }, singleLine = true)
            LazyColumn {
                items(bookmarks, key = { it.id }) { bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${bookmark.folder} · ${bookmark.url}", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Default.Bookmark, null) },
                        trailingContent = { IconButton(onClick = { onDelete(bookmark.id) }) { Icon(Icons.Default.Delete, "Delete") } },
                        modifier = Modifier.clickable { onOpen(bookmark.url) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onRetry: (com.nebula.browser.core.DownloadItem) -> Unit,
    onOpenExternal: (Long) -> Unit,
    onShare: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    var category by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val filtered = state.downloads.filter { item ->
        val ext = item.fileName.substringAfterLast('.', "").lowercase()
        when (category) {
            "Images" -> ext in setOf("jpg", "jpeg", "png", "gif", "webp")
            "Video" -> ext in setOf("mp4", "mkv", "webm", "3gp")
            "Docs" -> ext in setOf("pdf", "txt", "doc", "docx", "json", "csv", "md")
            "APK" -> ext == "apk"
            "Archives" -> ext in setOf("zip", "rar", "7z", "tar", "gz")
            else -> true
        }
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        LibraryTopBar("Downloads", onBack) {
            if (selected.isNotEmpty()) TextButton(onClick = { selected.forEach(onRemove); selected = emptySet() }) { Text("Delete ${selected.size}") }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
        LazyRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("All", "Images", "Video", "Docs", "APK", "Archives")) { filter ->
                androidx.compose.material3.FilterChip(selected = category == filter, onClick = { category = filter }, label = { Text(filter) })
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (filtered.isEmpty()) item { Text("No downloads in this category", modifier = Modifier.padding(20.dp)) }
            items(filtered, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(checked = item.id in selected, onCheckedChange = { checked -> selected = if (checked) selected + item.id else selected - item.id })
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text(item.fileName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(8.dp))
                        val elapsed = ((System.currentTimeMillis() - item.createdAt) / 1000).coerceAtLeast(0)
                        val elapsedText = if (elapsed < 60) "${elapsed}s" else "${elapsed / 60}m ${elapsed % 60}s"
                        Text("${item.status.name.lowercase()} · ${(item.progress * 100).toInt()}% · ${Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, item.bytesDownloaded)} / ${if (item.totalBytes > 0) Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, item.totalBytes) else "unknown"} · $elapsedText")
                        if (item.status == DownloadStatus.RUNNING) {
                            LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        }
                        item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.status == DownloadStatus.SUCCESSFUL) TextButton(onClick = { onOpen(item.id) }) { Text("Open") }
                            if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.QUEUED) {
                                TextButton(onClick = { onPause(item.id) }) { Text("Pause") }
                                TextButton(onClick = { onCancel(item.id) }) { Text("Cancel") }
                            }
                            if (item.status == DownloadStatus.PAUSED) {
                                TextButton(onClick = { onResume(item.id) }) { Text("Resume") }
                                TextButton(onClick = { onCancel(item.id) }) { Text("Cancel") }
                            }
                            if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED) TextButton(onClick = { onRetry(item) }) { Text("Retry") }
                            TextButton(onClick = { onShare(item.id) }) { Text("Share") }
                            TextButton(onClick = { onOpenExternal(item.id) }) { Text("Open with") }
                            TextButton(onClick = { onRemove(item.id) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(state: BrowserUiState, onBack: () -> Unit, onImport: (android.net.Uri, String) -> Unit, onClear: () -> Unit, onImportScript: (android.net.Uri, String) -> Unit, onToggleScript: (com.nebula.browser.core.UserScript) -> Unit, onDeleteScript: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImport(it, it.lastPathSegment?.substringAfterLast('/') ?: "extension") }
    }
    val scriptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportScript(it, it.lastPathSegment?.substringAfterLast('/') ?: "script.user.js") }
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        LibraryTopBar("Extension compatibility lab", onBack) { TextButton(onClick = onClear) { Text("Clear") } }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Default.Extension, null)
                        Text("Inspect before you trust", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Import CRX, XPI, or ZIP to inspect its manifest, permissions, APIs, and engine compatibility. Static analysis only—no extension code is executed.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { launcher.launch(arrayOf("application/zip", "application/x-xpinstall", "application/octet-stream")) }) { Text("Import extension package") }
                    }
                }
            }
            item {
                Text("Runtime status: Android WebView does not provide Chrome/Firefox extension APIs. ‘Compatible’ below means manifest-level potential, not tested execution.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                com.nebula.browser.ui.components.GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nebula Userscripts · ${state.userScripts.size} installed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${state.userScripts.count { it.enabled }} active · @grant none scripts only · website access is limited to declared @match rules")
                        Button(onClick = { scriptLauncher.launch(arrayOf("application/javascript", "text/javascript", "text/plain", "application/octet-stream")) }) { Text("Install .user.js") }
                    }
                }
            }
            items(state.userScripts, key = { it.id }) { script ->
                com.nebula.browser.ui.components.GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(script.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            androidx.compose.material3.Switch(checked = script.enabled, onCheckedChange = { onToggleScript(script) })
                            IconButton(onClick = { onDeleteScript(script.id) }) { Icon(Icons.Default.Delete, "Delete userscript") }
                        }
                        Text("v${script.version} · ${if (script.enabled) "Active" else "Disabled"}")
                        Text("Website permissions: ${script.matches.joinToString()}")
                        Text("API grants: ${script.grants.joinToString()}")
                        if (script.grants.any { it != "none" }) Text("Unsupported GM grants: script remains blocked for safety.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            items(state.extensionReports) { report -> ExtensionReportCard(report) }
        }
    }
}

@Composable
private fun ExtensionReportCard(report: ExtensionReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(report.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${report.fileName} · v${report.version} · Manifest V${report.manifestVersion}")
            Text("Chromium MV3 potential: ${if (report.chromiumCompatible) "Yes" else "No/unknown"}")
            Text("Gecko WebExtension potential: ${if (report.geckoCompatible) "Yes" else "No/unknown"}")
            if (report.permissions.isNotEmpty()) Text("Permissions: ${report.permissions.joinToString()}", fontWeight = FontWeight.SemiBold)
            if (report.hostPermissions.isNotEmpty()) Text("Website access: ${report.hostPermissions.joinToString()}")
            if (report.detectedApis.isNotEmpty()) Text("Detected APIs: ${report.detectedApis.joinToString()}")
            report.warnings.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
