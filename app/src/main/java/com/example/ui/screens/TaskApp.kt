package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import kotlinx.coroutines.launch
import android.widget.Toast
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.TaskEntity
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.viewModel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    
    // Splash screen state
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        showSplash = false
    }

    if (showSplash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Main Rounded Karyar leaf logo
                Image(
                    painter = painterResource(id = R.drawable.img_karyar_logo),
                    contentDescription = "کاریار",
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(32.dp))
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "کاریار",
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "کار‌یار، مدیریت ساده و هوشمندانه کارهای روزانه",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(56.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    } else {
        // UI state
        val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
        val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()
        val activeReminders by viewModel.activeReminders.collectAsStateWithLifecycle()

        var selectedTab by remember { mutableStateOf(0) } // 0 = Active tasks, 1 = Completed tasks, 2 = Reminders
        var selectedCategoryFilter by remember { mutableStateOf("همه") }
        var showAddDialog by remember { mutableStateOf(false) }
        var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

        // Multi-selection states
        var selectedTaskIds by remember { mutableStateOf(setOf<Int>()) }
        var searchQuery by remember { mutableStateOf("") }

        // Clear selection on tab, category filter or searchQuery change for safety
        LaunchedEffect(selectedTab, selectedCategoryFilter, searchQuery) {
            selectedTaskIds = emptySet()
        }

        // Search and category filter helper for tasks
        fun filterTasks(tasks: List<TaskEntity>, category: String, query: String): List<TaskEntity> {
            val categoryFiltered = if (category == "همه") tasks else tasks.filter { it.category == category }
            if (query.isBlank()) return categoryFiltered
            
            val normalizedQuery = query.trim().lowercase()
            return categoryFiltered.filter { task ->
                val titleMatch = task.title.lowercase().contains(normalizedQuery)
                val descriptionMatch = task.description.lowercase().contains(normalizedQuery)
                
                // Date match in reminderTime or createdAt
                val reminderTimeMatch = if (task.reminderTime != null) {
                    val formatted = formatTimestampToPersian(task.reminderTime).lowercase()
                    formatted.contains(normalizedQuery)
                } else false
                
                val createdAtMatch = run {
                    val calendar = Calendar.getInstance().apply { timeInMillis = task.createdAt }
                    val (jy, jm, jd) = JalaliCalendarHelper.gregorianToJalali(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    val monthName = JalaliCalendarHelper.persianMonthNames.getOrNull(jm - 1) ?: ""
                    val fullDateStr = "$jy/$jm/$jd"
                    val fullDateNamed = "$jd $monthName $jy"
                    fullDateStr.contains(normalizedQuery) || fullDateNamed.lowercase().contains(normalizedQuery)
                }
                
                titleMatch || descriptionMatch || reminderTimeMatch || createdAtMatch
            }
        }

        // Identify currently visible tasks in list
        val currentVisibleTasks = when (selectedTab) {
            0 -> filterTasks(activeTasks, selectedCategoryFilter, searchQuery)
            1 -> filterTasks(completedTasks, selectedCategoryFilter, searchQuery)
            else -> emptyList()
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        var showReportsScreen by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }

    // Enforce RTL Layout Direction globally in current view
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Drawer Header Info with custom logo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_karyar_logo),
                                contentDescription = "کاریار logo",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "کاریار",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "مدیریت هوشمند کارها",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Drawer Activities - Exactly 3 buttons requested by user:
                        
                        // Button 1: Reports (گزارشات)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            label = { Text("گزارشات کارها", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = showReportsScreen,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showReportsScreen = true
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Button 2: About Us (درباره ما)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            label = { Text("درباره ما", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showAboutDialog = true
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Button 3: Exit (خروج)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            label = { Text("خروج", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                (context as? android.app.Activity)?.finish()
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "توسعه‌دهنده: میلاد موسایی",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        ) {
            if (showReportsScreen) {
                Scaffold(
                    topBar = {
                        LargeTopAppBar(
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            title = {
                                Column {
                                    Text(
                                        text = "گزارشات کاریار",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = "امکان استخراج لایحه گزارش به فایل‌های مختلف",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { showReportsScreen = false },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "برگشت به خانه",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    var reportFormat by remember { mutableStateOf("pdf") } // pdf, excel, txt
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp)
                            .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Stats Card on Reports Page
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_karyar_logo),
                                        contentDescription = "کاریار",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "وضعیت کارهای ثبت شده",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = JalaliCalendarHelper.getPersianTodayString(),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(text = activeTasks.size.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "کارهای فعال", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(text = completedTasks.size.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text(text = "کارهای انجام شده", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(text = (activeTasks.size + completedTasks.size).toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = "کل کارها", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        
                        Text(
                            "قالب خروجی گزارش خود را انتخاب کنید:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        // Formats Radio Card list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FormatRadioCard(
                                title = "خروجی PDF (آماده چاپ)",
                                description = "یک سند گزارش مرتب با جدول و عناوین آماده استخراج و چاپ با فرمت پی‌دی‌اف تولید می‌کند.",
                                icon = Icons.Default.PictureAsPdf,
                                isSelected = reportFormat == "pdf",
                                onClick = { reportFormat = "pdf" }
                            )
                            FormatRadioCard(
                                title = "خروجی اکسل CSV (درخواست مالی)",
                                description = "یک فایل صفحه گسترده (تولید شده با متد UTF-8 BOM) که در نرم‌افزارهای مایکروسافت اکسل باز می‌شود.",
                                icon = Icons.Default.GridOn,
                                isSelected = reportFormat == "excel",
                                onClick = { reportFormat = "excel" }
                            )
                            FormatRadioCard(
                                title = "گزارش متنی ساده TXT",
                                description = "خروجی لایحه توصیفی کارهای روزانه به صورت پیام متنی جهت اشتراک در شبکه‌های اجتماعی.",
                                icon = Icons.Default.Description,
                                isSelected = reportFormat == "txt",
                                onClick = { reportFormat = "txt" }
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                when (reportFormat) {
                                    "pdf" -> exportToPdfFile(context, activeTasks, completedTasks)
                                    "excel" -> exportToExcelFile(context, activeTasks, completedTasks)
                                    "txt" -> exportToTxtFile(context, activeTasks, completedTasks)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("generate_report_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دریافت فایل لایحه گزارش", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                Scaffold(
                    topBar = {
                        LargeTopAppBar(
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            title = {
                                Column {
                                    Text(
                                        text = "کاریار",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        modifier = Modifier.testTag("app_title")
                                    )
                                    Text(
                                        text = JalaliCalendarHelper.getPersianTodayString(),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            navigationIcon = {
                                // Hamburger menu trigger gets placed in navigationIcon to render on TOP RIGHT in Persian RTL layout!
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "منو کشویی",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            actions = {
                                // Dark mode toggle on Left
                                IconButton(
                                    onClick = { viewModel.toggleDarkMode() },
                                    modifier = Modifier
                                        .testTag("theme_toggle_btn")
                                        .padding(horizontal = 4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                        contentDescription = if (isDarkMode) "تم روشن" else "تم تاریک",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    },
                floatingActionButton = {
                    // Pull the FAB upwards if the bulk action sliding bottom bar is visible
                    val paddingAmount = if (selectedTaskIds.isNotEmpty()) 80.dp else 16.dp
                    ExtendedFloatingActionButton(
                        text = { Text("افزودن کار جدید", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .testTag("add_task_fab")
                            .padding(bottom = paddingAmount, start = 16.dp)
                    )
                },
                floatingActionButtonPosition = FabPosition.End
            ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Task list Progress statistics bar
                val totalActive = activeTasks.size
                val totalCompleted = completedTasks.size
                val total = totalActive + totalCompleted
                val progress = if (total > 0) totalCompleted.toFloat() / total.toFloat() else 0f

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "پیشرفت امروز شما",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (total > 0) {
                                    "$totalCompleted کار انجام شده از مجموع $total کار (${(progress * 100).toInt()}٪)"
                                } else {
                                    "هنوز کاری برای امروز ثبت نکرده‌اید"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Inline Navigation tabs using standard layout
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("کارهای فعال", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.List, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("انجام شده‌ها", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Check, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("یادآوری‌ها", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                                if (activeReminders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = PriorityHigh) {
                                        Text(activeReminders.size.toString(), color = Color.White)
                                    }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                if (selectedTab != 2) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجو در بین کارها (عنوان یا تاریخ)...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("search_bar_input"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Show Category list only for active and completed task sheets
                if (selectedTab != 2) {
                    val categories = listOf("همه", "شخصی", "کاری", "خرید", "آموزش")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategoryFilter == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = category },
                                label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Render lists based on tabs
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> { // Active tasks
                            TaskListSection(
                                tasks = currentVisibleTasks,
                                selectedTaskIds = selectedTaskIds,
                                onToggleBulkSelection = { id ->
                                    selectedTaskIds = if (selectedTaskIds.contains(id)) {
                                        selectedTaskIds - id
                                    } else {
                                        selectedTaskIds + id
                                    }
                                },
                                onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                onDelete = { viewModel.deleteTask(it) },
                                emptyLabel = if (searchQuery.isNotEmpty()) "کار منطبقی با جستجوی شما یافت نشد." else "در دسته‌بندی «$selectedCategoryFilter» کار فعالی ندارید.",
                                promptAdd = "کار جدیدی برای خودت بنویس!",
                                onEdit = { taskToEdit = it }
                            )
                        }
                        1 -> { // Completed tasks
                            TaskListSection(
                                tasks = currentVisibleTasks,
                                selectedTaskIds = selectedTaskIds,
                                onToggleBulkSelection = { id ->
                                    selectedTaskIds = if (selectedTaskIds.contains(id)) {
                                        selectedTaskIds - id
                                    } else {
                                        selectedTaskIds + id
                                    }
                                },
                                onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                                onDelete = { viewModel.deleteTask(it) },
                                emptyLabel = if (searchQuery.isNotEmpty()) "کار انجام‌شده منطبقی با جستجوی شما یافت نشد." else "هنوز کار انجام شده‌ای در دسته‌بندی «$selectedCategoryFilter» ثبت نشده است.",
                                isHistorical = true
                            )
                        }
                        2 -> { // Active Reminders
                            RemindersListSection(
                                reminders = activeReminders,
                                onCancelReminder = { viewModel.cancelReminder(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                emptyLabel = "هیچ هشدار یا یادآوری فعالی ثبت نشده است."
                            )
                        }
                    }
                }

                // Animated Sliding Bulk Action Panel at the bottom
                AnimatedVisibility(
                    visible = selectedTaskIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${selectedTaskIds.size} کار انتخاب شده",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "انتخاب همه",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            selectedTaskIds = currentVisibleTasks.map { it.id }.toSet()
                                        }
                                    )
                                    Text(
                                        text = "لغو",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.clickable {
                                            selectedTaskIds = emptySet()
                                        }
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Complete Selected Button
                                Button(
                                    onClick = {
                                        val selectedTasks = currentVisibleTasks.filter { selectedTaskIds.contains(it.id) }
                                        viewModel.completeMultipleTasks(selectedTasks)
                                        selectedTaskIds = emptySet()
                                        Toast.makeText(context, "کارهای انتخاب شده با موفقیت به انجام رسیده ثبت شدند.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("انجام", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Delete Selected Button
                                Button(
                                    onClick = {
                                        val selectedTasks = currentVisibleTasks.filter { selectedTaskIds.contains(it.id) }
                                        viewModel.deleteMultipleTasks(selectedTasks)
                                        selectedTaskIds = emptySet()
                                        Toast.makeText(context, "کارهای انتخاب شده با موفقیت حذف شدند.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PriorityHigh,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // More dropdown options
                                var showBatchMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showBatchMenu = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "بیشتر", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }

                                    DropdownMenu(
                                        expanded = showBatchMenu,
                                        onDismissRequest = { showBatchMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("انجام همه کارهای لیست", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) },
                                            onClick = {
                                                showBatchMenu = false
                                                viewModel.completeMultipleTasks(currentVisibleTasks)
                                                selectedTaskIds = emptySet()
                                                Toast.makeText(context, "تمامی کارهای لیست انجام شدند.", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("حذف همه کارهای لیست", fontWeight = FontWeight.Bold, color = PriorityHigh) },
                                            onClick = {
                                                showBatchMenu = false
                                                viewModel.deleteMultipleTasks(currentVisibleTasks)
                                                selectedTaskIds = emptySet()
                                                Toast.makeText(context, "تمامی کارهای لیست حذف شدند.", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

        // Animated Dialog Form for Adding Tasks
        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, cat, prio, remindTime ->
                    viewModel.addTask(title, desc, cat, prio, remindTime)
                    showAddDialog = false
                    Toast.makeText(context, "کار با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Animated Dialog Form for Editing Tasks
        if (taskToEdit != null) {
            AddTaskDialog(
                taskToEdit = taskToEdit,
                onDismiss = { taskToEdit = null },
                onSave = { title, desc, cat, prio, remindTime ->
                    val updated = taskToEdit!!.copy(
                        title = title,
                        description = desc,
                        category = cat,
                        priority = prio,
                        reminderTime = remindTime
                    )
                    viewModel.updateTask(updated, remindTime)
                    taskToEdit = null
                    Toast.makeText(context, "تغییرات با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // About Us Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_karyar_logo),
                            contentDescription = "کاریار",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "درباره کاریار",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "نرم‌افزار مدیریت کارهای روزانه «کاریار» یک دستیار هوشمند و نوین برای پیگیری وظایف، اهداف و برنامه‌ریزی‌های روزانه شماست.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                        Text(
                            text = "🎯 برنامه‌ریزی ساده و سریع کارها\n⏰ تنظیم هشدارهای یادآوری دقیق\n📊 خروجی‌های گزارش‌دهی PDF و Excel و متنی\n💚 طراحی منطبق بر استانداردهای بومی و مذهبی کشور",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            text = "توسعه‌دهنده: میلاد موسایی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "نسخه ۱.۱.۰ (ارائه‌شده در سال ۱۴۰۵)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAboutDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بستن", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun TaskListSection(
    tasks: List<TaskEntity>,
    selectedTaskIds: Set<Int>,
    onToggleBulkSelection: (Int) -> Unit,
    onToggleCompletion: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    emptyLabel: String,
    promptAdd: String = "",
    isHistorical: Boolean = false,
    onEdit: ((TaskEntity) -> Unit)? = null
) {
    if (tasks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = emptyLabel,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(0.85f)
            )
            if (promptAdd.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = promptAdd,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { onToggleCompletion(task) },
                    onDelete = { onDelete(task) },
                    onEdit = if (onEdit != null) { { onEdit(task) } } else null,
                    isSelectedForBulk = selectedTaskIds.contains(task.id),
                    onToggleBulkSelect = { onToggleBulkSelection(task.id) }
                )
            }
        }
    }
}

@Composable
fun RemindersListSection(
    reminders: List<TaskEntity>,
    onCancelReminder: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    emptyLabel: String
) {
    if (reminders.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = emptyLabel,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "هنگام ثبت کار جدید، زنگ یادآوری را روشن و تنظیم کنید.",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reminders, key = { it.id }) { task ->
                ReminderCard(
                    task = task,
                    onCancelAlarm = { onCancelReminder(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    isSelectedForBulk: Boolean = false,
    onToggleBulkSelect: () -> Unit = {}
) {
    val priorityColor = when (task.priority) {
        3 -> PriorityHigh
        2 -> PriorityMedium
        else -> PriorityLow
    }

    val priorityText = when (task.priority) {
        3 -> "فوری و مهم"
        2 -> "متوسط"
        else -> "کم‌اهمیت"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Visual Priority Side bar indicator for beautiful layout
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(priorityColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular multi-selection checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelectedForBulk) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(
                            2.dp,
                            if (isSelectedForBulk) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .clickable { onToggleBulkSelect() }
                        .testTag("checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedForBulk) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "انتخاب شده",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Category Pill badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                task.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Priority Badge display
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "اولویت: $priorityText",
                                fontSize = 10.sp,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Reminder status badge (bell icon)
                        if (task.reminderTime != null) {
                            val isActive = task.isReminderActive && !task.isCompleted
                            val formattedTime = formatTimestampToPersian(task.reminderTime)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "یادآوری: $formattedTime",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Only show Edit and Done icons for active (not completed) tasks
                    if (!task.isCompleted) {
                        // Complete / Done Button
                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.testTag("done_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "ایکن انجام شد",
                                tint = Color(0xFF4CAF50), // Green
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Edit Button
                        if (onEdit != null) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.testTag("edit_btn_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش کار",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Delete Action Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف کار",
                            tint = PriorityHigh.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    task: TaskEntity,
    onCancelAlarm: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedTime = formatTimestampToPersian(task.reminderTime ?: 0)
    
    val priorityColor = when (task.priority) {
        3 -> PriorityHigh
        2 -> PriorityMedium
        else -> PriorityLow
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "زمان زنگ: $formattedTime",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(task.category, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "اولویت بالا",
                            fontSize = 9.sp,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick actions to deactivate reminder or delete item
            TextButton(
                onClick = onCancelAlarm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("غیرفعال", fontSize = 11.sp)
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف کار",
                    tint = PriorityHigh
                )
            }
        }
    }
}

// Dialog Composable to construct and add a task
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    taskToEdit: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, category: String, priority: Int, reminderTime: Long?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "شخصی") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: 1) } // 1 = Low, 2 = Med, 3 = High

    var enableReminder by remember { mutableStateOf(taskToEdit?.reminderTime != null) }
    var selectedCalendar by remember { 
        mutableStateOf(
            Calendar.getInstance().apply { 
                if (taskToEdit?.reminderTime != null) {
                    timeInMillis = taskToEdit.reminderTime!!
                }
            }
        ) 
    }

    var showShamsiDatePicker by remember { mutableStateOf(false) }

    var selectedDateText by remember { 
        mutableStateOf(
            if (taskToEdit?.reminderTime != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = taskToEdit.reminderTime!! }
                val (jy, jm, jd) = JalaliCalendarHelper.gregorianToJalali(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                "$jy/$jm/$jd"
            } else "امروز"
        ) 
    }
    var selectedTimeText by remember { 
        mutableStateOf(
            if (taskToEdit?.reminderTime != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = taskToEdit.reminderTime!! }
                String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else "انتخاب ساعت"
        ) 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .testTag("add_task_dialog"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (taskToEdit != null) "ویرایش کار" else "ثبت کار جدید",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Title field
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("عنوان کار (مثلاً: خرید داروی مادربزرگ)") },
                                placeholder = { Text("عنوانی برای کار خود بنویسید") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_title"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        item {
                            // Description field
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("توضیحات بیشتر (اختیاری)") },
                                placeholder = { Text("جزئیات بیشتر کار خود را بنویسید...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_description")
                            )
                        }

                        item {
                            // Category Select Grid
                            Text("دسته‌بندی کار:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            val listCategories = listOf("شخصی", "کاری", "خرید", "آموزش")
                            
                            FlowRow( // Wrapping in grid safely via M3 FlowRow APIs
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listCategories.forEach { cat ->
                                    val isCatSelected = category == cat
                                    val pillBackground = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    val pillText = if (isCatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(32.dp))
                                            .background(pillBackground)
                                            .clickable { category = cat }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                            .testTag("category_pill_$cat")
                                    ) {
                                        Text(cat, color = pillText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        item {
                            // Priority select Row buttons
                            Text("اولویت کار:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Priority Options: 1 = Low, 2 = Medium, 3 = High
                                priorityOption(
                                    label = "کم‌اهمیت",
                                    color = PriorityLow,
                                    isSelected = priority == 1,
                                    modifier = Modifier.weight(1f).testTag("priority_1").clickable { priority = 1 }
                                )
                                priorityOption(
                                    label = "متوسط",
                                    color = PriorityMedium,
                                    isSelected = priority == 2,
                                    modifier = Modifier.weight(1f).testTag("priority_2").clickable { priority = 2 }
                                )
                                priorityOption(
                                    label = "مهم و فوری",
                                    color = PriorityHigh,
                                    isSelected = priority == 3,
                                    modifier = Modifier.weight(1f).testTag("priority_3").clickable { priority = 3 }
                                )
                            }
                        }

                        item {
                            // Reminders toggler switch
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("تنظیم زنگ یادآوری", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("نمایش هشدار روی گوشی در ساعت مقرر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = enableReminder,
                                            onCheckedChange = { enableReminder = it },
                                            modifier = Modifier.testTag("reminder_switch")
                                        )
                                    }

                                    AnimatedVisibility(visible = enableReminder) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Date button picker
                                                Button(
                                                    onClick = {
                                                        showShamsiDatePicker = true
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("date_picker_trigger"),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                ) {
                                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(selectedDateText, fontSize = 12.sp)
                                                }

                                                if (showShamsiDatePicker) {
                                                    val currentYearMonthDay = if (taskToEdit?.reminderTime != null) {
                                                        val cal = Calendar.getInstance().apply { timeInMillis = taskToEdit.reminderTime!! }
                                                        JalaliCalendarHelper.gregorianToJalali(
                                                            cal.get(Calendar.YEAR),
                                                            cal.get(Calendar.MONTH) + 1,
                                                            cal.get(Calendar.DAY_OF_MONTH)
                                                        )
                                                    } else {
                                                        JalaliCalendarHelper.gregorianToJalali(
                                                            Calendar.getInstance().get(Calendar.YEAR),
                                                            Calendar.getInstance().get(Calendar.MONTH) + 1,
                                                            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                                        )
                                                    }

                                                    ShamsiDatePickerDialog(
                                                        initialYear = currentYearMonthDay.first,
                                                        initialMonth = currentYearMonthDay.second,
                                                        initialDay = currentYearMonthDay.third,
                                                        onDismiss = { showShamsiDatePicker = false },
                                                        onConfirm = { jy, jm, jd ->
                                                            val (gy, gm, gd) = JalaliCalendarHelper.jalaliToGregorian(jy, jm, jd)
                                                            selectedCalendar.set(Calendar.YEAR, gy)
                                                            selectedCalendar.set(Calendar.MONTH, gm - 1)
                                                            selectedCalendar.set(Calendar.DAY_OF_MONTH, gd)
                                                            selectedDateText = "$jy/$jm/$jd"
                                                            showShamsiDatePicker = false
                                                        }
                                                    )
                                                }

                                                // Time button picker
                                                Button(
                                                    onClick = {
                                                        val now = Calendar.getInstance()
                                                        TimePickerDialog(
                                                            context,
                                                            { _, hourOfDay, minute ->
                                                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                                selectedCalendar.set(Calendar.MINUTE, minute)
                                                                selectedCalendar.set(Calendar.SECOND, 0)
                                                                selectedTimeText = String.format("%02d:%02d", hourOfDay, minute)
                                                            },
                                                            now.get(Calendar.HOUR_OF_DAY),
                                                            now.get(Calendar.MINUTE),
                                                            true
                                                        ).show()
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("time_picker_trigger"),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                ) {
                                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(selectedTimeText, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons Save / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "لطفاً عنوان کار را وارد کنید", Toast.LENGTH_SHORT).show()
                            } else {
                                val reminderTime = if (enableReminder) selectedCalendar.timeInMillis else null
                                onSave(title, description, category, priority, reminderTime)
                            }
                        },
                        modifier = Modifier.weight(1.5f).testTag("save_task_btn")
                    ) {
                        Text(if (taskToEdit != null) "ذخیره تغییرات" else "ثبت کار جدید", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

@Composable
fun priorityOption(
    label: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent
    val border = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textStyleColor = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, border),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = textStyleColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Convert timestamp into Persian hours and date string helpers
fun formatTimestampToPersian(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    
    val now = Calendar.getInstance()

    val dateStr = if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
        "امروز"
    } else {
        val nextDay = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (calendar.get(Calendar.YEAR) == nextDay.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == nextDay.get(Calendar.DAY_OF_YEAR)) {
            "فردا"
        } else {
            val (jy, jm, jd) = JalaliCalendarHelper.gregorianToJalali(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            "$jy/$jm/$jd"
        }
    }

    return String.format("%s ساعت %02d:%02d", dateStr, hour, minute)
}

@Composable
fun ShamsiDatePickerDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedDay by remember { mutableStateOf(initialDay) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انتخاب تاریخ شمسی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Day column selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("روز", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            val maxDays = if (selectedMonth <= 6) 31 else if (selectedMonth <= 11) 30 else 29
                            if (selectedDay > maxDays) {
                                selectedDay = maxDays
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(vertical = 48.dp)
                            ) {
                                items((1..maxDays).toList()) { d ->
                                    val isSelected = d == selectedDay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDay = d }
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = d.toString(),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Month column selector
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("ماه", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(vertical = 48.dp)
                            ) {
                                itemsIndexed(JalaliCalendarHelper.persianMonthNames) { index, name ->
                                    val monthNum = index + 1
                                    val isSelected = monthNum == selectedMonth
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMonth = monthNum }
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Year column selector
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("سال", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            val years = (1405..1415).toList()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(vertical = 48.dp)
                            ) {
                                items(years) { y ->
                                    val isSelected = y == selectedYear
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedYear = y }
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = y.toString(),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = { onConfirm(selectedYear, selectedMonth, selectedDay) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تایید")
                    }
                }
            }
        }
    }
}

fun shareFile(context: android.content.Context, file: File, mimeType: String, title: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, title))
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در اشتراک‌گذاری فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun exportToTxtFile(context: android.content.Context, activeTasks: List<TaskEntity>, completedTasks: List<TaskEntity>) {
    try {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "گزارش_کاریار_${System.currentTimeMillis()}.txt")
        val fos = FileOutputStream(file)
        fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM
        
        val builder = java.lang.StringBuilder()
        builder.append("📊 گزارش کارهای روزانه شما (برنامه کاریار) 📊\n")
        builder.append("تاریخ گزارش: ${JalaliCalendarHelper.getPersianTodayString()}\n")
        builder.append("==================================\n\n")
        
        builder.append("📝 کارهای فعال:\n")
        if (activeTasks.isEmpty()) {
            builder.append("[هیچ کار فعالی یافت نشد]\n")
        } else {
            activeTasks.forEachIndexed { index, task ->
                val priorityName = when (task.priority) {
                    3 -> "فوری و مهم"
                    2 -> "متوسط"
                    else -> "کم‌اهمیت"
                }
                builder.append("${index + 1}. ${task.title}\n")
                if (task.description.isNotEmpty()) {
                    builder.append("   - توضیحات: ${task.description}\n")
                }
                builder.append("   - دسته‌بندی: ${task.category}\n")
                builder.append("   - اولویت: $priorityName\n")
                if (task.reminderTime != null) {
                    builder.append("   - زنگ یادآوری: ${formatTimestampToPersian(task.reminderTime)}\n")
                }
                builder.append("\n")
            }
        }
        
        builder.append("==================================\n")
        builder.append("✅ کارهای انجام‌شده:\n")
        if (completedTasks.isEmpty()) {
            builder.append("[هیچ کار انجام‌شده‌ای یافت نشد]\n")
        } else {
            completedTasks.forEachIndexed { index, task ->
                builder.append("${index + 1}. ${task.title}\n")
                if (task.description.isNotEmpty()) {
                    builder.append("   - توضیحات: ${task.description}\n")
                }
                builder.append("   - دسته‌بندی: ${task.category}\n")
                builder.append("\n")
            }
        }
        
        builder.append("تولید شده توسط نرم‌افزار «کاریار»\n")
        builder.append("توسعه‌دهنده: میلاد موسایی")
        
        fos.write(builder.toString().toByteArray(Charsets.UTF_8))
        fos.close()
        
        shareFile(context, file, "text/plain", "ارسال لایحه گزارش کاریار")
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در تولید فایل متنی: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun exportToExcelFile(context: android.content.Context, activeTasks: List<TaskEntity>, completedTasks: List<TaskEntity>) {
    try {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "گزارش_کاریار_${System.currentTimeMillis()}.csv")
        val fos = FileOutputStream(file)
        fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM
        
        val builder = java.lang.StringBuilder()
        builder.append("ردیف;عنوان کار;توضیحات;دسته‌بندی;اولویت;وضعیت;زمان یادآوری\n")
        
        var rowNum = 1
        activeTasks.forEach { task ->
            val prioName = when (task.priority) {
                3 -> "فوری و مهم"
                2 -> "متوسط"
                else -> "کم‌اهمیت"
            }
            val reminder = if (task.reminderTime != null) formatTimestampToPersian(task.reminderTime) else "-"
            val cleanedTitle = task.title.replace(";", " ").replace("\n", " ")
            val cleanedDesc = task.description.replace(";", " ").replace("\n", " ")
            
            builder.append("$rowNum;\"$cleanedTitle\";\"$cleanedDesc\";\"${task.category}\";\"$prioName\";\"فعال\";\"$reminder\"\n")
            rowNum++
        }
        
        completedTasks.forEach { task ->
            val prioName = when (task.priority) {
                3 -> "فوری و مهم"
                2 -> "متوسط"
                else -> "کم‌اهمیت"
            }
            val reminder = if (task.reminderTime != null) formatTimestampToPersian(task.reminderTime) else "-"
            val cleanedTitle = task.title.replace(";", " ").replace("\n", " ")
            val cleanedDesc = task.description.replace(";", " ").replace("\n", " ")
            
            builder.append("$rowNum;\"$cleanedTitle\";\"$cleanedDesc\";\"${task.category}\";\"$prioName\";\"انجام شده\";\"$reminder\"\n")
            rowNum++
        }
        
        fos.write(builder.toString().toByteArray(Charsets.UTF_8))
        fos.close()
        
        shareFile(context, file, "text/comma-separated-values", "ارسال خروجی اکسل کاریار")
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در تولید فایل اکسل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun exportToPdfFile(context: android.content.Context, activeTasks: List<TaskEntity>, completedTasks: List<TaskEntity>) {
    try {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "گزارش_کاریار_${System.currentTimeMillis()}.pdf")
        
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = android.graphics.Paint()
        
        // Header green banner
        paint.color = android.graphics.Color.parseColor("#2E7D32")
        canvas.drawRect(0f, 0f, 595f, 90f, paint)
        
        val titlePaint = TextPaint().apply {
            color = android.graphics.Color.WHITE
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val todayStr = JalaliCalendarHelper.getPersianTodayString()
        
        val headerLayout = @Suppress("DEPRECATION") StaticLayout(
            "گزارش مدیریت کارهای روزانه - نرم‌افزار کاریار\nتاریخ گزارش: $todayStr",
            titlePaint,
            500,
            Layout.Alignment.ALIGN_NORMAL,
            1.1f,
            0f,
            false
        )
        
        canvas.save()
        canvas.translate(47f, 20f)
        headerLayout.draw(canvas)
        canvas.restore()
        
        // Setup body fonts
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        
        val boldPaint = TextPaint().apply {
            color = android.graphics.Color.parseColor("#1B5E20")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        var currentY = 115f
        
        // Stats bar
        paint.color = android.graphics.Color.parseColor("#E8F5E9")
        canvas.drawRect(40f, currentY, 555f, currentY + 45f, paint)
        
        val statsText = "آمار کل:  کارهای در دست اقدام: ${activeTasks.size}  |  کارهای انجام‌شده دیروز و امروز: ${completedTasks.size}"
        val statsLayout = @Suppress("DEPRECATION") StaticLayout(
            statsText,
            boldPaint,
            480,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0f,
            false
        )
        canvas.save()
        canvas.translate(55f, currentY + 12f)
        statsLayout.draw(canvas)
        canvas.restore()
        
        currentY += 70f
        
        // Section: Active Tasks
        val actTitleLayout = @Suppress("DEPRECATION") StaticLayout(
            "📝 لیست کارهای فعال روزانه شما:",
            boldPaint,
            500,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,
            0f,
            false
        )
        canvas.save()
        canvas.translate(45f, currentY)
        actTitleLayout.draw(canvas)
        canvas.restore()
        
        currentY += 25f
        
        if (activeTasks.isEmpty()) {
            val emptyLayout = @Suppress("DEPRECATION") StaticLayout(
                "[کار فعالی یافت نشد]",
                textPaint,
                480,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0f,
                false
            )
            canvas.save()
            canvas.translate(60f, currentY)
            emptyLayout.draw(canvas)
            canvas.restore()
            currentY += 25f
        } else {
            activeTasks.take(15).forEachIndexed { index, task ->
                if (currentY > 780f) return@forEachIndexed
                
                val priorityName = when (task.priority) {
                    3 -> "فوری و مهم"
                    2 -> "متوسط"
                    else -> "کم‌اهمیت"
                }
                val descStr = if (task.description.isNotEmpty()) " (توضیح: ${task.description})" else ""
                val tStr = "${index + 1}. ${task.title} [دسته‌بندی: ${task.category} | اولویت: $priorityName]$descStr"
                
                val taskLayout = @Suppress("DEPRECATION") StaticLayout(
                    tStr,
                    textPaint,
                    480,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.1f,
                    0f,
                    false
                )
                canvas.save()
                canvas.translate(60f, currentY)
                taskLayout.draw(canvas)
                canvas.restore()
                currentY += taskLayout.height + 10f
            }
        }
        
        currentY += 15f
        
        // Section: Completed Tasks
        if (currentY < 740f) {
            val compTitleLayout = @Suppress("DEPRECATION") StaticLayout(
                "✅ کارهای به اتمام رسیده اخیر:",
                boldPaint,
                500,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0f,
                false
            )
            canvas.save()
            canvas.translate(45f, currentY)
            compTitleLayout.draw(canvas)
            canvas.restore()
            
            currentY += 25f
            
            if (completedTasks.isEmpty()) {
                val emptyLayout = @Suppress("DEPRECATION") StaticLayout(
                    "[هیچ کار انجام‌شده‌ای یافت نشد]",
                    textPaint,
                    480,
                    Layout.Alignment.ALIGN_NORMAL,
                    1.0f,
                    0f,
                    false
                )
                canvas.save()
                canvas.translate(60f, currentY)
                emptyLayout.draw(canvas)
                canvas.restore()
            } else {
                completedTasks.take(10).forEachIndexed { index, task ->
                    if (currentY > 780f) return@forEachIndexed
                    
                    val tStr = "${index + 1}. ${task.title} [دسته‌بندی: ${task.category}]"
                    val taskLayout = @Suppress("DEPRECATION") StaticLayout(
                        tStr,
                        textPaint,
                        480,
                        Layout.Alignment.ALIGN_NORMAL,
                        1.1f,
                        0f,
                        false
                    )
                    canvas.save()
                    canvas.translate(60f, currentY)
                    taskLayout.draw(canvas)
                    canvas.restore()
                    currentY += taskLayout.height + 10f
                }
            }
        }
        
        // Footer divider line and text
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawLine(40f, 800f, 555f, 800f, paint)
        
        val footerPaint = TextPaint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }
        val footerLayout = @Suppress("DEPRECATION") StaticLayout(
            "کاریار  |  سامانه دستیار ارزیابی کارها  |  برنامه نویسی شده توسط میلاد موسایی",
            footerPaint,
            480,
            Layout.Alignment.ALIGN_CENTER,
            1.0f,
            0f,
            false
        )
        canvas.save()
        canvas.translate(55f, 805f)
        footerLayout.draw(canvas)
        canvas.restore()
        
        pdfDoc.finishPage(page)
        
        val fos = FileOutputStream(file)
        pdfDoc.writeTo(fos)
        pdfDoc.close()
        fos.close()
        
        shareFile(context, file, "application/pdf", "ارسال لایحه pdf کاریار")
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در تولید فایل PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun exportAndShareTasks(
    context: android.content.Context,
    activeTasks: List<TaskEntity>,
    completedTasks: List<TaskEntity>
) {
    exportToTxtFile(context, activeTasks, completedTasks)
}

@Composable
fun FormatRadioCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val borderWidth = if (isSelected) 2.dp else 1.dp
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}


