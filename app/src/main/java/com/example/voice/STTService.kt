package com.example.voice

import com.example.core.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class STTService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(audioFile: File, language: String = AppConfig.WHISPER_LANGUAGE): String? =
        withContext(Dispatchers.IO) {
            val apiKey = AppConfig.openAiApiKey
            if (apiKey.isBlank()) return@withContext null

            runCatching {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", audioFile.name,
                        audioFile.asRequestBody("audio/mp4".toMediaType())
                    )
                    .addFormDataPart("model", AppConfig.WHISPER_MODEL)
                    .addFormDataPart("language", language)
                    .addFormDataPart("response_format", "json")
                    .build()

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val text = JSONObject(response.body?.string() ?: return@runCatching null)
                        .optString("text")
                    text.ifBlank { null }
                }
            }.getOrNull()?.also { audioFile.delete() }
        }
}
