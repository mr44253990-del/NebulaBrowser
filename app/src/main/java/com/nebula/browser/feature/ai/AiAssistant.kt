package com.nebula.browser.feature.ai

import com.nebula.browser.core.SecretStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiAssistant(private val secrets: SecretStore) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    var apiKey: String
        get() = secrets.get(AI_KEY).orEmpty()
        set(value) = if (value.isBlank()) secrets.remove(AI_KEY) else secrets.put(AI_KEY, value.trim())

    suspend fun askAboutPage(
        endpoint: String,
        model: String,
        temperature: Float,
        pageTitle: String,
        pageUrl: String,
        pageText: String,
        question: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Add an API key in AI settings" }
            require(endpoint.startsWith("https://")) { "The AI endpoint must use HTTPS" }
            require(pageText.isNotBlank()) { "No readable page text was found" }
            val safeEndpoint = endpoint.trimEnd('/') + "/chat/completions"
            val prompt = buildString {
                append("You are Nebula, a careful browser assistant. Answer only from the supplied page when possible. ")
                append("Say when the page does not contain enough information. Never invent citations.\n\n")
                append("Page title: ").append(pageTitle).append('\n')
                append("Page URL: ").append(pageUrl).append("\n\n")
                append("Page content:\n").append(pageText.take(45_000))
            }
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", temperature.toDouble())
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", prompt))
                    put(JSONObject().put("role", "user").put("content", question))
                })
            }.toString()
            val request = Request.Builder()
                .url(safeEndpoint)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(responseBody).getJSONObject("error").getString("message")
                    }.getOrDefault("HTTP ${response.code}")
                    error("AI provider: $message")
                }
                val json = JSONObject(responseBody)
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            }
        }
    }



    suspend fun createBrowserPlan(
        endpoint: String,
        model: String,
        temperature: Float,
        task: String,
    ): Result<List<AgentAction>> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Add an API key in AI settings" }
            require(endpoint.startsWith("https://")) { "The AI endpoint must use HTTPS" }
            require(task.length in 3..4000) { "Task must be between 3 and 4000 characters" }
            val instructions = """
                You are a planning API for an Android browser. Convert the user's research task into a small safe plan.
                Return ONLY JSON: {"steps":[{"action":"open_urls","title":"Open sources","urls":["https://example.com"]},{"action":"collect_pages","title":"Collect readable page text","urls":[]},{"action":"export_pdf","title":"Export report","urls":[]}]}.
                Allowed actions: open_urls, go_back, collect_pages, export_text, export_pdf. No clicks, forms, credentials, payments, JavaScript, downloads, account changes, or arbitrary code.
                Use at most 12 total URLs and 16 steps. Use direct HTTPS URLs from well-known relevant sites when possible; do not invent deep links or citations.
                Every research plan should include collect_pages and exactly one export action.
            """.trimIndent()
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", temperature.toDouble().coerceAtMost(0.5))
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", instructions))
                    put(JSONObject().put("role", "user").put("content", task))
                })
            }.toString()
            val request = Request.Builder()
                .url(endpoint.trimEnd('/') + "/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("AI planner: HTTP ${response.code} ${responseBody.take(300)}")
                val content = JSONObject(responseBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                val root = JSONObject(content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
                val steps = root.getJSONArray("steps")
                val allowed = setOf("open_urls", "go_back", "collect_pages", "export_text", "export_pdf")
                buildList {
                    var urlBudget = 0
                    for (index in 0 until minOf(steps.length(), 16)) {
                        val item = steps.getJSONObject(index)
                        val action = item.getString("action")
                        require(action in allowed) { "Planner returned a blocked action: $action" }
                        val urls = item.optJSONArray("urls")?.let { array ->
                            List(array.length()) { array.getString(it) }.filter { it.startsWith("https://") }
                        }.orEmpty()
                        urlBudget += urls.size
                        require(urlBudget <= 12) { "The 12-tab safety limit was exceeded" }
                        add(AgentAction(action, item.optString("title").ifBlank { action.replace('_', ' ') }, urls))
                    }
                }
            }
        }
    }

    private companion object { const val AI_KEY = "openai_compatible_key" }
}
