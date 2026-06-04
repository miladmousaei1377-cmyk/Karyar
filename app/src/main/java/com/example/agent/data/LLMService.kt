package com.example.agent.data

import com.example.agent.domain.models.ApiMessage
import com.example.agent.domain.models.LLMResponse
import com.example.agent.domain.models.ToolCallRequest
import com.example.core.config.AppConfig
import com.example.core.utils.toJSONArray
import com.example.core.utils.toJSONObject
import com.example.core.utils.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LLMService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<Map<String, Any>>,
    ): LLMResponse = withContext(Dispatchers.IO) {
        val body = buildRequestJson(messages, systemPrompt, tools)

        val request = Request.Builder()
            .url("${AppConfig.CLAUDE_BASE_URL}messages")
            .addHeader("x-api-key", AppConfig.claudeApiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Claude API")
            if (!response.isSuccessful) {
                throw Exception("Claude API error ${response.code}: $responseBody")
            }
            parseLLMResponse(responseBody)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildRequestJson(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<Map<String, Any>>,
    ): String {
        val messagesList = messages.map { msg ->
            mapOf(
                "role" to msg.role,
                "content" to msg.content
            )
        }

        val requestMap: Map<String, Any> = mapOf(
            "model" to AppConfig.CLAUDE_MODEL,
            "max_tokens" to AppConfig.MAX_TOKENS,
            "system" to systemPrompt,
            "messages" to messagesList,
            "tools" to tools,
            "temperature" to AppConfig.TEMPERATURE,
        )

        return requestMap.toJSONObject().toString()
    }

    private fun parseLLMResponse(body: String): LLMResponse {
        val json = JSONObject(body)
        val contentArr = json.getJSONArray("content")
        var text: String? = null
        val toolCalls = mutableListOf<ToolCallRequest>()

        for (i in 0 until contentArr.length()) {
            val block = contentArr.getJSONObject(i)
            when (block.getString("type")) {
                "text" -> text = block.getString("text")
                "tool_use" -> {
                    val inputJson = block.getJSONObject("input")
                    val inputMap = inputJson.toMap()
                    toolCalls.add(
                        ToolCallRequest(
                            id = block.getString("id"),
                            name = block.getString("name"),
                            input = inputMap
                        )
                    )
                }
            }
        }

        return LLMResponse(
            text = text,
            toolCalls = toolCalls.ifEmpty { null },
            stopReason = json.optString("stop_reason", "end_turn")
        )
    }
}
