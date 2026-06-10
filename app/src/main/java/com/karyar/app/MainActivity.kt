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
            KaryarTheme(darkTheme = isDarkMode) {
                // key(isDarkMode) forces full recomposition when theme switches → fixes white-text-on-light bug
                key(isDarkMode) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            val navController = rememberNavController()
                            NavHost(navController = navController, startDestination = "splash") {
                                composable("splash") {
                                    SplashScreen(onFinished = {
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
