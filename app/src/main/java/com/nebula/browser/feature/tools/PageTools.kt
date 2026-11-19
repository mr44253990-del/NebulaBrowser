package com.nebula.browser.feature.tools

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

/**
 * Writes page captures and exports into Download/Nebula, using MediaStore on Android 10+
 * and the public downloads directory on older releases.
 */
object PageTools {

    fun saveScreenshot(context: Context, bitmap: Bitmap): String? {
        val name = "Nebula-shot-${timestamp()}.png"
        return runCatching {
            write(context, name, "image/png") { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
            name
        }.getOrNull()
    }

    fun savePageText(context: Context, title: String, url: String, body: String): String? {
        val name = "Nebula-page-${slug(title)}-${timestamp()}.md"
        val content = buildString {
            appendLine("# ${title.ifBlank { "Untitled page" }}")
            appendLine()
            appendLine(url)
            appendLine()
            appendLine(body)
        }
        return runCatching {
            write(context, name, "text/markdown") { output -> output.bufferedWriter().use { it.write(content) } }
            name
        }.getOrNull()
    }

    fun savePageHtml(context: Context, title: String, html: String): String? {
        val name = "Nebula-page-${slug(title)}-${timestamp()}.html"
        return runCatching {
            write(context, name, "text/html") { output -> output.bufferedWriter().use { it.write(html) } }
            name
        }.getOrNull()
    }

    fun slug(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "page" }

    private fun timestamp(): String = System.currentTimeMillis().toString().takeLast(9)

    private fun write(context: Context, name: String, mime: String, block: (OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Nebula")
            }
            val uri: Uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create $name in Downloads")
            context.contentResolver.openOutputStream(uri)?.use(block) ?: error("Cannot write $name")
        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Nebula").apply { mkdirs() }
            File(directory, name).outputStream().use(block)
        }
    }
}
