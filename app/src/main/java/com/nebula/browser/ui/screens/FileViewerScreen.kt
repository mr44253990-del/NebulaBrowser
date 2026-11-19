package com.nebula.browser.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nebula.browser.core.DownloadItem
import java.io.File
import java.util.zip.ZipFile

@Composable
fun FileViewerScreen(item: DownloadItem, onBack: () -> Unit, onOpenExternal: () -> Unit, onShare: () -> Unit) {
    val file = remember(item) { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Nebula/${item.fileName}") }
    Scaffold(topBar = {
        LibraryTopBar(item.fileName, onBack) {
            TextButton(onClick = onShare) { Text("Share") }
            TextButton(onClick = onOpenExternal) { Text("Open with") }
        }
    }, containerColor = Color.Transparent) { padding ->
        when (file.extension.lowercase()) {
            in TEXT_TYPES -> TextFileViewer(file, Modifier.padding(padding))
            in IMAGE_TYPES -> ImageFileViewer(file, Modifier.padding(padding))
            "pdf" -> PdfFileViewer(file, Modifier.padding(padding))
            "zip" -> ZipFileViewer(file, Modifier.padding(padding))
            in MEDIA_TYPES -> MediaFileViewer(file, Modifier.padding(padding))
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("This format needs an external app") }
        }
    }
}

@Composable
private fun TextFileViewer(file: File, modifier: Modifier) {
    val text = remember(file) { runCatching { if (file.length() <= 2 * 1024 * 1024) file.readText() else "File is larger than the 2 MB built-in text preview limit." }.getOrElse { it.message ?: "Cannot read file" } }
    SelectionContainer(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable
private fun ImageFileViewer(file: File, modifier: Modifier) {
    val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
    Box(modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), file.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        else Text("This image encoding is not supported by the device")
    }
}

@Composable
private fun PdfFileViewer(file: File, modifier: Modifier) {
    val renderer = remember(file) {
        runCatching {
            require(file.exists() && file.length() > 0) { "PDF file is empty or unavailable" }
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        }.getOrNull()
    }
    DisposableEffect(renderer) { onDispose { runCatching { renderer?.close() } } }
    if (renderer == null || renderer.pageCount <= 0) {
        return Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cannot preview this PDF safely. Use “Open with” to choose another PDF reader.")
                TextButton(onClick = { }, enabled = false) { Text("Preview unavailable") }
            }
        }
    }
    var pageIndex by remember(renderer) { mutableIntStateOf(0) }
    val bitmap = remember(pageIndex, renderer) {
        runCatching {
            renderer.openPage(pageIndex).use { page ->
                require(page.width > 0 && page.height > 0) { "Invalid PDF page" }
                val width = 900 // Bounded rendering prevents oversized bitmap crashes.
                val height = (width.toFloat() / page.width * page.height).toInt().coerceIn(1, 4096)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                    it.eraseColor(android.graphics.Color.WHITE)
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }.getOrNull()
    }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) { Icon(Icons.Default.ArrowBack, "Previous page") }
            Text("Page ${pageIndex + 1} / ${renderer.pageCount}")
            IconButton(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(renderer.pageCount - 1) }, enabled = pageIndex + 1 < renderer.pageCount) { Icon(Icons.Default.ArrowForward, "Next page") }
        }
        if (bitmap == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This PDF page could not be rendered") }
        else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Image(
                bitmap.asImageBitmap(), "PDF page ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth().height(with(androidx.compose.ui.platform.LocalDensity.current) { bitmap.height.toDp() }),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@Composable
private fun ZipFileViewer(file: File, modifier: Modifier) {
    val entries = remember(file) {
        runCatching {
            ZipFile(file).use { zip -> zip.entries().asSequence().take(500).map { entry -> "${if (entry.isDirectory) "📁" else "📄"}  ${entry.name}  ·  ${entry.size} bytes" }.toList() }
        }.getOrElse { listOf("Cannot read archive: ${it.message}") }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item { Text("Archive contents (read-only preview)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp)) }
        items(entries) { Text(it, modifier = Modifier.padding(vertical = 7.dp)) }
    }
}

@Composable
private fun MediaFileViewer(file: File, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
        Text(file.name, style = MaterialTheme.typography.titleMedium)
        AndroidView(factory = { context ->
            VideoView(context).apply {
                setMediaController(MediaController(context))
                setVideoPath(file.absolutePath)
                setOnPreparedListener { start() }
            }
        }, modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black))
        Text("Playback uses codecs supported by your Android device.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    }
}

private val TEXT_TYPES = setOf("txt", "log", "json", "xml", "csv", "md", "html", "htm", "css", "js", "kt", "java", "py")
private val IMAGE_TYPES = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
private val MEDIA_TYPES = setOf("mp3", "wav", "ogg", "m4a", "mp4", "webm", "mkv", "3gp")
