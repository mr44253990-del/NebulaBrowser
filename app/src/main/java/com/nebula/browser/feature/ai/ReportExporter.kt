package com.nebula.browser.feature.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.OutputStream

object ReportExporter {
    fun exportText(context: Context, task: String, report: String): String {
        val name = "Nebula-research-${System.currentTimeMillis()}.txt"
        write(context, name, "text/plain") { it.bufferedWriter().use { writer -> writer.write("TASK: $task\n\n$report") } }
        return name
    }

    fun exportPdf(context: Context, task: String, report: String): String {
        val name = "Nebula-research-${System.currentTimeMillis()}.pdf"
        val document = PdfDocument()
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.BLACK }
        val titlePaint = TextPaint(paint).apply { textSize = 18f; isFakeBoldText = true }
        val pageWidth = 595
        val pageHeight = 842
        val content = "TASK: $task\n\n$report"
        var remaining = content
        var pageNumber = 1
        while (remaining.isNotEmpty() && pageNumber <= 80) {
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            val canvas: Canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawText("NEBULA RESEARCH", 36f, 36f, titlePaint)
            val layout = StaticLayout.Builder.obtain(remaining, 0, remaining.length, paint, pageWidth - 72)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
            canvas.save(); canvas.translate(36f, 62f); layout.draw(canvas); canvas.restore()
            // StaticLayout lays out all lines; determine how many fit this page.
            var lastLine = 0
            while (lastLine < layout.lineCount && layout.getLineBottom(lastLine) <= pageHeight - 100) lastLine++
            val visibleCharacters = if (lastLine < layout.lineCount) layout.getLineStart(lastLine) else remaining.length
            document.finishPage(page)
            remaining = remaining.substring(visibleCharacters.coerceIn(1, remaining.length)).trimStart()
            pageNumber++
        }
        write(context, name, "application/pdf") { output -> document.writeTo(output) }
        document.close()
        return name
    }

    private fun write(context: Context, name: String, mime: String, block: (OutputStream) -> Unit) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Nebula")
            }
            val uri: Uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create report in Downloads")
            context.contentResolver.openOutputStream(uri)?.use(block) ?: error("Cannot write report")
        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Nebula").apply { mkdirs() }
            File(directory, name).outputStream().use(block)
        }
    }
}
