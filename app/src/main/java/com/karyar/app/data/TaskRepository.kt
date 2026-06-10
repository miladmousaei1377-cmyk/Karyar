package com.karyar.app.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks()
    fun getActiveTasks(): Flow<List<Task>> = dao.getActiveTasks()
    fun getCompletedTasks(): Flow<List<Task>> = dao.getCompletedTasks()
    fun searchTasks(query: String): Flow<List<Task>> = dao.searchTasks(query)
    fun getTasksByCategory(category: String): Flow<List<Task>> = dao.getTasksByCategory(category)
    fun getTasksByPriority(priority: String): Flow<List<Task>> = dao.getTasksByPriority(priority)
    fun getActiveTaskCount(): Flow<Int> = dao.getActiveTaskCount()
    fun getCompletedTaskCount(): Flow<Int> = dao.getCompletedTaskCount()
    fun getTotalTaskCount(): Flow<Int> = dao.getTotalTaskCount()
    suspend fun getTaskById(id: Long): Task? = dao.getTaskById(id)
    suspend fun getAllTasksList(): List<Task> = dao.getAllTasksList()
    fun getTasksWithReminders(): Flow<List<Task>> = dao.getTasksWithReminders()
    suspend fun insertTask(task: Task): Long = dao.insertTask(task)
    suspend fun updateTask(task: Task) = dao.updateTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
    suspend fun deleteAllCompleted() = dao.deleteAllCompleted()
    suspend fun deleteAllActive() = dao.deleteAllActive()
}
