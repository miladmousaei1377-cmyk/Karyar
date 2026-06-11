package com.karyar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.Task
import com.karyar.app.data.TaskCategory
import com.karyar.app.data.TaskPriority
import com.karyar.app.ui.theme.PriorityHigh
import com.karyar.app.ui.theme.PriorityLow
import com.karyar.app.ui.theme.PriorityMedium
import com.karyar.app.utils.JalaliCalendar
import com.karyar.app.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: Long?,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var existingTask by remember { mutableStateOf<Task?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.PERSONAL) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isEditMode = taskId != null

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getTaskById(taskId)?.let {
                existingTask = it
                title = it.title
                description = it.description
                selectedCategory = TaskCategory.valueOf(it.category)
                selectedPriority = TaskPriority.valueOf(it.priority)
                dueDate = it.dueDate
                reminderTime = it.reminderTime
            }
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { ts -> dueDate = ts; showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { if (reminderTime != null) timeInMillis = reminderTime!! }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("زمان یادآوری") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        if (dueDate != null) timeInMillis = dueDate!!
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    reminderTime = c.timeInMillis
                    showTimePicker = false
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "ویرایش کار" else "افزودن کار", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text("عنوان کار *") },
                isError = titleError,
                supportingText = if (titleError) {{ Text("عنوان الزامی است") }} else null,
                leadingIcon = { Icon(Icons.Default.Title, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات") },
                leadingIcon = { Icon(Icons.Default.Description, null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Category
            Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskCategory.values().take(3).forEach { cat ->
                    CategoryChipItem(cat, selectedCategory == cat, { selectedCategory = cat }, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskCategory.values().drop(3).forEach { cat ->
                    CategoryChipItem(cat, selectedCategory == cat, { selectedCategory = cat }, Modifier.weight(1f))
                }
            }

            // Priority
            Text("اولویت", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.values().forEach { p ->
                    val c = when(p) { TaskPriority.HIGH->PriorityHigh; TaskPriority.MEDIUM->PriorityMedium; TaskPriority.LOW->PriorityLow }
                    PriorityOption(p, c, selectedPriority == p, { selectedPriority = p }, Modifier.weight(1f))
                }
            }

            // Due date
            Text("تاریخ سررسید", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedCard(Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = dueDate?.let { JalaliCalendar.toLongDate(it) } ?: "انتخاب تاریخ",
                        color = if (dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (dueDate != null) {
                        IconButton(onClick = { dueDate = null }, Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, null, Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Reminder
            Text("یادآوری", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            OutlinedCard(Modifier.fillMaxWidth().clickable { showTimePicker = true }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = reminderTime?.let { JalaliCalendar.toDateTimeString(it) } ?: "تنظیم یادآوری",
                        color = if (reminderTime != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (reminderTime != null) {
                        IconButton(onClick = { reminderTime = null }, Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, null, Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (title.isBlank()) { titleError = true; return@Button }
                    scope.launch {
                        if (isEditMode && existingTask != null) {
                            viewModel.updateTask(existingTask!!.copy(title = title.trim(), description = description.trim(),
                                category = selectedCategory.name, priority = selectedPriority.name,
                                dueDate = dueDate, reminderTime = reminderTime))
                        } else {
                            viewModel.addTask(Task(title = title.trim(), description = description.trim(),
                                category = selectedCategory.name, priority = selectedPriority.name,
                                dueDate = dueDate, reminderTime = reminderTime))
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isEditMode) Icons.Default.Save else Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditMode) "ذخیره تغییرات" else "افزودن کار", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryChipItem(category: TaskCategory, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() }.clip(RoundedCornerShape(10.dp))
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(category.icon, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(category.label, style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PriorityOption(priority: TaskPriority, color: Color, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() }.clip(RoundedCornerShape(10.dp))
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        color = if (isSelected) color.copy(0.15f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(priority.label, style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
