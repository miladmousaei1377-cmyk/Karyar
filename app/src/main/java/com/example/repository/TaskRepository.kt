package com.example.repository

import com.example.database.TaskDao
import com.example.database.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val activeTasks: Flow<List<TaskEntity>> = taskDao.getActiveTasks()
    val completedTasks: Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    val activeReminders: Flow<List<TaskEntity>> = taskDao.getActiveReminders()

    suspend fun getAllTasksOnce(): List<TaskEntity> = taskDao.getAllTasksOnce()

    suspend fun getTaskById(id: Int): TaskEntity? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun updateCompletionStatus(id: Int, isCompleted: Boolean) {
        taskDao.updateCompletionStatus(id, isCompleted)
    }

    suspend fun updateReminderStatus(id: Int, isActive: Boolean) {
        taskDao.updateReminderStatus(id, isActive)
    }
}
