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
        intent?.data?.toString()?.let(browserViewModel::navigate)
    }

    override fun onPause() {
        browserViewModel.persistSessions()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let(browserViewModel::navigate)
    }
}
