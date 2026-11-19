package com.nebula.browser.engine

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ValueCallback
import android.widget.FrameLayout
import kotlinx.coroutines.flow.Flow

interface BrowserEngine {
    fun createSession(tabId: String, privateMode: Boolean): BrowserSession
    fun clearCache()
}

interface BrowserSession {
    val tabId: String
    val events: Flow<EngineEvent>
    val canGoBack: Boolean
    val canGoForward: Boolean
    val currentUrl: String?
    fun attach(container: FrameLayout)
    fun detach()
    fun loadUrl(url: String)
    fun goBack()
    fun goForward()
    fun reload()
    fun stopLoading()
    fun setDesktopMode(enabled: Boolean)
    fun setJavaScriptEnabled(enabled: Boolean)
    fun evaluateJavaScript(script: String, callback: (String?) -> Unit)
    fun findInPage(query: String)
    fun clearFind()
    fun printPage()
    fun capturePreview(): Bitmap?
    fun saveState(): ByteArray?
    fun restoreState(data: ByteArray)
    fun flushCookies()
    fun destroy()
}

data class SitePermissionRequest(
    val tabId: String,
    val origin: String,
    val resources: List<String>,
    val androidPermissions: List<String>,
    val grant: () -> Unit,
    val deny: () -> Unit,
)

sealed interface EngineEvent {
    data class UrlChanged(val tabId: String, val url: String) : EngineEvent
    data class TitleChanged(val tabId: String, val title: String) : EngineEvent
    data class ProgressChanged(val tabId: String, val progress: Int) : EngineEvent
    data class LoadingChanged(val tabId: String, val loading: Boolean) : EngineEvent
    data class NavigationStateChanged(val tabId: String, val canBack: Boolean, val canForward: Boolean) : EngineEvent
    data class PageStarted(val tabId: String, val url: String, val icon: Bitmap?) : EngineEvent
    data class Error(val tabId: String, val message: String) : EngineEvent
    data class DownloadRequested(
        val tabId: String,
        val url: String,
        val fileName: String,
        val mimeType: String?,
        val userAgent: String?,
        val contentDisposition: String?,
        val contentLength: Long,
    ) : EngineEvent
    data class FileChooserRequested(
        val tabId: String,
        val callback: ValueCallback<Array<Uri>>,
        val acceptTypes: Array<String>,
        val allowMultiple: Boolean,
    ) : EngineEvent
    data class ExternalRequest(val tabId: String, val url: String) : EngineEvent
    data class BlockedRequest(val tabId: String, val host: String) : EngineEvent
    data class SitePermissionRequested(val request: SitePermissionRequest) : EngineEvent
    data class ConsoleMessage(val tabId: String, val message: String, val source: String, val line: Int) : EngineEvent
    data class ScrollChanged(val tabId: String, val scrollY: Int, val deltaY: Int) : EngineEvent
}
