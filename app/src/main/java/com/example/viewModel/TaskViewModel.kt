package com.example.viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.TaskEntity
import com.example.receiver.AlarmHelper
import com.example.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val sharedPrefs = application.getSharedPreferences("karha_settings", Context.MODE_PRIVATE)

    // Dark Mode preference flow
    val isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode", false))

    // Task streams
    val allTasks: StateFlow<List<TaskEntity>>
    val activeTasks: StateFlow<List<TaskEntity>>
    val completedTasks: StateFlow<List<TaskEntity>>
    val activeReminders: StateFlow<List<TaskEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao)

        allTasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeTasks = repository.activeTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        completedTasks = repository.completedTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeReminders = repository.activeReminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun toggleDarkMode() {
        val newValue = !isDarkMode.value
        isDarkMode.value = newValue
        sharedPrefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    fun addTask(
        title: String,
        description: String,
        category: String,
        priority: Int,
        reminderTime: Long?
    ) {
        viewModelScope.launch {
            val hasFutureReminder = reminderTime != null && reminderTime > System.currentTimeMillis()
            val newTask = TaskEntity(
                title = title,
                description = description,
                category = category,
                priority = priority,
                reminderTime = reminderTime,
                isReminderActive = hasFutureReminder,
                isCompleted = false
            )

            val generatedId = repository.insertTask(newTask)

            if (hasFutureReminder) {
                // Copy with actual generated ID from database
                val taskWithId = newTask.copy(id = generatedId.toInt())
                AlarmHelper.scheduleAlarm(getApplication(), taskWithId)
            }
        }
    }

    fun updateTask(task: TaskEntity, newReminderTime: Long? = task.reminderTime) {
        viewModelScope.launch {
            // Cancel old alarm if any
            AlarmHelper.cancelAlarm(getApplication(), task.id)

            val hasFutureReminder = newReminderTime != null && newReminderTime > System.currentTimeMillis()
            val updatedTask = task.copy(
                reminderTime = newReminderTime,
                isReminderActive = hasFutureReminder
            )

            repository.updateTask(updatedTask)

            if (hasFutureReminder) {
                AlarmHelper.scheduleAlarm(getApplication(), updatedTask)
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val newCompletion = !task.isCompleted
            repository.updateCompletionStatus(task.id, newCompletion)

            if (newCompletion) {
                // If completed, cancel any pending alarms
                AlarmHelper.cancelAlarm(getApplication(), task.id)
                repository.updateReminderStatus(task.id, false)
            } else {
                // If uncompleted and has reminder in future, activate it again
                val reminder = task.reminderTime
                if (reminder != null && reminder > System.currentTimeMillis()) {
                    repository.updateReminderStatus(task.id, true)
                    AlarmHelper.scheduleAlarm(getApplication(), task.copy(isCompleted = false, isReminderActive = true))
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            AlarmHelper.cancelAlarm(getApplication(), task.id)
            repository.deleteTask(task)
        }
    }

    fun completeMultipleTasks(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            tasks.forEach { task ->
                if (!task.isCompleted) {
                    repository.updateCompletionStatus(task.id, true)
                    AlarmHelper.cancelAlarm(getApplication(), task.id)
                    repository.updateReminderStatus(task.id, false)
                }
            }
        }
    }

    fun deleteMultipleTasks(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            tasks.forEach { task ->
                AlarmHelper.cancelAlarm(getApplication(), task.id)
                repository.deleteTask(task)
            }
        }
    }

    fun cancelReminder(task: TaskEntity) {
        viewModelScope.launch {
            AlarmHelper.cancelAlarm(getApplication(), task.id)
            repository.updateTask(task.copy(reminderTime = null, isReminderActive = false))
        }
    }
}
