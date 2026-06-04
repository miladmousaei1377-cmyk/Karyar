package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userMessage: String,
    val agentResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
