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
fun HistoryScreen(state: BrowserUiState, onBack: () -> Unit, onOpen: (String) -> Unit, onDelete: (Long) -> Unit, onClear: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val entries = state.history.filter { query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        LibraryTopBar("History", onBack) { TextButton(onClick = onClear) { Text("Clear all") } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Search history") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
            )
            LazyColumn {
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
) {
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = {
        LibraryTopBar("Downloads", onBack) { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") } }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.downloads.isEmpty()) item { Text("No downloads yet", modifier = Modifier.padding(20.dp)) }
            items(state.downloads, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(12.dp))
                            Text(item.fileName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${item.status.name.lowercase()} · ${Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, item.bytesDownloaded)} / ${if (item.totalBytes > 0) Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, item.totalBytes) else "unknown"}")
                        if (item.status == DownloadStatus.RUNNING) {
                            LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        }
                        item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.status == DownloadStatus.SUCCESSFUL) TextButton(onClick = { onOpen(item.id) }) { Text("Open") }
                            if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.QUEUED) TextButton(onClick = { onCancel(item.id) }) { Text("Cancel") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(state: BrowserUiState, onBack: () -> Unit, onImport: (android.net.Uri, String) -> Unit, onClear: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImport(it, it.lastPathSegment?.substringAfterLast('/') ?: "extension") }
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
