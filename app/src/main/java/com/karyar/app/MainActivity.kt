package com.karyar.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.karyar.app.ui.screens.*
import com.karyar.app.ui.theme.KaryarTheme
import com.karyar.app.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            // Survives Activity recreation (e.g. when system dark mode changes):
            // ViewModel outlives Activity, so splashShown=true persists → no replay.
            var splashDone by remember { mutableStateOf(viewModel.splashShown) }

            // Override LocalConfiguration.uiMode to match the APP preference, not the system.
            // This ensures isSystemInDarkTheme() and all Material3 internal token resolution
            // reflect our own dark mode setting even when the system dark mode differs.
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
                // key outside KaryarTheme: fully recreates MaterialTheme + all color caches
                key(isDarkMode) {
                    KaryarTheme(darkTheme = isDarkMode) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) {
                            val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
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
                        }
                    }
                }
            }
        }
    }
}

