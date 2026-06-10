package com.karyar.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karyar.app.data.*
import com.karyar.app.notification.AlarmHelper
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

    val tasks: StateFlow<List<Task>> = _filterState.flatMapLatest { filter ->
        when {
            filter.searchQuery.isNotBlank() -> repository.searchTasks(filter.searchQuery)
            filter.selectedCategory != null -> repository.getTasksByCategory(filter.selectedCategory.name)
            filter.selectedPriority != null -> repository.getTasksByPriority(filter.selectedPriority.name)
            filter.showCompleted -> repository.getCompletedTasks()
            else -> repository.getActiveTasks()
        }
    }.map { list ->
        val filter = _filterState.value
        if (filter.selectedCategory != null && filter.searchQuery.isNotBlank()) {
            list.filter { it.category == filter.selectedCategory.name }
        } else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = repository.getActiveTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val completedCount: StateFlow<Int> = repository.getCompletedTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalCount: StateFlow<Int> = repository.getTotalTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setDarkMode(enabled)
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun setCategory(category: TaskCategory?) {
        _filterState.update { it.copy(selectedCategory = category) }
    }

    fun setPriority(priority: TaskPriority?) {
        _filterState.update { it.copy(selectedPriority = priority) }
    }

    fun setShowCompleted(show: Boolean) {
        _filterState.update { it.copy(showCompleted = show) }
    }

    fun clearFilters() {
        _filterState.value = FilterState()
    }

    fun addTask(task: Task) = viewModelScope.launch {
        val id = repository.insertTask(task)
        task.reminderTime?.let { time ->
            AlarmHelper.scheduleAlarm(getApplication(), id, task.title, time)
        }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        task.reminderTime?.let { time ->
            AlarmHelper.scheduleAlarm(getApplication(), task.id, task.title, time)
        } ?: AlarmHelper.cancelAlarm(getApplication(), task.id)
    }

    fun toggleComplete(task: Task) = viewModelScope.launch {
        repository.updateTask(task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis()))
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        AlarmHelper.cancelAlarm(getApplication(), task.id)
        repository.deleteTask(task)
    }

    fun deleteAllCompleted() = viewModelScope.launch {
        repository.deleteAllCompleted()
    }

    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)
}
