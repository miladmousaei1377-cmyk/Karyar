package com.example.tools

import com.example.repository.TaskRepository

class ToolRegistry(taskRepository: TaskRepository) {
    private val tools: Map<String, BaseTool> = listOf(
        DateTimeTool(),
        CalculatorTool(),
        WebSearchTool(),
        WeatherTool(),
        TasksTool(taskRepository),
    ).associateBy { it.name }

    fun getToolDefinitions(): List<Map<String, Any>> =
        tools.values.map { it.toClaudeDefinition() }

    suspend fun execute(toolName: String, input: Map<String, Any>): String =
        tools[toolName]?.execute(input) ?: "ابزار \"$toolName\" یافت نشد."
}
