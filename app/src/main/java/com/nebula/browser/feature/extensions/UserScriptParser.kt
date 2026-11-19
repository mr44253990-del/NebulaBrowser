package com.nebula.browser.feature.extensions

import com.nebula.browser.core.UserScript
import java.util.UUID

object UserScriptParser {
    fun parse(code: String, fallbackName: String): UserScript {
        require(code.length <= 1_000_000) { "Userscript exceeds the 1 MB safety limit" }
        val metadata = Regex("==UserScript==(.*?)==/UserScript==", RegexOption.DOT_MATCHES_ALL).find(code)?.groupValues?.get(1).orEmpty()
        fun values(key: String) = Regex("^\\s*//\\s*@$key\\s+(.+)$", RegexOption.MULTILINE).findAll(metadata).map { it.groupValues[1].trim() }.toList()
        val matches = values("match") + values("include")
        require(matches.isNotEmpty()) { "Userscript needs at least one @match or @include rule" }
        val grants = values("grant").ifEmpty { listOf("none") }
        return UserScript(
            id = UUID.randomUUID().toString(),
            name = values("name").firstOrNull() ?: fallbackName.substringBeforeLast('.'),
            version = values("version").firstOrNull() ?: "1.0",
            code = code,
            matches = matches.take(30),
            grants = grants.distinct(),
            enabled = grants.all { it == "none" },
        )
    }
}
