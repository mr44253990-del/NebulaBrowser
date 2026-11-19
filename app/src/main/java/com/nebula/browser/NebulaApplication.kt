package com.nebula.browser

import android.app.Application
import android.os.Build
import java.io.File

class NebulaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeCrashReport(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /**
     * Keeps a small, rotated crash report in app storage so Settings > Diagnostics can
     * show and share it without adb. Build type and device data make release-only
     * problems (for example R8 or WebView issues) recognisable.
     */
    private fun writeCrashReport(thread: Thread, error: Throwable) {
        val log = File(filesDir, "crash-report.log")
        if (log.length() > 256 * 1024) log.delete()
        log.appendText(
            buildString {
                append("\n=== ").append(java.util.Date()).append(" | ").append(thread.name).append(" ===\n")
                append("version ").append(BuildConfig.VERSION_NAME)
                append(" (").append(BuildConfig.VERSION_CODE).append(", ").append(BuildConfig.BUILD_TYPE).append(")\n")
                append("device ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                append(" · Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
                append(error.stackTraceToString()).append('\n')
            },
        )
        getSharedPreferences("nebula_runtime", MODE_PRIVATE)
            .edit()
            .putLong("last_crash_at", System.currentTimeMillis())
            .apply()
    }
}
