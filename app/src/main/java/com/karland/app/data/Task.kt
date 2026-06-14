package com.karland.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = TaskCategory.PERSONAL.name,
    val priority: String = TaskPriority.MEDIUM.name,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TaskCategory(val label: String, val icon: String) {
    WORK("کاری", "💼"),
    PERSONAL("شخصی", "👤"),
    SHOPPING("خرید", "🛒"),
    HEALTH("سلامت", "❤️"),
    EDUCATION("آموزش", "📚"),
    OTHER("سایر", "📌")
}

enum class TaskPriority(val label: String, val color: Long) {
    HIGH("مهم", 0xFFE53935),
    MEDIUM("متوسط", 0xFFFB8C00),
    LOW("کم", 0xFF43A047)
}
