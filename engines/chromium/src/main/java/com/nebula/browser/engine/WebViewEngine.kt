package com.nebula.browser.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Parcel
import android.print.PrintAttributes
import android.util.Log
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.nebula.browser.core.UserScript
import android.widget.FrameLayout
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

data class EnginePrivacyConfig(
    val adBlockEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val javaScriptEnabled: Boolean = true,
)

/** Android System WebView (Chromium) engine. This is not a desktop extension runtime. */
class WebViewEngine(
    private val context: Context,
    private val privacyConfig: () -> EnginePrivacyConfig,
    private val scriptProvider: () -> List<UserScript> = { emptyList() },
) : BrowserEngine {
    override fun createSession(tabId: String, privateMode: Boolean): BrowserSession =
        WebViewSession(context, tabId, privateMode, privacyConfig, scriptProvider)

    override fun clearCache() = Unit // Each session owns and clears its WebView cache.
}

@SuppressLint("SetJavaScriptEnabled")
private class WebViewSession(
    private val context: Context,
    override val tabId: String,
    private val privateMode: Boolean,
    private val privacyConfig: () -> EnginePrivacyConfig,
    private val scriptProvider: () -> List<UserScript>,
) : BrowserSession {
    private val mutableEvents = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)
    override val events: Flow<EngineEvent> = mutableEvents
    private var destroyed = false
    private var desktopMode = false
    private var previousScrollY = 0

    private val webView = object : WebView(context) {
        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            val delta = t - previousScrollY
            if (kotlin.math.abs(delta) > 24) {
                mutableEvents.tryEmit(EngineEvent.ScrollChanged(tabId, t, delta))
                previousScrollY = t
            }
        }
    }.apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.apply {
            javaScriptEnabled = privacyConfig().javaScriptEnabled
            domStorageEnabled = !privateMode
            databaseEnabled = !privateMode
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
            offscreenPreRaster = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
            cacheMode = if (privateMode) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString.replace("; wv", "")
        }
        isSaveEnabled = !privateMode
        webViewClient = createClient()
        webChromeClient = createChromeClient()
        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            mutableEvents.tryEmit(EngineEvent.DownloadRequested(
                tabId, url, fileName, mimeType, userAgent, contentDisposition, contentLength
            ))
        }
    }

    init {
        CookieManager.getInstance().apply {
            // WebView's cookie store is process-wide. Never disable normal login cookies globally.
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, !privateMode && !privacyConfig().blockThirdPartyCookies)
        }
    }

    override val canGoBack: Boolean get() = !destroyed && webView.canGoBack()
    override val canGoForward: Boolean get() = !destroyed && webView.canGoForward()
    override val currentUrl: String? get() = if (destroyed) null else webView.url

    override fun attach(container: FrameLayout) {
        if (destroyed) return
        (webView.parent as? ViewGroup)?.removeView(webView)
        if (webView.parent == null) container.addView(webView)
        webView.onResume()
    }

    override fun detach() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.onPause()
    }

    override fun loadUrl(url: String) {
        if (!destroyed) webView.loadUrl(url)
    }

    override fun goBack() { if (canGoBack) webView.goBack() }
    override fun goForward() { if (canGoForward) webView.goForward() }
    override fun reload() { if (!destroyed) webView.reload() }
    override fun stopLoading() { if (!destroyed) webView.stopLoading() }

    override fun setDesktopMode(enabled: Boolean) {
        if (destroyed) return
        desktopMode = enabled
        webView.settings.apply {
            userAgentString = if (enabled) DESKTOP_USER_AGENT else WebSettings.getDefaultUserAgent(context).replace("; wv", "")
            useWideViewPort = true
            loadWithOverviewMode = enabled
            setSupportZoom(true)
            builtInZoomControls = true
            textZoom = 100
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        }
        webView.setInitialScale(0)
        updateUserAgentMetadata(enabled)
        applyViewportMode()
    }

    private fun updateUserAgentMetadata(enabled: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
        val builder = UserAgentMetadata.Builder()
            .setMobile(!enabled)
            .setPlatform(if (enabled) "Windows" else "Android")
            .setPlatformVersion(if (enabled) "10.0.0" else android.os.Build.VERSION.RELEASE)
            .setModel(if (enabled) "" else android.os.Build.MODEL)
            .setArchitecture(if (enabled) "x86" else "")
        if (enabled) builder.setBitness(64).setWow64(true)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS)) {
            builder.setFormFactors(listOf(if (enabled) UserAgentMetadata.FORM_FACTOR_DESKTOP else UserAgentMetadata.FORM_FACTOR_MOBILE))
        }
        WebSettingsCompat.setUserAgentMetadata(webView.settings, builder.build())
    }

    private fun applyViewportMode() {
        val width = if (desktopMode) "1280" else "device-width"
        webView.evaluateJavascript("(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.setAttribute('content','width=$width, initial-scale=1.0, maximum-scale=5.0');})()", null)
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        webView.settings.javaScriptEnabled = enabled
    }

    override fun evaluateJavaScript(script: String, callback: (String?) -> Unit) {
        if (destroyed) return callback(null)
        webView.evaluateJavascript(script) { value -> callback(value?.let(::decodeJavascriptString)) }
    }

    override fun findInPage(query: String) { if (!destroyed) webView.findAllAsync(query) }
    override fun clearFind() { if (!destroyed) webView.clearMatches() }

    override fun printPage() {
        if (destroyed) return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = webView.createPrintDocumentAdapter(webView.title ?: "Nebula page")
        printManager.print("Nebula Browser", adapter, PrintAttributes.Builder().build())
    }

    override fun capturePreview(): Bitmap? {
        if (destroyed || webView.width <= 0 || webView.height <= 0) return null
        return runCatching {
            val scale = minOf(1f, 640f / webView.width)
            val bitmap = Bitmap.createBitmap((webView.width * scale).toInt().coerceAtLeast(1), (webView.height * scale * .62f).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.scale(scale, scale)
            webView.draw(canvas)
            bitmap
        }.getOrNull()
    }

    override fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        if (destroyed || webView.width <= 0 || webView.height <= 0) return callback(null)
        webView.post {
            val bitmap = runCatching {
                Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888).also { snapshot ->
                    webView.draw(android.graphics.Canvas(snapshot))
                }
            }.getOrNull()
            callback(bitmap)
        }
    }

    override fun saveState(): ByteArray? {
        if (destroyed) return null
        return runCatching {
            val bundle = Bundle()
            webView.saveState(bundle)
            val parcel = Parcel.obtain()
            try { parcel.writeBundle(bundle); parcel.marshall() } finally { parcel.recycle() }
        }.getOrNull()
    }

    override fun restoreState(data: ByteArray) {
        if (destroyed) return
        runCatching {
            val parcel = Parcel.obtain()
            try {
                parcel.unmarshall(data, 0, data.size)
                parcel.setDataPosition(0)
                val bundle = Bundle.CREATOR.createFromParcel(parcel)
                bundle.classLoader = WebView::class.java.classLoader
                webView.restoreState(bundle)
            } finally { parcel.recycle() }
        }
    }

    override fun flushCookies() = CookieManager.getInstance().flush()

    override fun destroy() {
        if (destroyed) return
        destroyed = true
        detach()
        webView.stopLoading()
        webView.clearHistory()
        if (privateMode) webView.clearCache(true)
        webView.destroy()
    }

    private fun createClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme ?: return true
            if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "view-source") return false
            mutableEvents.tryEmit(EngineEvent.ExternalRequest(tabId, request.url.toString()))
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            mutableEvents.tryEmit(EngineEvent.PageStarted(tabId, url, favicon))
            emitNavigationState()
        }

        override fun onPageFinished(view: WebView, url: String) {
            mutableEvents.tryEmit(EngineEvent.UrlChanged(tabId, url))
            mutableEvents.tryEmit(EngineEvent.LoadingChanged(tabId, false))
            emitNavigationState()
            if (privateMode) view.clearHistory()
            applyViewportMode()
            injectUserScripts(view, url)
        }

        private fun injectUserScripts(target: WebView, url: String) {
            scriptProvider().filter { it.enabled && it.code.length <= 1_000_000 && it.grants.all { grant -> grant == "none" } }
                .filter { script -> script.matches.any { pattern -> matchesPattern(url, pattern) } }
                .forEach { script ->
                    val wrapped: String = "(function(){'use strict';try{" + script.code +
                        "\n}catch(e){console.error('Nebula userscript error',e.message);}})();"
                    target.evaluateJavascript(wrapped, null)
                }
        }

        private fun matchesPattern(url: String, pattern: String): Boolean {
            if (pattern == "<all_urls>" || pattern == "*") return url.startsWith("http")
            val regex = pattern.split("*").joinToString(".*") { Regex.escape(it) }
            return runCatching { Regex(regex, RegexOption.IGNORE_CASE).matches(url) }.getOrDefault(false)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            mutableEvents.tryEmit(EngineEvent.Error(tabId, "Secure connection failed: certificate validation error"))
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val host = request.url.host?.lowercase() ?: return null
            if (privacyConfig().adBlockEnabled && isBlockedHost(host)) {
                mutableEvents.tryEmit(EngineEvent.BlockedRequest(tabId, host))
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
            }
            return null
        }
    }

    private fun createChromeClient() = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            mutableEvents.tryEmit(EngineEvent.ProgressChanged(tabId, newProgress))
            mutableEvents.tryEmit(EngineEvent.LoadingChanged(tabId, newProgress < 100))
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            mutableEvents.tryEmit(EngineEvent.TitleChanged(tabId, title))
        }

        override fun onReceivedIcon(view: WebView, icon: Bitmap?) = Unit

        override fun onPermissionRequest(request: PermissionRequest) {
            val supported = request.resources.filter {
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE || it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }
            if (supported.isEmpty()) return request.deny()
            val androidPermissions = supported.mapNotNull {
                when (it) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> android.Manifest.permission.CAMERA
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> android.Manifest.permission.RECORD_AUDIO
                    else -> null
                }
            }.distinct()
            mutableEvents.tryEmit(EngineEvent.SitePermissionRequested(SitePermissionRequest(
                tabId, request.origin.toString(), supported, androidPermissions,
                grant = { request.grant(supported.toTypedArray()) }, deny = { request.deny() },
            )))
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            mutableEvents.tryEmit(EngineEvent.SitePermissionRequested(SitePermissionRequest(
                tabId, origin, listOf("location"), listOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION),
                grant = { callback.invoke(origin, true, false) }, deny = { callback.invoke(origin, false, false) },
            )))
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            mutableEvents.tryEmit(EngineEvent.ConsoleMessage(tabId, consoleMessage.message(), consoleMessage.sourceId() ?: "", consoleMessage.lineNumber()))
            return true
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            mutableEvents.tryEmit(EngineEvent.FileChooserRequested(
                tabId,
                filePathCallback,
                fileChooserParams.acceptTypes,
                fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE,
            ))
            return true
        }
    }

    private fun emitNavigationState() {
        mutableEvents.tryEmit(EngineEvent.NavigationStateChanged(tabId, webView.canGoBack(), webView.canGoForward()))
    }

    private fun decodeJavascriptString(raw: String): String = runCatching {
        org.json.JSONObject("{value:$raw}").optString("value")
    }.getOrDefault(raw)

    private companion object {
        const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        val BLOCKED_HOST_SUFFIXES = setOf(
            "doubleclick.net", "google-analytics.com", "googlesyndication.com", "googletagmanager.com",
            "facebook.net", "adservice.google.com", "ads.yahoo.com", "amazon-adsystem.com",
            "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net", "outbrain.com", "taboola.com",
            "scorecardresearch.com", "quantserve.com", "chartbeat.com", "hotjar.com", "mouseflow.com",
            "branch.io", "appsflyer.com", "adjust.com", "mixpanel.com", "segment.io", "fullstory.com",
        )

        fun isBlockedHost(host: String): Boolean = BLOCKED_HOST_SUFFIXES.any { host == it || host.endsWith(".$it") }
    }
}
