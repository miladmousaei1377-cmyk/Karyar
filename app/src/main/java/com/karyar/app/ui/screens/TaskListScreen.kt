package com.karyar.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.Task
import com.karyar.app.data.TaskCategory
import com.karyar.app.data.TaskPriority
import com.karyar.app.ui.theme.PriorityHigh
import com.karyar.app.ui.theme.PriorityLow
import com.karyar.app.ui.theme.PriorityMedium
import com.karyar.app.viewmodel.FilterState
import com.karyar.app.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaryarNavigationDrawer(
                activeCount = activeCount,
                onTasksClick = {
                    scope.launch { drawerState.close() }
                    viewModel.clearFilters()
                },
                onStatsClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToStats()
                },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
                onAboutClick = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            if (showSearch) {
                                TextField(
                                    value = searchText,
                                    onValueChange = { text ->
                                        searchText = text
                                        viewModel.setSearchQuery(text)
                                    },
                                    placeholder = { Text("جستجو...") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = "کاریار",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                showSearch = !showSearch
                                if (!showSearch) {
                                    searchText = ""
                                    viewModel.setSearchQuery("")
                                }
                            }) {
                                Icon(
                                    if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "جستجو"
                                )
                            }
                            IconButton(onClick = { viewModel.setDarkMode(!isDarkMode) }) {
                                Icon(
                                    if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "حالت تاریک"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterBar(
                        filterState = filterState,
                        onPrioritySelected = { viewModel.setPriority(it) },
                        onCategorySelected = { viewModel.setCategory(it) },
                        onShowCompleted = { viewModel.setShowCompleted(it) },
                        onClearFilters = { viewModel.clearFilters() }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "افزودن کار",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (tasks.isEmpty()) {
                    EmptyState(filterState = filterState)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleComplete(task) },
                                onEdit = { onEditTask(task.id) },
                                onDelete = {
                                    viewModel.deleteTask(task)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "کار حذف شد",
                                            actionLabel = "بازگردانی",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addTask(task)
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun KaryarNavigationDrawer(
    activeCount: Int,
    onTasksClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "کاریار",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "مدیریت کارهای روزانه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "$activeCount کار فعال",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        NavigationDrawerItem(
            label = { Text("کارها") },
            selected = true,
            onClick = onTasksClick,
            icon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label = { Text("آمار") },
            selected = false,
            onClick = onStatsClick,
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label = { Text("تنظیمات") },
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp))

        NavigationDrawerItem(
            label = { Text("درباره کاریار") },
            selected = false,
            onClick = onAboutClick,
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun FilterBar(
    filterState: FilterState,
    onPrioritySelected: (TaskPriority?) -> Unit,
    onCategorySelected: (TaskCategory?) -> Unit,
    onShowCompleted: (Boolean) -> Unit,
    onClearFilters: () -> Unit
) {
    val hasFilter = filterState.selectedPriority != null ||
            filterState.selectedCategory != null ||
            filterState.showCompleted

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = !hasFilter,
                    onClick = onClearFilters,
                    label = { Text("همه") }
                )
            }
            item {
                FilterChip(
                    selected = filterState.showCompleted,
                    onClick = { onShowCompleted(!filterState.showCompleted) },
                    label = { Text("انجام‌شده") },
                    leadingIcon = if (filterState.showCompleted) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            items(TaskPriority.values()) { priority ->
                val priorityColor = when (priority) {
                    TaskPriority.HIGH -> PriorityHigh
                    TaskPriority.MEDIUM -> PriorityMedium
                    TaskPriority.LOW -> PriorityLow
                }
                FilterChip(
                    selected = filterState.selectedPriority == priority,
                    onClick = {
                        onPrioritySelected(if (filterState.selectedPriority == priority) null else priority)
                    },
                    label = { Text(priority.label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                    }
                )
            }
            items(TaskCategory.values()) { category ->
                FilterChip(
                    selected = filterState.selectedCategory == category,
                    onClick = {
                        onCategorySelected(if (filterState.selectedCategory == category) null else category)
                    },
                    label = { Text("${category.icon} ${category.label}") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }
    ) {
        val priorityColor = when (TaskPriority.valueOf(task.priority)) {
            TaskPriority.HIGH -> PriorityHigh
            TaskPriority.MEDIUM -> PriorityMedium
            TaskPriority.LOW -> PriorityLow
        }
        val category = TaskCategory.valueOf(task.category)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(priorityColor)
                )
                Spacer(Modifier.width(12.dp))
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${category.icon} ${category.label}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        task.dueDate?.let { dueDate ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dueDate))
                            val isOverdue = dueDate < System.currentTimeMillis() && !task.isCompleted
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (task.reminderTime != null) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "یادآور",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(filterState: FilterState) {
    val hasFilter = filterState.selectedPriority != null ||
            filterState.selectedCategory != null ||
            filterState.showCompleted ||
            filterState.searchQuery.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasFilter) "🔍" else "✅",
            fontSize = 64.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasFilter) "نتیجه‌ای یافت نشد" else "هیچ کاری وجود ندارد",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "فیلتر یا جستجوی خود را تغییر دهید"
            else "روی + بزنید تا اولین کار خود را اضافه کنید",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
