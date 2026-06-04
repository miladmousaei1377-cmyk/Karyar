package com.example.tools

abstract class BaseTool {
    abstract val name: String
    abstract val description: String
    abstract val inputSchema: Map<String, Map<String, Any>>
    open val requiredFields: List<String> get() = emptyList()

    abstract suspend fun execute(input: Map<String, Any>): String

    fun toClaudeDefinition(): Map<String, Any> = mapOf(
        "name" to name,
        "description" to description,
        "input_schema" to mapOf(
            "type" to "object",
            "properties" to inputSchema,
            "required" to requiredFields
        )
    )
}
