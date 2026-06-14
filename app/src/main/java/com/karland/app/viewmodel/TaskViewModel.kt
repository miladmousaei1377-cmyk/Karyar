package com.karland.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karland.app.data.*
import com.karland.app.notification.AlarmHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FilterState(
    val searchQuery: String = "",
    val selectedCategory: TaskCategory? = null,
    val selectedPriority: TaskPriority? = null,
    val showCompleted: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(db.taskDao())
    private val prefsRepo = PreferencesRepository(application)

    val isDarkMode: StateFlow<Boolean> = prefsRepo.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Unfiltered flows for home screen sections
    val activeTasks: StateFlow<List<Task>> = repository.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<Task>> = repository.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderTasks: StateFlow<List<Task>> = repository.getTasksWithReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tasks for search
    val tasks: StateFlow<List<Task>> = _filterState.flatMapLatest { filter ->
        when {
            filter.searchQuery.isNotBlank() -> repository.searchTasks(filter.searchQuery)
            filter.selectedCategory != null -> repository.getTasksByCategory(filter.selectedCategory.name)
            filter.selectedPriority != null -> repository.getTasksByPriority(filter.selectedPriority.name)
            filter.showCompleted -> repository.getCompletedTasks()
            else -> repository.getActiveTasks()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = repository.getActiveTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val completedCount: StateFlow<Int> = repository.getCompletedTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalCount: StateFlow<Int> = repository.getTotalTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val onboardingCompleted: StateFlow<Boolean> = prefsRepo.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Survives Activity recreation so splash screen never replays after first launch.
    var splashShown: Boolean = false
        private set

    fun markSplashShown() { splashShown = true }

    fun setOnboardingCompleted() = viewModelScope.launch { prefsRepo.setOnboardingCompleted() }

    fun importTasks(tasks: List<Task>) = viewModelScope.launch {
        tasks.forEach { repository.insertTask(it.copy(id = 0)) }
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { prefsRepo.setDarkMode(enabled) }
    fun setSearchQuery(q: String) { _filterState.update { it.copy(searchQuery = q) } }
    fun setCategory(c: TaskCategory?) { _filterState.update { it.copy(selectedCategory = c) } }
    fun setPriority(p: TaskPriority?) { _filterState.update { it.copy(selectedPriority = p) } }
    fun setShowCompleted(b: Boolean) { _filterState.update { it.copy(showCompleted = b) } }
    fun clearFilters() { _filterState.value = FilterState() }

    fun addTask(task: Task) = viewModelScope.launch {
        val id = repository.insertTask(task)
        task.reminderTime?.let { AlarmHelper.scheduleAlarm(getApplication(), id, task.title, it) }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        task.reminderTime?.let { AlarmHelper.scheduleAlarm(getApplication(), task.id, task.title, it) }
            ?: AlarmHelper.cancelAlarm(getApplication(), task.id)
    }

    fun toggleComplete(task: Task) = viewModelScope.launch {
        repository.updateTask(task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis()))
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        AlarmHelper.cancelAlarm(getApplication(), task.id)
        repository.deleteTask(task)
    }

    fun deleteAllCompleted() = viewModelScope.launch { repository.deleteAllCompleted() }
    fun deleteAllActive() = viewModelScope.launch { repository.deleteAllActive() }

    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)
    suspend fun getAllTasksForExport(): List<Task> = repository.getAllTasksList()
}
