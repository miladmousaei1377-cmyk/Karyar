package com.example.agent.domain.models

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false
)

enum class MessageRole { USER, AGENT }

data class ApiMessage(
    val role: String,
    val content: Any
)

data class ToolCallRequest(
    val id: String,
    val name: String,
    val input: Map<String, Any>
)

data class LLMResponse(
    val text: String?,
    val toolCalls: List<ToolCallRequest>?,
    val stopReason: String
) {
    val hasToolCalls: Boolean get() = !toolCalls.isNullOrEmpty()
}
