package com.example.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT id FROM conversations ORDER BY timestamp DESC LIMIT 100)")
    suspend fun pruneOld()

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM conversations")
    fun countAll(): Flow<Int>
}
