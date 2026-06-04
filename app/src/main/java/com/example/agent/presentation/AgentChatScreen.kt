package com.example.agent.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.presentation.components.MessageBubble
import com.example.agent.presentation.components.TypingIndicator
import com.example.agent.presentation.components.VoiceButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(
    viewModel: AgentViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.checkMicPermission() }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Column {
                            Text(
                                "دستیار هوشمند",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Text(
                                "کاریار — ایجنت هوش مصنوعی",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "برگشت",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "پاک کردن مکالمه",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Status bar
                AnimatedVisibility(visible = uiState.statusText.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.statusText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Messages list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            WelcomeCard()
                        }
                    }
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubble(message = msg)
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TypingIndicator()
                            }
                        }
                    }
                }

                // Voice recording indicator
                AnimatedVisibility(visible = uiState.isRecording) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "در حال ضبط... انگشت را رها کن برای ارسال",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Input bar
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VoiceButton(
                            isRecording = uiState.isRecording,
                            isSpeaking = uiState.isSpeaking,
                            hasMicPermission = uiState.hasMicPermission,
                            onPressStart = { viewModel.startRecording() },
                            onPressEnd = { viewModel.stopRecordingAndSend() },
                            onCancelRecording = { viewModel.cancelRecording() },
                            onStopSpeaking = { viewModel.stopSpeaking() },
                        )

                        Spacer(Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    if (!uiState.hasMicPermission) "برای صدا، دسترسی میکروفون لازم است"
                                    else "بنویس یا دکمه صدا را نگه دار...",
                                    fontSize = 13.sp
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                if (!uiState.hasMicPermission) {
                                    TextButton(
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    ) { Text("دسترسی", fontSize = 12.sp) }
                                }
                            }
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val text = inputText.trim()
                                if (text.isNotBlank()) {
                                    viewModel.sendText(text)
                                    inputText = ""
                                    scope.launch {
                                        listState.animateScrollToItem(
                                            maxOf(0, listState.layoutInfo.totalItemsCount - 1)
                                        )
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !uiState.isLoading)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "ارسال",
                                tint = if (inputText.isNotBlank() && !uiState.isLoading)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("👋", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "سلام! من دستیار هوشمند کاریار هستم.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "می‌توانم جستجو کنم، محاسبه کنم،\nآب‌وهوا بگیرم، وظایف مدیریت کنم\nو به سوالات شما پاسخ دهم.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "چه کمکی می‌توانم بکنم؟",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
