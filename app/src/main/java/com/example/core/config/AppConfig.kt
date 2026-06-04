package com.example.core.config

import com.example.BuildConfig

object AppConfig {
    // Secrets plugin injects these from .env into BuildConfig at build time.
    // Fields will be empty strings if the .env key is missing/blank.
    val claudeApiKey: String get() = runCatching { BuildConfig.CLAUDE_API_KEY }.getOrDefault("")
    val openAiApiKey: String get() = runCatching { BuildConfig.OPENAI_API_KEY }.getOrDefault("")
    val elevenLabsApiKey: String get() = runCatching { BuildConfig.ELEVENLABS_API_KEY }.getOrDefault("")
    val tavilyApiKey: String get() = runCatching { BuildConfig.TAVILY_API_KEY }.getOrDefault("")
    val openWeatherApiKey: String get() = runCatching { BuildConfig.OPENWEATHER_API_KEY }.getOrDefault("")

    const val CLAUDE_MODEL = "claude-sonnet-4-6"
    const val CLAUDE_BASE_URL = "https://api.anthropic.com/v1/"
    const val MAX_TOKENS = 4096
    const val TEMPERATURE = 0.7

    const val WHISPER_MODEL = "whisper-1"
    const val WHISPER_LANGUAGE = "fa"

    const val ELEVENLABS_VOICE_ID = "pNInz6obpgDQGcFmaJgB"
    const val ELEVENLABS_MODEL = "eleven_multilingual_v2"

    const val MAX_TOOL_ITERATIONS = 10
    const val MAX_CONTEXT_MESSAGES = 20
}
