package com.nebula.browser

import com.nebula.browser.feature.extensions.UserScriptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserScriptParserTest {
    @Test fun `grant none script is enabled with declared matches`() {
        val script = UserScriptParser.parse("// ==UserScript==\n// @name Test\n// @match https://example.com/*\n// @grant none\n// ==/UserScript==\nconsole.log('ok')", "test.user.js")
        assertEquals("Test", script.name)
        assertTrue(script.enabled)
        assertEquals(listOf("https://example.com/*"), script.matches)
    }

    @Test fun `GM grants keep script disabled`() {
        val script = UserScriptParser.parse("// ==UserScript==\n// @match *://*/*\n// @grant GM_xmlhttpRequest\n// ==/UserScript==\n", "unsafe.user.js")
        assertFalse(script.enabled)
    }
}
