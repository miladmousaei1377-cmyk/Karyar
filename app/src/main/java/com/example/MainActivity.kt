package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.presentation.AgentChatScreen
import com.example.agent.presentation.AgentViewModel
import com.example.ui.screens.TaskApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewModel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()
    private val agentViewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by taskViewModel.isDarkMode.collectAsStateWithLifecycle()
            var showAgent by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                if (showAgent) {
                    AgentChatScreen(
                        viewModel = agentViewModel,
                        onBack = { showAgent = false }
                    )
                } else {
                    TaskApp(
                        viewModel = taskViewModel,
                        onNavigateToAgent = { showAgent = true }
                    )
                }
            }
        }
    }
}
