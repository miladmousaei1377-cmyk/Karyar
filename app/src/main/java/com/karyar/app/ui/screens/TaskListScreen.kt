package com.karyar.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.karyar.app.utils.JalaliCalendar
import com.karyar.app.viewmodel.FilterState
import com.karyar.app.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val activeTasks by viewModel.activeTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val reminderTasks by viewModel.reminderTasks.collectAsState()
    val searchResults by viewModel.tasks.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteAllActiveDialog by remember { mutableStateOf(false) }
    var deleteAllCompletedDialog by remember { mutableStateOf(false) }

    if (deleteAllActiveDialog) {
        AlertDialog(
            onDismissRequest = { deleteAllActiveDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("حذف همه کارهای فعال") },
            text = { Text("آیا مطمئن هستید؟ این عمل قابل بازگشت نیست.") },
            confirmButton = {
                Button(onClick = { activeTasks.forEach { viewModel.deleteTask(it) }; deleteAllActiveDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف همه") }
            },
            dismissButton = { TextButton(onClick = { deleteAllActiveDialog = false }) { Text("انصراف") } }
        )
    }

    if (deleteAllCompletedDialog) {
        AlertDialog(
            onDismissRequest = { deleteAllCompletedDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("حذف همه انجام‌شده‌ها") },
            text = { Text("آیا مطمئن هستید؟") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAllCompleted(); deleteAllCompletedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف همه") }
            },
            dismissButton = { TextButton(onClick = { deleteAllCompletedDialog = false }) { Text("انصراف") } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaryarNavigationDrawer(
                activeCount = activeCount,
                onTasksClick = { scope.launch { drawerState.close() }; viewModel.clearFilters(); showSearch = false; searchText = "" },
                onStatsClick = { scope.launch { drawerState.close() }; onNavigateToStats() },
                onSettingsClick = { scope.launch { drawerState.close() }; onNavigateToSettings() },
                onAboutClick = { scope.launch { drawerState.close() } }
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
                                    onValueChange = { searchText = it; viewModel.setSearchQuery(it) },
                                    placeholder = { Text("جستجو در کارها...", color = MaterialTheme.colorScheme.onPrimary.copy(0.7f)) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        cursorColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text("کاریار", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "منو", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                showSearch = !showSearch
                                if (!showSearch) { searchText = ""; viewModel.setSearchQuery("") }
                            }) {
                                Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, "جستجو", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            IconButton(onClick = { viewModel.setDarkMode(!isDarkMode) }) {
                                Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, "تم", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    if (!showSearch) {
                        FilterBar(filterState = filterState,
                            onPrioritySelected = { viewModel.setPriority(it) },
                            onCategorySelected = { viewModel.setCategory(it) },
                            onShowCompleted = { viewModel.setShowCompleted(it) },
                            onClearFilters = { viewModel.clearFilters() })
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddTask, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, "افزودن", tint = MaterialTheme.colorScheme.onPrimary)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            if (showSearch && searchText.isNotBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    item { Text("${searchResults.size} نتیجه یافت شد", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(searchResults, key = { it.id }) { task ->
                        HomeTaskCard(task = task,
                            onToggleComplete = { viewModel.toggleComplete(task) },
                            onEdit = { onEditTask(task.id) },
                            onDelete = {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val r = snackbarHostState.showSnackbar("کار حذف شد", "بازگردانی", duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) viewModel.addTask(task)
                                }
                            },
                            showEdit = !task.isCompleted)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    item {
                        TaskSection(
                            title = "کارهای انجام نشده",
                            icon = Icons.Default.RadioButtonUnchecked,
                            color = MaterialTheme.colorScheme.primary,
                            tasks = activeTasks,
                            showEdit = true,
                            onEdit = { onEditTask(it) },
                            onDelete = { task ->
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val r = snackbarHostState.showSnackbar("کار حذف شد", "بازگردانی", duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) viewModel.addTask(task)
                                }
                            },
                            onToggleComplete = { viewModel.toggleComplete(it) },
                            onDeleteAll = { deleteAllActiveDialog = true },
                            emptyText = "هیچ کار فعالی وجود ندارد"
                        )
                    }
                    item {
                        TaskSection(
                            title = "انجام شده‌ها",
                            icon = Icons.Default.CheckCircle,
                            color = PriorityLow,
                            tasks = completedTasks,
                            showEdit = false,
                            onEdit = {},
                            onDelete = { task ->
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val r = snackbarHostState.showSnackbar("کار حذف شد", "بازگردانی", duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) viewModel.addTask(task)
                                }
                            },
                            onToggleComplete = { viewModel.toggleComplete(it) },
                            onDeleteAll = { deleteAllCompletedDialog = true },
                            emptyText = "هنوز کاری انجام نشده"
                        )
                    }
                    item {
                        TaskSection(
                            title = "یادآوری‌ها",
                            icon = Icons.Default.NotificationsActive,
                            color = PriorityMedium,
                            tasks = reminderTasks,
                            showEdit = true,
                            onEdit = { onEditTask(it) },
                            onDelete = { task ->
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val r = snackbarHostState.showSnackbar("کار حذف شد", "بازگردانی", duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) viewModel.addTask(task)
                                }
                            },
                            onToggleComplete = { viewModel.toggleComplete(it) },
                            onDeleteAll = { reminderTasks.forEach { t -> viewModel.updateTask(t.copy(reminderTime = null)) } },
                            emptyText = "هیچ یادآوری‌ای تنظیم نشده"
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun TaskSection(
    title: String,
    icon: ImageVector,
    color: Color,
    tasks: List<Task>,
    showEdit: Boolean,
    onEdit: (Long) -> Unit,
    onDelete: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onDeleteAll: () -> Unit,
    emptyText: String
) {
    var expanded by remember { mutableStateOf(false) }
    val LIMIT = 3
    val displayed = if (expanded || tasks.size <= LIMIT) tasks else tasks.take(LIMIT)
    val hasMore = tasks.size > LIMIT

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("${tasks.size} مورد", style = MaterialTheme.typography.labelSmall, color = color)
                }
                if (tasks.isNotEmpty()) {
                    TextButton(onClick = onDeleteAll, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف همه", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            HorizontalDivider(color = color.copy(alpha = 0.15f))

            if (tasks.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓", fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayed.forEach { task ->
                        HomeTaskCard(task = task,
                            onToggleComplete = { onToggleComplete(task) },
                            onEdit = { onEdit(task.id) },
                            onDelete = { onDelete(task) },
                            showEdit = showEdit)
                    }
                }
                if (hasMore) {
                    HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (expanded) "نمایش کمتر" else "مشاهده همه (${tasks.size})", color = color, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(4.dp))
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showEdit: Boolean
) {
    val priorityColor = when (TaskPriority.valueOf(task.priority)) {
        TaskPriority.HIGH -> PriorityHigh
        TaskPriority.MEDIUM -> PriorityMedium
        TaskPriority.LOW -> PriorityLow
    }
    val category = TaskCategory.valueOf(task.category)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(3.dp).height(42.dp).clip(RoundedCornerShape(2.dp)).background(priorityColor))
            Spacer(Modifier.width(6.dp))
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(0.5f) else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${category.icon} ${category.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    task.dueDate?.let { date ->
                        val overdue = date < System.currentTimeMillis() && !task.isCompleted
                        Text("📅 ${JalaliCalendar.toShortDate(date)}", style = MaterialTheme.typography.labelSmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    task.reminderTime?.let {
                        Icon(Icons.Default.Notifications, null, Modifier.size(12.dp), tint = PriorityMedium)
                        Text(JalaliCalendar.toDateTimeString(it), style = MaterialTheme.typography.labelSmall, color = PriorityMedium)
                    }
                }
            }
            if (showEdit) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "ویرایش", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "حذف", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
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
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp)) {
            Column {
                Text("کاریار", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("مدیریت کارهای روزانه", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.onPrimary.copy(0.2f)) {
                    Text("$activeCount کار فعال", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(label = { Text("کارها") }, selected = true, onClick = onTasksClick, icon = { Icon(Icons.Default.CheckBox, null) }, modifier = Modifier.padding(horizontal = 12.dp))
        NavigationDrawerItem(label = { Text("آمار و گزارش") }, selected = false, onClick = onStatsClick, icon = { Icon(Icons.Default.BarChart, null) }, modifier = Modifier.padding(horizontal = 12.dp))
        NavigationDrawerItem(label = { Text("تنظیمات") }, selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, null) }, modifier = Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 12.dp))
        NavigationDrawerItem(label = { Text("درباره کاریار") }, selected = false, onClick = onAboutClick, icon = { Icon(Icons.Default.Info, null) }, modifier = Modifier.padding(horizontal = 12.dp))
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
    val hasFilter = filterState.selectedPriority != null || filterState.selectedCategory != null || filterState.showCompleted
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
    ) {
        item { FilterChip(selected = !hasFilter, onClick = onClearFilters, label = { Text("همه") }) }
        items(TaskPriority.values()) { p ->
            val c = when(p) { TaskPriority.HIGH -> PriorityHigh; TaskPriority.MEDIUM -> PriorityMedium; TaskPriority.LOW -> PriorityLow }
            FilterChip(selected = filterState.selectedPriority == p, onClick = { onPrioritySelected(if(filterState.selectedPriority==p) null else p) },
                label = { Text(p.label) },
                leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(c)) })
        }
        items(TaskCategory.values()) { cat ->
            FilterChip(selected = filterState.selectedCategory == cat, onClick = { onCategorySelected(if(filterState.selectedCategory==cat) null else cat) },
                label = { Text("${cat.icon} ${cat.label}") })
        }
    }
}

@Composable
fun EmptyState(filterState: FilterState, modifier: Modifier = Modifier) {
    val hasFilter = filterState.selectedPriority != null || filterState.selectedCategory != null || filterState.searchQuery.isNotBlank()
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (hasFilter) "🔍" else "✅", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(if (hasFilter) "نتیجه‌ای یافت نشد" else "هیچ کاری وجود ندارد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(if (hasFilter) "جستجو یا فیلتر را تغییر دهید" else "روی + بزنید تا اولین کار را ثبت کنید", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
