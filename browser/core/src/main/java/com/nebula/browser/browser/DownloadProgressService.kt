package com.nebula.browser.browser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/** Live progress UI only. Android DownloadManager independently owns the resilient transfer. */
class DownloadProgressService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val manager by lazy { getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    private val notifications by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val previousBytes = mutableMapOf<Long, Pair<Long, Long>>()
    private val speedByDownload = mutableMapOf<Long, Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf(); return START_NOT_STICKY
        }
        createChannel()
        startMonitoringOnce(startId)
        return START_NOT_STICKY
    }

    @Volatile private var running = false
    private fun startMonitoringOnce(startId: Int) {
        if (running) return
        running = true
        scope.launch {
            startForegroundCompat(SUMMARY_ID, buildNotification(SUMMARY_ID, "Nebula downloads", "Preparing download…", 0, true))
            try {
                while (isActive) {
                    val snapshots = activeSnapshots()
                    snapshots.forEach { snapshot ->
                        notifications.notify(snapshot.id.toInt(), snapshotNotification(snapshot))
                    }
                    if (snapshots.isEmpty()) break
                    delay(1_000)
                }
            } finally {
                running = false
                stopForeground(false)
                notifications.cancel(SUMMARY_ID)
                stopSelf(startId)
            }
        }
    }

    private fun activeSnapshots(): List<DownloadSnapshot> {
        val result = mutableListOf<DownloadSnapshot>()
        manager.query(DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED)).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0)
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val now = System.currentTimeMillis()
                val previous = previousBytes.put(id, bytes to now)
                if (previous != null && now > previous.second) {
                    speedByDownload[id] = ((bytes - previous.first).coerceAtLeast(0) * 1000 / (now - previous.second))
                }
                result += DownloadSnapshot(
                    id = id,
                    title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "Download",
                    bytes = bytes, total = total,
                    speed = speedByDownload[id] ?: 0,
                    status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                )
            }
        }
        return result
    }

    private fun snapshotNotification(snapshot: DownloadSnapshot): Notification {
        val percent = if (snapshot.total > 0) (snapshot.bytes * 100 / snapshot.total).toInt().coerceIn(0, 100) else 0
        val status = when (snapshot.status) {
            DownloadManager.STATUS_PAUSED -> if (snapshot.reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK) "Waiting for network · auto-resume enabled" else "Paused by system · ${snapshot.reason}"
            DownloadManager.STATUS_PENDING -> "Queued…"
            else -> if (snapshot.total > 0) "$percent% · ${formatSpeed(snapshot.speed)} · ETA ${formatEta(snapshot.remainingEta)}" else "${formatBytes(snapshot.bytes)} · ${formatSpeed(snapshot.speed)}"
        }
        return buildNotification(snapshot.id.toInt(), snapshot.title, status, percent, snapshot.total <= 0)
    }

    private fun buildNotification(id: Int, title: String, text: String, percent: Int, indeterminate: Boolean): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title).setContentText(text)
            .setProgress(100, if (indeterminate) 0 else percent, indeterminate)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launch)
            .build()
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 29) startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(id, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            notifications.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Download progress", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live download percentage, speed and network status"
            })
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15 dataSync timeout contract: stop promptly; system downloads continue independently.
        stopSelf(startId)
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private data class DownloadSnapshot(val id: Long, val title: String, val bytes: Long, val total: Long, val speed: Long, val status: Int, val reason: Int) {
        val remainingEta: Long get() = if (speed > 0 && total > bytes) (total - bytes) / speed else -1
    }

    companion object {
        const val ACTION_START = "com.nebula.browser.action.DOWNLOAD_MONITOR"
        const val ACTION_STOP = "com.nebula.browser.action.DOWNLOAD_MONITOR_STOP"
        private const val CHANNEL_ID = "nebula_download_progress"
        private const val SUMMARY_ID = 7_700

        fun formatBytes(bytes: Long): String = when {
            bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
        fun formatSpeed(bytes: Long): String = if (bytes > 0) "${formatBytes(bytes)}/s" else "measuring speed"
        fun formatEta(seconds: Long): String = when {
            seconds < 0 -> "calculating"
            seconds < 60 -> "${seconds}s"
            else -> "${seconds / 60}m ${seconds % 60}s"
        }
    }
}
