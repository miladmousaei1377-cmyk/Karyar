package com.example.agent.data

import com.example.agent.domain.models.ApiMessage
import com.example.core.config.AppConfig
import com.example.core.config.AppConfig.MAX_TOOL_ITERATIONS
import com.example.core.constants.AgentPrompts
import com.example.memory.MemoryService
import com.example.tools.ToolRegistry

class AgentOrchestrator(
    private val llm: LLMService,
    private val tools: ToolRegistry,
    private val memory: MemoryService,
) {
    var onStatusUpdate: ((String) -> Unit)? = null

    suspend fun run(
        userInput: String,
        conversationHistory: List<ApiMessage>,
        isVoice: Boolean = false,
    ): String {
        val memCtx = memory.getMemoryContext()
        val systemPrompt = buildSystemPrompt(memCtx, isVoice)

        val messages = mutableListOf<ApiMessage>()
        messages.addAll(conversationHistory.takeLast(AppConfig.MAX_CONTEXT_MESSAGES))
        messages.add(ApiMessage("user", userInput))

        var iteration = 0

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++
            onStatusUpdate?.invoke("در حال فکر کردن...")

            val response = llm.sendMessage(
                messages = messages,
                systemPrompt = systemPrompt,
                tools = tools.getToolDefinitions(),
            )

            if (!response.hasToolCalls) {
                val finalText = response.text ?: ""
                memory.saveConversation(userInput, finalText)
                return finalText
            }

            // Add assistant message with tool use blocks
            val assistantContent = mutableListOf<Map<String, Any>>()
            response.text?.let { assistantContent.add(mapOf("type" to "text", "text" to it)) }
            response.toolCalls!!.forEach { tc ->
                assistantContent.add(
                    mapOf("type" to "tool_use", "id" to tc.id, "name" to tc.name, "input" to tc.input)
                )
            }
            messages.add(ApiMessage("assistant", assistantContent))

            // Execute tools and collect results
            val toolResults = mutableListOf<Map<String, Any>>()
            for (tc in response.toolCalls) {
                onStatusUpdate?.invoke("در حال اجرای ${tc.name}...")
                val result = runCatching { tools.execute(tc.name, tc.input) }
                    .getOrElse { "خطا: ${it.message}" }
                toolResults.add(
                    mapOf("type" to "tool_result", "tool_use_id" to tc.id, "content" to result)
                )
            }
            messages.add(ApiMessage("user", toolResults))
        }

        // Fallback if max iterations hit
        val fallback = "متأسفم، پردازش طولانی شد. لطفاً دوباره سعی کنید."
        memory.saveConversation(userInput, fallback)
        return fallback
    }

    private fun buildSystemPrompt(memCtx: String, isVoice: Boolean): String {
        var prompt = AgentPrompts.SYSTEM_PROMPT
        if (memCtx.isNotBlank()) prompt += "\n\n## اطلاعات ذخیره‌شده کاربر\n$memCtx"
        if (isVoice) prompt += AgentPrompts.VOICE_INSTRUCTION
        return prompt
    }
}
