package com.nebula.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.nebula.browser.ui.NebulaApp

class MainActivity : FragmentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
