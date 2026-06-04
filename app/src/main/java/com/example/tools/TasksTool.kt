package com.example.tools

import com.example.database.TaskEntity
import com.example.repository.TaskRepository

class TasksTool(private val repository: TaskRepository) : BaseTool() {
    override val name = "manage_tasks"
    override val description =
        "مدیریت وظایف: مشاهده لیست، افزودن وظیفه جدید، یا تکمیل وظیفه موجود"
    override val inputSchema: Map<String, Map<String, Any>> = mapOf(
        "action" to mapOf(
            "type" to "string",
            "description" to "عملیات: \"list\" (نمایش وظایف)، \"add\" (افزودن)، \"complete\" (تکمیل)"
        ),
        "title" to mapOf("type" to "string", "description" to "عنوان وظیفه"),
        "description" to mapOf("type" to "string", "description" to "توضیحات وظیفه"),
        "priority" to mapOf(
            "type" to "integer",
            "description" to "اولویت: 1=کم، 2=متوسط، 3=زیاد"
        ),
        "category" to mapOf(
            "type" to "string",
            "description" to "دسته‌بندی: شخصی، کاری، خرید، آموزش"
        ),
        "task_id" to mapOf("type" to "integer", "description" to "شناسه وظیفه برای تکمیل")
    )
    override val requiredFields = listOf("action")

    override suspend fun execute(input: Map<String, Any>): String {
        return when (input["action"] as? String) {
            "list" -> listTasks()
            "add" -> addTask(input)
            "complete" -> completeTask(input)
            else -> "عملیات نامعتبر. از: list، add، complete استفاده کن"
        }
    }

    private suspend fun listTasks(): String {
        val tasks = repository.getAllTasksOnce()
        if (tasks.isEmpty()) return "هیچ وظیفه‌ای وجود ندارد."
        val active = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }
        val sb = StringBuilder()
        if (active.isNotEmpty()) {
            sb.appendLine("وظایف فعال (${active.size}):")
            active.take(10).forEach { sb.appendLine("  [${it.id}] ${it.title} (اولویت: ${priorityLabel(it.priority)})") }
        }
        if (completed.isNotEmpty()) {
            sb.appendLine("\nتکمیل‌شده (${completed.size} تا)")
        }
        return sb.toString().trim()
    }

    private suspend fun addTask(input: Map<String, Any>): String {
        val title = input["title"] as? String ?: return "عنوان وظیفه الزامی است."
        val desc = input["description"] as? String ?: ""
        val priority = (input["priority"] as? Number)?.toInt()?.coerceIn(1, 3) ?: 2
        val category = input["category"] as? String ?: "شخصی"
        val task = TaskEntity(
            title = title,
            description = desc,
            category = category,
            priority = priority
        )
        repository.insertTask(task)
        return "وظیفه \"$title\" با اولویت ${priorityLabel(priority)} اضافه شد."
    }

    private suspend fun completeTask(input: Map<String, Any>): String {
        val id = (input["task_id"] as? Number)?.toInt() ?: return "شناسه وظیفه نامعتبر است."
        val task = repository.getTaskById(id) ?: return "وظیفه با شناسه $id یافت نشد."
        repository.updateCompletionStatus(id, true)
        return "وظیفه \"${task.title}\" تکمیل شد."
    }

    private fun priorityLabel(p: Int) = when (p) { 3 -> "زیاد"; 2 -> "متوسط"; else -> "کم" }
}
