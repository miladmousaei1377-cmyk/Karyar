package com.karyar.app

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

            // splashDone lives OUTSIDE key(isDarkMode) so it survives theme changes.
            // When theme toggles, key resets NavHost but splashDone stays true → no splash replay.
            var splashDone by remember { mutableStateOf(false) }

            SideEffect {
                val ctrl = WindowCompat.getInsetsController(window, window.decorView)
                ctrl.isAppearanceLightStatusBars = !isDarkMode
                ctrl.isAppearanceLightNavigationBars = !isDarkMode
            }

            // key wraps KaryarTheme itself so Material3 internal color caches are fully
            // reset on every theme toggle — prevents white text after dark→light switch.
            key(isDarkMode) {
                KaryarTheme(darkTheme = isDarkMode) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ) {
                            val navController = rememberNavController()
                            NavHost(
                                navController = navController,
                                startDestination = if (splashDone) "task_list" else "splash"
                            ) {
                                composable("splash") {
                                    SplashScreen(onFinished = {
                                        splashDone = true
                                        navController.navigate("task_list") {
                                            popUpTo("splash") { inclusive = true }
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
