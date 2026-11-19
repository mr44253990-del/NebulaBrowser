package com.nebula.browser.core

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsDataStore by preferencesDataStore(name = "browser_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<BrowserSettings> = context.settingsDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::readSettings)

    suspend fun update(transform: (BrowserSettings) -> BrowserSettings) {
        context.settingsDataStore.edit { preferences ->
            val next = transform(readSettings(preferences))
            preferences[Keys.SEARCH_ENGINE] = next.searchEngineId
            preferences[Keys.CUSTOM_ENGINES] = encodeEngines(next.customSearchEngines)
            preferences[Keys.DARK_THEME] = next.darkTheme
            preferences[Keys.BOTTOM_TOOLBAR] = next.bottomToolbar
            preferences[Keys.HOME_COLUMNS] = next.homeColumns
            preferences[Keys.SHOW_RECENT] = next.showRecentCard
            preferences[Keys.JAVASCRIPT] = next.javaScriptEnabled
            preferences[Keys.AD_BLOCK] = next.adBlockEnabled
            preferences[Keys.HTTPS_FIRST] = next.httpsFirst
            preferences[Keys.THIRD_PARTY_COOKIES] = next.blockThirdPartyCookies
            preferences[Keys.CLEAR_PRIVATE] = next.clearPrivateOnExit
            preferences[Keys.WIFI_ONLY] = next.downloadWifiOnly
            preferences[Keys.AI_ENABLED] = next.aiEnabled
            preferences[Keys.AI_ENDPOINT] = next.aiEndpoint
            preferences[Keys.AI_MODEL] = next.aiModel
            preferences[Keys.AI_TEMPERATURE] = next.aiTemperature
            preferences[Keys.DEVELOPER_MODE] = next.developerMode
            preferences[Keys.SHORTCUTS] = encodeShortcuts(next.shortcuts)
        }
    }

    private fun readSettings(p: Preferences) = BrowserSettings(
        searchEngineId = p[Keys.SEARCH_ENGINE] ?: "duckduckgo",
        customSearchEngines = decodeEngines(p[Keys.CUSTOM_ENGINES]),
        darkTheme = p[Keys.DARK_THEME] ?: true,
        bottomToolbar = p[Keys.BOTTOM_TOOLBAR] ?: true,
        homeColumns = p[Keys.HOME_COLUMNS] ?: 4,
        showRecentCard = p[Keys.SHOW_RECENT] ?: true,
        javaScriptEnabled = p[Keys.JAVASCRIPT] ?: true,
        adBlockEnabled = p[Keys.AD_BLOCK] ?: true,
        httpsFirst = p[Keys.HTTPS_FIRST] ?: true,
        blockThirdPartyCookies = p[Keys.THIRD_PARTY_COOKIES] ?: true,
        clearPrivateOnExit = p[Keys.CLEAR_PRIVATE] ?: true,
        downloadWifiOnly = p[Keys.WIFI_ONLY] ?: false,
        aiEnabled = p[Keys.AI_ENABLED] ?: false,
        aiEndpoint = p[Keys.AI_ENDPOINT] ?: "https://api.openai.com/v1",
        aiModel = p[Keys.AI_MODEL] ?: "gpt-4o-mini",
        aiTemperature = p[Keys.AI_TEMPERATURE] ?: 0.3f,
        developerMode = p[Keys.DEVELOPER_MODE] ?: false,
        shortcuts = decodeShortcuts(p[Keys.SHORTCUTS]),
    )

    private fun encodeEngines(engines: List<SearchEngine>): String = JSONArray().apply {
        engines.forEach { engine ->
            put(JSONObject().apply {
                put("id", engine.id)
                put("name", engine.name)
                put("url", engine.queryUrl)
                put("keyword", engine.keyword)
                put("suggestions", engine.suggestionUrl ?: JSONObject.NULL)
            })
        }
    }.toString()

    private fun decodeEngines(raw: String?): List<SearchEngine> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(SearchEngine(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    queryUrl = item.getString("url"),
                    keyword = item.getString("keyword"),
                    suggestionUrl = if (item.isNull("suggestions")) null else item.getString("suggestions"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun encodeShortcuts(shortcuts: List<HomeShortcut>): String = JSONArray().apply {
        shortcuts.forEach { shortcut -> put(JSONObject().apply {
            put("id", shortcut.id); put("title", shortcut.title); put("url", shortcut.url)
            put("symbol", shortcut.symbol); put("color", shortcut.color)
        }) }
    }.toString()

    private fun decodeShortcuts(raw: String?): List<HomeShortcut> {
        if (raw == null) return DefaultHomeShortcuts.all
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(HomeShortcut(item.getString("id"), item.getString("title"), item.getString("url"),
                        item.optString("symbol", item.getString("title").take(1)), item.optLong("color", 0xFF7C4DFF)))
                }
            }
        }.getOrDefault(DefaultHomeShortcuts.all)
    }

    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_ENGINES = stringPreferencesKey("custom_engines")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val BOTTOM_TOOLBAR = booleanPreferencesKey("bottom_toolbar")
        val HOME_COLUMNS = intPreferencesKey("home_columns")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val JAVASCRIPT = booleanPreferencesKey("javascript")
        val AD_BLOCK = booleanPreferencesKey("ad_block")
        val HTTPS_FIRST = booleanPreferencesKey("https_first")
        val THIRD_PARTY_COOKIES = booleanPreferencesKey("third_party_cookies")
        val CLEAR_PRIVATE = booleanPreferencesKey("clear_private")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val SHORTCUTS = stringPreferencesKey("home_shortcuts")
    }
}
