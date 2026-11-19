package com.nebula.browser.core

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "New tab",
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    val workspaceId: String = "personal",
    val groupName: String? = null,
    val desktopMode: Boolean = false,
    val previewPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
)

data class Workspace(
    val id: String,
    val name: String,
    val color: Long,
    val icon: String = "folder",
)

data class Bookmark(
    val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String = "Bookmarks",
    val tags: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class HistoryEntry(
    val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
)

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, SUCCESSFUL, FAILED, CANCELLED }

data class DownloadItem(
    val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val createdAt: Long = System.currentTimeMillis(),
    val error: String? = null,
) {
    val progress: Float
        get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

data class SearchEngine(
    val id: String,
    val name: String,
    val queryUrl: String,
    val keyword: String,
    val suggestionUrl: String? = null,
) {
    fun search(query: String): String = queryUrl.replace("%s", query.encodeUrlParameter())
}

data class PasswordCredential(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)

data class HomeShortcut(
    val id: String,
    val title: String,
    val url: String,
    val symbol: String,
    val color: Long = 0xFF7C4DFF,
)

data class BrowserSettings(
    val searchEngineId: String = "duckduckgo",
    val customSearchEngines: List<SearchEngine> = emptyList(),
    val darkTheme: Boolean = true,
    val bottomToolbar: Boolean = true,
    val homeColumns: Int = 4,
    val showRecentCard: Boolean = true,
    val javaScriptEnabled: Boolean = true,
    val adBlockEnabled: Boolean = true,
    val httpsFirst: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val clearPrivateOnExit: Boolean = true,
    val downloadWifiOnly: Boolean = false,
    val aiEnabled: Boolean = false,
    val aiEndpoint: String = "https://api.openai.com/v1",
    val aiModel: String = "gpt-4o-mini",
    val aiTemperature: Float = 0.3f,
    val developerMode: Boolean = false,
    val shortcuts: List<HomeShortcut> = DefaultHomeShortcuts.all,
    val immersiveMode: Boolean = false,
    val autoHideToolbar: Boolean = true,
    val edgeMenuEnabled: Boolean = false,
    val edgeMenuSide: String = "left",
    val threeFingerMenu: Boolean = true,
    val liquidEffects: Boolean = true,
    val homeShowFrequent: Boolean = true,
    val homeShowTips: Boolean = true,
    val backgroundStyle: String = "nebula",
    val downloadNotifications: Boolean = true,
    val dynamicColors: Boolean = false,
    val accentColor: String = "violet",
    val reduceMotion: Boolean = false,
)

data class ExtensionReport(
    val fileName: String,
    val name: String,
    val version: String,
    val manifestVersion: Int,
    val permissions: List<String>,
    val hostPermissions: List<String>,
    val detectedApis: List<String>,
    val warnings: List<String>,
    val chromiumCompatible: Boolean,
    val geckoCompatible: Boolean,
    val runtimeAvailable: Boolean = false,
)

object DefaultWorkspaces {
    val all = listOf(
        Workspace("personal", "Personal", 0xFF6750A4),
        Workspace("development", "Development", 0xFF006A6A),
        Workspace("research", "Research", 0xFF825500),
        Workspace("entertainment", "Entertainment", 0xFF984061),
        Workspace("shopping", "Shopping", 0xFF00639B),
    )
}

object DefaultSearchEngines {
    val all = listOf(
        SearchEngine("google", "Google", "https://www.google.com/search?q=%s", "g"),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s", "b"),
        SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "d"),
        SearchEngine("brave", "Brave Search", "https://search.brave.com/search?q=%s", "br"),
        SearchEngine("startpage", "Startpage", "https://www.startpage.com/sp/search?query=%s", "sp"),
        SearchEngine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s", "e"),
        SearchEngine("github", "GitHub", "https://github.com/search?q=%s", "gh"),
        SearchEngine("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s", "w"),
    )
}

fun String.encodeUrlParameter(): String = java.net.URLEncoder.encode(this, "UTF-8")


object DefaultHomeShortcuts {
    val all = listOf(
        HomeShortcut("google", "Google", "https://google.com", "G", 0xFF4285F4),
        HomeShortcut("youtube", "YouTube", "https://youtube.com", "▶", 0xFFFF0033),
        HomeShortcut("wikipedia", "Wikipedia", "https://wikipedia.org", "W", 0xFFEEEEEE),
        HomeShortcut("github", "GitHub", "https://github.com", "GH", 0xFFF5F5F5),
        HomeShortcut("gmail", "Gmail", "https://mail.google.com", "M", 0xFFEA4335),
        HomeShortcut("drive", "Drive", "https://drive.google.com", "D", 0xFF34A853),
        HomeShortcut("facebook", "Facebook", "https://facebook.com", "f", 0xFF1877F2),
    )
}


data class SearchQuery(
    val query: String,
    val count: Int,
    val lastSearchedAt: Long,
)

data class UserScript(
    val id: String,
    val name: String,
    val version: String,
    val code: String,
    val matches: List<String>,
    val grants: List<String>,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
