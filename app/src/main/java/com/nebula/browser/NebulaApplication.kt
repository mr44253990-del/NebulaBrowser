package com.nebula.browser

import android.app.Application
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
    }
}
