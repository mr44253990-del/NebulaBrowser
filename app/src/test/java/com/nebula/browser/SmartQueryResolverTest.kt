package com.nebula.browser

import com.nebula.browser.core.SmartQueryResolver
import com.nebula.browser.core.SmartResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartQueryResolverTest {
    private val resolver = SmartQueryResolver()

    @Test fun `domain is upgraded to https`() {
        assertEquals(SmartResult.Navigate("https://example.com"), resolver.resolve("example.com"))
    }

    @Test fun `search keyword selects engine`() {
        val result = resolver.resolve("gh compose browser")
        assertEquals(SmartResult.Navigate("https://github.com/search?q=compose+browser"), result)
    }

    @Test fun `calculator respects precedence`() {
        assertEquals(SmartResult.Answer("22"), resolver.resolve("2 + 4 * 5"))
    }

    @Test fun `calculator rejects division by zero`() {
        val result = resolver.resolve("4 / 0")
        assertTrue(result is SmartResult.Navigate)
    }

    @Test fun `unit conversion works`() {
        assertEquals(SmartResult.Answer("6.21 MI"), resolver.resolve("10 km to mi"))
    }
}
