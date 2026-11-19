package com.nebula.browser.core

import java.net.URI
import kotlin.math.roundToInt

sealed interface SmartResult {
    data class Navigate(val url: String) : SmartResult
    data class Answer(val text: String) : SmartResult
}

class SmartQueryResolver(
    private val engines: List<SearchEngine> = DefaultSearchEngines.all,
    private val defaultEngineId: String = "duckduckgo",
    private val httpsFirst: Boolean = true,
) {
    fun resolve(rawInput: String): SmartResult {
        val input = rawInput.trim()
        require(input.isNotEmpty()) { "Query cannot be empty" }
        calculate(input)?.let { return SmartResult.Answer(it) }
        convertUnits(input)?.let { return SmartResult.Answer(it) }
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return SmartResult.Navigate(input)
        }
        if (looksLikeDomain(input)) {
            return SmartResult.Navigate((if (httpsFirst) "https://" else "http://") + input)
        }
        val parts = input.split(Regex("\\s+"), limit = 2)
        val keywordEngine = engines.firstOrNull { it.keyword.equals(parts[0], true) }
        if (keywordEngine != null && parts.size == 2) {
            return SmartResult.Navigate(keywordEngine.search(parts[1]))
        }
        val engine = engines.firstOrNull { it.id == defaultEngineId } ?: engines.first()
        return SmartResult.Navigate(engine.search(input))
    }

    private fun looksLikeDomain(value: String): Boolean {
        if (value.contains(' ') || value.startsWith("localhost")) return value == "localhost" || value.startsWith("localhost:")
        val candidate = runCatching { URI("https://$value") }.getOrNull() ?: return false
        return candidate.host?.contains('.') == true && !value.endsWith('.')
    }

    private fun convertUnits(input: String): String? {
        val match = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*(km|mi|kg|lb|cm|in|c|f)\\s+(?:to|in)\\s+(km|mi|kg|lb|cm|in|c|f)$", RegexOption.IGNORE_CASE)
            .matchEntire(input) ?: return null
        val value = match.groupValues[1].toDouble()
        val from = match.groupValues[2].lowercase()
        val to = match.groupValues[3].lowercase()
        val result = when (from to to) {
            "km" to "mi" -> value * 0.621371
            "mi" to "km" -> value * 1.609344
            "kg" to "lb" -> value * 2.20462262
            "lb" to "kg" -> value * 0.45359237
            "cm" to "in" -> value / 2.54
            "in" to "cm" -> value * 2.54
            "c" to "f" -> value * 9 / 5 + 32
            "f" to "c" -> (value - 32) * 5 / 9
            else -> return null
        }
        return "${((result * 100).roundToInt() / 100.0)} ${to.uppercase()}"
    }

    private fun calculate(input: String): String? {
        if (!input.matches(Regex("[0-9+\\-*/(). %]+")) || !input.any { it in "+-*/%" }) return null
        return runCatching {
            val parser = ArithmeticParser(input)
            val value = parser.parse()
            if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        }.getOrNull()
    }
}

private class ArithmeticParser(expression: String) {
    private val tokens = expression.filterNot { it.isWhitespace() }
    private var position = 0

    fun parse(): Double {
        val value = expression()
        check(position == tokens.length) { "Invalid expression" }
        return value
    }

    private fun expression(): Double {
        var value = term()
        while (position < tokens.length) {
            value = when (tokens[position]) {
                '+' -> { position++; value + term() }
                '-' -> { position++; value - term() }
                else -> return value
            }
        }
        return value
    }

    private fun term(): Double {
        var value = factor()
        while (position < tokens.length) {
            value = when (tokens[position]) {
                '*' -> { position++; value * factor() }
                '/' -> { position++; val divisor = factor(); check(divisor != 0.0); value / divisor }
                '%' -> { position++; value % factor() }
                else -> return value
            }
        }
        return value
    }

    private fun factor(): Double {
        if (position < tokens.length && tokens[position] == '-') {
            position++
            return -factor()
        }
        if (position < tokens.length && tokens[position] == '(') {
            position++
            val result = expression()
            check(position < tokens.length && tokens[position++] == ')')
            return result
        }
        val start = position
        while (position < tokens.length && (tokens[position].isDigit() || tokens[position] == '.')) position++
        check(start != position) { "Number expected" }
        return tokens.substring(start, position).toDouble()
    }
}
