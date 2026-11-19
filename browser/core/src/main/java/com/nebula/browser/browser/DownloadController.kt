package com.nebula.browser.browser

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
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
        notificationsEnabled: Boolean = true,
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
            if (notificationsEnabled) {
                runCatching {
                    androidx.core.content.ContextCompat.startForegroundService(appContext, Intent(appContext, DownloadProgressService::class.java).setAction(DownloadProgressService.ACTION_START))
                }
            }
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
                    error = when {
                        status == DownloadStatus.FAILED -> "Download error (code $reason)"
                        status == DownloadStatus.PAUSED && reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network — resumes automatically"
                        status == DownloadStatus.PAUSED -> "Paused by Android (code $reason)"
                        else -> null
                    },
                ))
            }
        }
    }

    suspend fun cancel(id: Long) = withContext(Dispatchers.IO) {
        manager.remove(id)
        dao.byId(id)?.let { dao.save(it.copy(status = DownloadStatus.CANCELLED.name)) }
    }

    /** Android's DownloadManager supports pause/resume for HTTP(S) downloads on API 24+. */
    fun pause(id: Long): Boolean = runCatching {
        manager.pauseDownload(id)
        true
    }.getOrDefault(false)

    fun resume(id: Long): Boolean = runCatching {
        manager.resumeDownload(id)
        true
    }.getOrDefault(false)

    suspend fun remove(id: Long) = withContext(Dispatchers.IO) {
        manager.remove(id)
        dao.deleteById(id)
    }

    suspend fun retry(item: DownloadItem, wifiOnly: Boolean, notificationsEnabled: Boolean = true): Result<Long> {
        remove(item.id)
        return enqueue(item.url, item.fileName, item.mimeType, null, wifiOnly, notificationsEnabled)
    }

    suspend fun openIntent(id: Long): Intent? = fileIntent(id, Intent.ACTION_VIEW)
    suspend fun shareIntent(id: Long): Intent? = fileIntent(id, Intent.ACTION_SEND)

    suspend fun downloadedFile(id: Long): File? = withContext(Dispatchers.IO) {
        val item = dao.byId(id)?.toModel() ?: return@withContext null
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Nebula/${item.fileName}").takeIf { it.exists() }
    }

    private suspend fun fileIntent(id: Long, action: String): Intent? {
        val file = downloadedFile(id) ?: return null
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.files", file)
        val extension = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: if (extension == "apk") "application/vnd.android.package-archive" else "*/*"
        val target = if (action == Intent.ACTION_SEND) Intent(action).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
        } else Intent(action).apply { setDataAndType(uri, mime) }
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        target.clipData = android.content.ClipData.newUri(appContext.contentResolver, file.name, uri)
        return Intent.createChooser(target, if (action == Intent.ACTION_SEND) "Share ${file.name}" else "Open ${file.name}").apply {
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
