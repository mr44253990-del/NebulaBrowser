package com.nebula.browser.browser

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.nebula.browser.core.DownloadItem
import com.nebula.browser.core.DownloadStatus
import com.nebula.browser.core.NebulaDatabase
import com.nebula.browser.core.toEntity
import com.nebula.browser.core.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadController(context: Context, private val database: NebulaDatabase) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val dao = database.downloads()
    val downloads: Flow<List<DownloadItem>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun enqueue(
        url: String,
        fileName: String,
        mimeType: String?,
        userAgent: String?,
        wifiOnly: Boolean,
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            require(url.startsWith("https://") || url.startsWith("http://")) { "Only HTTP(S) downloads are supported" }
            val safeName = fileName.substringAfterLast('/').replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "download" }
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(safeName)
                .setDescription("Nebula Browser download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(!wifiOnly)
                .setAllowedOverRoaming(!wifiOnly)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Nebula/$safeName")
            mimeType?.takeIf { it.isNotBlank() }?.let(request::setMimeType)
            userAgent?.let { request.addRequestHeader("User-Agent", it) }
            val id = manager.enqueue(request)
            dao.save(DownloadItem(id = id, url = url, fileName = safeName, mimeType = mimeType).toEntity())
            id
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        dao.ids().forEach { id ->
            val stored = dao.byId(id) ?: return@forEach
            val query = manager.query(DownloadManager.Query().setFilterById(id))
            query.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val status = when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
                    DownloadManager.STATUS_RUNNING -> DownloadStatus.RUNNING
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
                    else -> DownloadStatus.FAILED
                }
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                dao.save(stored.copy(
                    status = status.name,
                    bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    error = if (status == DownloadStatus.FAILED) "Download error (code $reason)" else null,
                ).toEntity())
            }
        }
    }

    suspend fun cancel(id: Long) = withContext(Dispatchers.IO) {
        manager.remove(id)
        dao.byId(id)?.let { dao.save(it.copy(status = DownloadStatus.CANCELLED.name).toEntity()) }
    }

    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        manager.remove(id)
        dao.deleteById(id)
    }

    suspend fun retry(item: DownloadItem, wifiOnly: Boolean): Result<Long> {
        remove(item.id)
        return enqueue(item.url, item.fileName, item.mimeType, null, wifiOnly)
    }

    fun openIntent(id: Long): Intent? = manager.getUriForDownloadedFile(id)?.let { uri ->
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, manager.getMimeTypeForDownloadedFile(id))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DownloadController(context, NebulaDatabase.get(context)).refresh()
            } finally {
                pending.finish()
            }
        }
    }
}
