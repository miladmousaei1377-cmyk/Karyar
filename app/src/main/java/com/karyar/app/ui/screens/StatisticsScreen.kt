package com.karyar.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.Task
import com.karyar.app.data.TaskCategory
import com.karyar.app.utils.ExportHelper
import com.karyar.app.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val activeTasks by viewModel.activeTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var importResultMsg by remember { mutableStateOf("") }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val tasks = ExportHelper.importCsv(context, it)
                if (tasks.isNotEmpty()) {
                    viewModel.importTasks(tasks)
                    importResultMsg = "${tasks.size} کار با موفقیت وارد شد"
                } else {
                    importResultMsg = "فایل خالی یا نامعتبر است"
                }
            }
        }
    }

    val allTasks = remember(activeTasks, completedTasks) { activeTasks + completedTasks }

    val completionRate = if (totalCount > 0) {
        (completedCount.toFloat() / totalCount.toFloat()) * 100f
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("آمار و گزارش", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Cards
            Text(
                text = "نمای کلی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Assignment,
                    label = "کل کارها",
                    value = totalCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.RadioButtonUnchecked,
                    label = "فعال",
                    value = activeCount.toString(),
                    color = PriorityMedium
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    label = "انجام‌شده",
                    value = completedCount.toString(),
                    color = PriorityLow
                )
            }

            // Completion Rate
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نرخ تکمیل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${completionRate.toInt()}٪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { completionRate / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$completedCount از $totalCount کار انجام شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Breakdown
            Text(
                text = "بر اساس دسته‌بندی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TaskCategory.values().forEach { category ->
                        val categoryTasks = allTasks.filter { it.category == category.name }
                        val categoryTotal = categoryTasks.size
                        val categoryCompleted = categoryTasks.count { it.isCompleted }
                        if (categoryTotal > 0) {
                            CategoryStatRow(
                                category = category,
                                total = categoryTotal,
                                completed = categoryCompleted
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (allTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هنوز کاری ثبت نشده",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Import section
            Text("دریافت اطلاعات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "وارد کردن کارها از فایل CSV:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "می‌توانید فایل CSV صادر شده از کاریار را وارد کنید تا کارها به برنامه اضافه شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("انتخاب فایل CSV")
                    }
                    if (importResultMsg.isNotBlank()) {
                        Text(
                            text = importResultMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (importResultMsg.contains("موفق")) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Export section
            Text("خروجی گزارش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("دانلود گزارش کارها:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Text export button
                        Button(modifier = Modifier.weight(1f), onClick = {
                            scope.launch {
                                val tasks = viewModel.getAllTasksForExport()
                                val uri = ExportHelper.exportText(context, tasks)
                                uri?.let { ExportHelper.openFile(context, it, "text/plain") }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Icon(Icons.Default.Description, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("متن", fontSize = 13.sp)
                        }
                        // CSV/Excel export button
                        Button(modifier = Modifier.weight(1f), onClick = {
                            scope.launch {
                                val tasks = viewModel.getAllTasksForExport()
                                val uri = ExportHelper.exportCsv(context, tasks)
                                uri?.let { ExportHelper.openFile(context, it, "text/csv") }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))) {
                            Icon(Icons.Default.TableChart, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("اکسل", fontSize = 13.sp)
                        }
                        // PDF export button
                        Button(modifier = Modifier.weight(1f), onClick = {
                            scope.launch {
                                val tasks = viewModel.getAllTasksForExport()
                                val uri = ExportHelper.exportPdf(context, tasks)
                                uri?.let { ExportHelper.openFile(context, it, "application/pdf") }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Default.PictureAsPdf, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryStatRow(
    category: TaskCategory,
    total: Int,
    completed: Int
) {
    val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category.icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "$completed/$total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

