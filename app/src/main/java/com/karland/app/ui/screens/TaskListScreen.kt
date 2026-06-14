package com.karland.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
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
import com.karland.app.data.Task
import com.karland.app.data.TaskCategory
import com.karland.app.data.TaskPriority
import com.karland.app.ui.theme.PriorityHigh
import com.karland.app.ui.theme.PriorityLow
import com.karland.app.ui.theme.PriorityMedium
import com.karland.app.utils.JalaliCalendar
import com.karland.app.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val activeTasks by viewModel.activeTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val reminderTasks by viewModel.reminderTasks.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TaskCategory?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    var backPressedOnce by remember { mutableStateOf(false) }
    BackHandler(enabled = drawerState.isClosed) {
        if (backPressedOnce) {
            android.os.Process.killProcess(android.os.Process.myPid())
        } else {
            backPressedOnce = true
            scope.launch {
                snackbarHostState.showSnackbar("برای خروج دوباره فشار دهید")
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    // Clear selection when switching tabs
    LaunchedEffect(selectedTab) {
        selectedIds = emptySet()
        searchQuery = ""
        selectedCategory = null
    }

    val todayCompleted = completedCount
    val todayTotal = totalCount
    val progress = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal.toFloat() else 0f

    val currentList: List<Task> = remember(selectedTab, activeTasks, completedTasks, reminderTasks, searchQuery, selectedCategory) {
        val base = when (selectedTab) {
            0 -> activeTasks
            1 -> completedTasks
            else -> reminderTasks
        }
        base.filter { task ->
            (searchQuery.isBlank() || task.title.contains(searchQuery, true) || task.description.contains(searchQuery, true))
            && (selectedCategory == null || task.category == selectedCategory!!.name)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaryarNavigationDrawer(
                activeCount = activeCount,
                onTasksClick = { scope.launch { drawerState.close() } },
                onStatsClick = { scope.launch { drawerState.close() }; onNavigateToStats() },
                onSettingsClick = { scope.launch { drawerState.close() }; onNavigateToSettings() },
                onAboutClick = { scope.launch { drawerState.close() }; onNavigateToAbout() }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Top row: menu (right in RTL) + dark mode (left in RTL)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu button (right in RTL = first in Row)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "منو", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    // Dark mode toggle (left in RTL = second in Row)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(onClick = { viewModel.setDarkMode(!isDarkMode) }) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تم",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Progress card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پیشرفت امروز شما",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (todayTotal == 0) "هنوز کاری ثبت نکرده‌اید"
                                       else "$todayCompleted از $todayTotal کار انجام شده",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tab row
                val tabs = listOf(
                    Triple("کارهای فعال", Icons.Default.List, activeTasks.size),
                    Triple("انجام شده‌ها", Icons.Default.Check, completedTasks.size),
                    Triple("یادآوری‌ها", Icons.Default.Notifications, reminderTasks.size)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    tabs.forEachIndexed { index, (label, icon, count) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Column(
                                Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.height(2.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در بین کارها...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = if (searchQuery.isNotBlank()) {{ IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } }} else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Category filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("همه") },
                            leadingIcon = if (selectedCategory == null) {{ Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }} else null
                        )
                    }
                    items(TaskCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text("${cat.icon} ${cat.label}") }
                        )
                    }
                }

                // Delete all active button row (tab 0 only, not in selection mode)
                if (selectedTab == 0 && currentList.isNotEmpty() && !isSelectionMode) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("حذف همه کارها", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }

                if (showDeleteAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAllDialog = false },
                        title = { Text("حذف همه کارها", fontWeight = FontWeight.Bold) },
                        text = { Text("آیا مطمئن هستید که می‌خواهید همه کارهای فعال را حذف کنید؟ این عملیات قابل بازگشت نیست.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteAllActive()
                                    showDeleteAllDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("بله، حذف کن")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAllDialog = false }) {
                                Text("انصراف")
                            }
                        }
                    )
                }

                // Task list
                Box(modifier = Modifier.weight(1f)) {
                    if (currentList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(0.5f), modifier = Modifier.size(64.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "نتیجه‌ای یافت نشد"
                                       else when (selectedTab) { 0 -> "کار جدیدی برای خودت بنویس!"; 1 -> "هنوز کاری انجام نشده"; else -> "هیچ یادآوری‌ای تنظیم نشده" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentList, key = { it.id }) { task ->
                                HomeTaskCard(
                                    task = task,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = task.id in selectedIds,
                                    onToggleComplete = { viewModel.toggleComplete(task) },
                                    onLongPress = {
                                        selectedIds = selectedIds + task.id
                                    },
                                    onSelect = {
                                        selectedIds = if (task.id in selectedIds)
                                            selectedIds - task.id
                                        else
                                            selectedIds + task.id
                                    },
                                    onEdit = { onEditTask(task.id) },
                                    onDelete = {
                                        viewModel.deleteTask(task)
                                        scope.launch {
                                            val r = snackbarHostState.showSnackbar("کار حذف شد", "بازگردانی", duration = SnackbarDuration.Short)
                                            if (r == SnackbarResult.ActionPerformed) viewModel.addTask(task)
                                        }
                                    },
                                    showEdit = selectedTab != 1,
                                    checkboxEntersSelection = selectedTab == 0,
                                    showCheckbox = selectedTab != 2
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }

                // Bottom: selection action bar or add button
                AnimatedContent(targetState = isSelectionMode, label = "bottom_bar") { inSelection ->
                    if (inSelection) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${selectedIds.size} مورد انتخاب شده",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { selectedIds = emptySet() }) {
                                        Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            currentList.filter { it.id in selectedIds && !it.isCompleted }
                                                .forEach { viewModel.toggleComplete(it) }
                                            selectedIds = emptySet()
                                            selectedTab = 1
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("انجام شد")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            currentList.filter { it.id in selectedIds }
                                                .forEach { viewModel.deleteTask(it) }
                                            selectedIds = emptySet()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("حذف")
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddTask,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("افزودن کار جدید", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTaskCard(
    task: Task,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleComplete: () -> Unit,
    onLongPress: () -> Unit = {},
    onSelect: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showEdit: Boolean,
    checkboxEntersSelection: Boolean = false,
    showCheckbox: Boolean = true
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
            .combinedClickable(
                onClick = { if (isSelectionMode) onSelect() },
                onLongClick = { if (!isSelectionMode) onLongPress() }
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(4.dp).height(52.dp).clip(RoundedCornerShape(2.dp)).background(priorityColor))
            Spacer(Modifier.width(8.dp))
            if (showCheckbox) {
                Checkbox(
                    checked = if (isSelectionMode) isSelected else if (checkboxEntersSelection) false else task.isCompleted,
                    onCheckedChange = {
                        when {
                            isSelectionMode -> onSelect()
                            checkboxEntersSelection -> onLongPress()
                            else -> onToggleComplete()
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (isSelectionMode) MaterialTheme.colorScheme.secondary
                                       else MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted && !isSelectionMode) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted && !isSelectionMode)
                        MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(0.6f)) {
                        Text(
                            "${category.icon} ${category.label}",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    task.dueDate?.let { date ->
                        val overdue = date < System.currentTimeMillis() && !task.isCompleted
                        Text(
                            "📅 ${JalaliCalendar.toShortDate(date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    task.reminderTime?.let {
                        Icon(Icons.Default.Notifications, null, Modifier.size(12.dp), tint = PriorityMedium)
                    }
                }
                // Creation date in Jalali
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "ثبت: ${JalaliCalendar.toShortDate(task.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                )
            }
            if (!isSelectionMode) {
                if (showEdit) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, "ویرایش", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, "حذف", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
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
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp)) {
            Column {
                Text("کارلند", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
        NavigationDrawerItem(label = { Text("درباره کارلند") }, selected = false, onClick = onAboutClick, icon = { Icon(Icons.Default.Info, null) }, modifier = Modifier.padding(horizontal = 12.dp))

        Spacer(Modifier.weight(1f))
        HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("خروج از برنامه", color = MaterialTheme.colorScheme.error) },
            selected = false,
            onClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
            icon = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun FilterBar(
    filterState: com.karland.app.viewmodel.FilterState,
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
