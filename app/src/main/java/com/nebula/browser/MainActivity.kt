package com.nebula.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nebula.browser.ui.NebulaApp

class MainActivity : FragmentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.rgb(5, 3, 11)
        window.navigationBarColor = android.graphics.Color.rgb(5, 3, 11)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { NebulaApp(browserViewModel) }
        handleIntent(intent)
    }

    override fun onPause() {
        browserViewModel.persistSessions()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Launcher shortcuts and external links land here. */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            ACTION_NEW_TAB -> browserViewModel.createTab()
            ACTION_NEW_PRIVATE_TAB -> browserViewModel.createTab(privateMode = true)
            ACTION_BOOKMARKS -> browserViewModel.openRoute("bookmarks")
            ACTION_TABS -> browserViewModel.openRoute("tabs")
            else -> intent.data?.toString()?.takeIf { it.isNotBlank() }?.let(browserViewModel::navigate)
        }
    }

    companion object {
        const val ACTION_NEW_TAB = "com.nebula.browser.action.NEW_TAB"
        const val ACTION_NEW_PRIVATE_TAB = "com.nebula.browser.action.NEW_PRIVATE_TAB"
        const val ACTION_BOOKMARKS = "com.nebula.browser.action.BOOKMARKS"
        const val ACTION_TABS = "com.nebula.browser.action.TABS"
    }
}
