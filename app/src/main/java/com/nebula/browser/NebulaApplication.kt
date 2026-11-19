package com.nebula.browser

import android.app.Application
import android.os.Looper
import android.webkit.WebView
import java.io.File

class NebulaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val log = File(filesDir, "crash-report.log")
                log.appendText("\n=== ${java.util.Date()} | ${thread.name} ===\n${error.stackTraceToString()}\n")
            }
            previous?.uncaughtException(thread, error)
        }
        prewarmWebView()
    }

    /**
     * Starts the System WebView provider while the activity is still inflating its
     * first frame, so the first page load does not pay the provider startup cost.
     * A throwaway WebView on a low-priority looper thread is enough to warm it.
     */
    private fun prewarmWebView() {
        runCatching {
            Thread({
                Looper.prepare()
                runCatching {
                    WebView(this).destroy()
                }
                Looper.myLooper()?.quitSafely()
            }, "nebula-webview-prewarm").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
            }.start()
        }
    }
}
