package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // "شخصی", "کاری", "خرید", "آموزش"
    val priority: Int, // 1 = Low, 2 = Medium, 3 = High
    val isCompleted: Boolean = false,
    val reminderTime: Long? = null,
    val isReminderActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
