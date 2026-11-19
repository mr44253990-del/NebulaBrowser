package com.nebula.browser.util

import com.nebula.browser.core.Bookmark

fun bookmarksToHtml(bookmarks: List<Bookmark>): String = buildString {
    append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n")
    bookmarks.forEach {
        append("  <DT><A HREF=\"").append(it.url.escapeHtml()).append("\">")
            .append(it.title.escapeHtml()).append("</A>\n")
    }
    append("</DL><p>")
}

fun bookmarksFromHtml(html: String): List<Bookmark> {
    val pattern = Regex("<A\\s+[^>]*HREF=[\"']([^\"']+)[\"'][^>]*>(.*?)</A>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    return pattern.findAll(html).map { match ->
        Bookmark(
            url = match.groupValues[1].unescapeHtml(),
            title = match.groupValues[2].replace(Regex("<[^>]+>"), "").unescapeHtml().ifBlank { match.groupValues[1] },
        )
    }.filter { it.url.startsWith("http://") || it.url.startsWith("https://") }.toList()
}

private fun String.escapeHtml() = replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
private fun String.unescapeHtml() = replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
