package com.karland.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.karland.app.notification.AlarmSoundManager
import com.karland.app.notification.NotificationHelper
import com.karland.app.ui.screens.*
import com.karland.app.ui.theme.KaryarTheme
import com.karland.app.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()
    private var pendingAlarmTaskId by mutableStateOf<Long?>(null)
    private var pendingAlarmTitle by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAlarmIntent(intent)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            var splashDone by remember { mutableStateOf(viewModel.splashShown) }

            val sysConfig = LocalConfiguration.current
            val appConfig = remember(isDarkMode, sysConfig) {
                Configuration(sysConfig).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (isDarkMode) Configuration.UI_MODE_NIGHT_YES
                            else Configuration.UI_MODE_NIGHT_NO
                }
            }

            SideEffect {
                val ctrl = WindowCompat.getInsetsController(window, window.decorView)
                ctrl.isAppearanceLightStatusBars = !isDarkMode
                ctrl.isAppearanceLightNavigationBars = !isDarkMode
            }

            CompositionLocalProvider(
                LocalConfiguration provides appConfig,
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                key(isDarkMode) {
                    KaryarTheme(darkTheme = isDarkMode) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) {
                            val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
                            val lastSeenVersion by viewModel.lastSeenVersionCode.collectAsState()
                            val navController = rememberNavController()
                            val startDest = when {
                                !splashDone -> "splash"
                                !onboardingCompleted -> "onboarding"
                                else -> "task_list"
                            }
                            NavHost(
                                navController = navController,
                                startDestination = startDest
                            ) {
                                composable("splash") {
                                    SplashScreen(onFinished = {
                                        splashDone = true
                                        viewModel.markSplashShown()
                                        val dest = if (!onboardingCompleted) "onboarding" else "task_list"
                                        navController.navigate(dest) {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    })
                                }
                                composable("onboarding") {
                                    OnboardingScreen(onStart = {
                                        viewModel.setOnboardingCompleted()
                                        navController.navigate("task_list") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    })
                                }
                                composable("task_list") {
                                    TaskListScreen(
                                        viewModel = viewModel,
                                        onAddTask = { navController.navigate("add_task") },
                                        onEditTask = { id -> navController.navigate("edit_task/$id") },
                                        onNavigateToStats = { navController.navigate("statistics") },
                                        onNavigateToSettings = { navController.navigate("settings") },
                                        onNavigateToAbout = { navController.navigate("about") }
                                    )
                                }
                                composable("add_task") {
                                    AddEditTaskScreen(taskId = null, viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
                                }
                                composable(
                                    route = "edit_task/{taskId}",
                                    arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                                ) { back ->
                                    val id = back.arguments?.getLong("taskId") ?: return@composable
                                    AddEditTaskScreen(taskId = id, viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
                                }
                                composable("statistics") {
                                    StatisticsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToAbout = { navController.navigate("about") }
                                    )
                                }
                                composable("about") {
                                    AboutScreen(onNavigateBack = { navController.popBackStack() })
                                }
                            }

                            // Alarm dismiss dialog — shown when app opened from a reminder notification
                            val alarmId = pendingAlarmTaskId
                            if (alarmId != null) {
                                AlertDialog(
                                    onDismissRequest = {
                                        AlarmSoundManager.stop()
                                        pendingAlarmTaskId = null
                                    },
                                    icon = { Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
                                    title = { Text("یادآوری", fontWeight = FontWeight.Bold) },
                                    text = { Text(pendingAlarmTitle, style = MaterialTheme.typography.bodyLarge) },
                                    confirmButton = {
                                        Button(onClick = {
                                            AlarmSoundManager.stop()
                                            viewModel.completeTask(alarmId)
                                            pendingAlarmTaskId = null
                                        }) { Text("انجام شد") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            AlarmSoundManager.stop()
                                            pendingAlarmTaskId = null
                                        }) { Text("بستن") }
                                    }
                                )
                            }

                            // What's New dialog — shown once per version for returning users
                            val currentVersionCode = viewModel.currentVersionCode
                            if (splashDone && onboardingCompleted && lastSeenVersion in 0 until currentVersionCode && pendingAlarmTaskId == null) {
                                WhatsNewDialog(onDismiss = { viewModel.markVersionSeen() })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmIntent(intent)
    }

    private fun handleAlarmIntent(intent: Intent?) {
        val taskId = intent?.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L) ?: -1L
        if (taskId != -1L) {
            pendingAlarmTaskId = taskId
            pendingAlarmTitle = intent?.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "یادآوری کار"
        }
    }
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویژگی‌های جدید نسخه ۱.۲", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("در این به‌روزرسانی امکانات زیر اضافه شده:", style = MaterialTheme.typography.bodyMedium)
                WhatsNewRow(Icons.Default.MusicNote, "انتخاب آهنگ آلارم برای هر یادآوری از صداهای پیش‌فرض گوشی")
                WhatsNewRow(Icons.Default.VolumeUp, "پیش‌نمایش صدا قبل از انتخاب آهنگ")
                WhatsNewRow(Icons.Default.NotificationsOff, "گزینه «بدون آلارم» برای یادآوری‌های بی‌صدا")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("فهمیدم") }
        }
    )
}

@Composable
private fun WhatsNewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
