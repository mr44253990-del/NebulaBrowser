package com.nebula.browser

import com.nebula.browser.core.Bookmark
import com.nebula.browser.util.bookmarksFromHtml
import com.nebula.browser.util.bookmarksToHtml
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkHtmlTest {
    @Test fun `bookmarks survive html export and import`() {
        val original = Bookmark(title = "Kotlin & Android", url = "https://example.com/?a=1&b=2")
        val imported = bookmarksFromHtml(bookmarksToHtml(listOf(original)))
        assertEquals(1, imported.size)
        assertEquals(original.title, imported.single().title)
        assertEquals(original.url, imported.single().url)
    }
}
