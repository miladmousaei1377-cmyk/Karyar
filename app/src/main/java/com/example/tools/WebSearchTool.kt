package com.example.tools

import com.example.core.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class WebSearchTool : BaseTool() {
    override val name = "web_search"
    override val description =
        "جستجو در اینترنت برای اطلاعات به‌روز. برای اخبار، قیمت‌ها، و اطلاعات فعلی استفاده کن."
    override val inputSchema: Map<String, Map<String, Any>> = mapOf(
        "query" to mapOf("type" to "string", "description" to "عبارت جستجو"),
        "num_results" to mapOf(
            "type" to "integer",
            "description" to "تعداد نتایج (۱ تا ۵، پیش‌فرض: ۳)"
        )
    )
    override val requiredFields = listOf("query")

    private val client = OkHttpClient()

    override suspend fun execute(input: Map<String, Any>): String = withContext(Dispatchers.IO) {
        val query = input["query"] as? String ?: return@withContext "عبارت جستجو نامعتبر"
        val numResults = (input["num_results"] as? Number)?.toInt() ?: 3

        val apiKey = AppConfig.tavilyApiKey
        if (apiKey.isBlank()) return@withContext "کلید API جستجو تنظیم نشده است."

        runCatching {
            val body = JSONObject().apply {
                put("api_key", apiKey)
                put("query", query)
                put("max_results", numResults.coerceIn(1, 5))
                put("include_answer", true)
            }.toString()

            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching "خطا در جستجو: ${response.code}"
                val json = JSONObject(response.body?.string() ?: return@runCatching "پاسخ خالی")

                val sb = StringBuilder()
                json.optString("answer").takeIf { it.isNotBlank() }?.let {
                    sb.appendLine("خلاصه: $it\n")
                }
                val results = json.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until minOf(results.length(), numResults)) {
                        val r = results.getJSONObject(i)
                        sb.appendLine("${i + 1}. ${r.optString("title")}")
                        val content = r.optString("content").take(250)
                        sb.appendLine("   $content...")
                        sb.appendLine("   منبع: ${r.optString("url")}")
                    }
                }
                sb.toString().ifBlank { "نتیجه‌ای یافت نشد." }
            }
        }.getOrElse { "خطا در جستجو: ${it.message}" }
    }
}
