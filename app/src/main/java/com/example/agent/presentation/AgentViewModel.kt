package com.example.agent.presentation

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.data.AgentOrchestrator
import com.example.agent.data.LLMService
import com.example.agent.domain.models.ApiMessage
import com.example.agent.domain.models.ChatMessage
import com.example.agent.domain.models.MessageRole
import com.example.database.AppDatabase
import com.example.memory.MemoryService
import com.example.repository.TaskRepository
import com.example.tools.ToolRegistry
import com.example.voice.AudioRecorder
import com.example.voice.STTService
import com.example.voice.TTSService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isSpeaking: Boolean = false,
    val statusText: String = "",
    val hasMicPermission: Boolean = false,
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private val db = AppDatabase.getDatabase(application)
    private val memory = MemoryService(db.conversationDao, application)
    private val tools = ToolRegistry(TaskRepository(db.taskDao))
    private val llm = LLMService()
    private val orchestrator = AgentOrchestrator(llm, tools, memory)
    private val audioRecorder = AudioRecorder(application)
    private val stt = STTService()
    private val tts = TTSService(application)

    private val apiHistory = mutableListOf<ApiMessage>()
    private var isVoiceMode = false

    init {
        checkMicPermission()
        orchestrator.onStatusUpdate = { msg ->
            _uiState.update { it.copy(statusText = msg) }
        }
    }

    fun checkMicPermission() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(), android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasMicPermission = granted) }
    }

    fun sendText(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return
        isVoiceMode = false
        addMessage(ChatMessage(role = MessageRole.USER, text = text))
        runAgent(text, voice = false)
    }

    fun startRecording() {
        if (!_uiState.value.hasMicPermission) return
        runCatching { audioRecorder.startRecording() }
        _uiState.update { it.copy(isRecording = true) }
    }

    fun stopRecordingAndSend() {
        val file = audioRecorder.stopRecording()
        _uiState.update { it.copy(isRecording = false, statusText = "در حال تبدیل صدا...") }
        if (file == null) {
            _uiState.update { it.copy(statusText = "") }
            return
        }
        viewModelScope.launch {
            val text = stt.transcribe(file)
            _uiState.update { it.copy(statusText = "") }
            if (text.isNullOrBlank()) {
                addMessage(ChatMessage(role = MessageRole.AGENT, text = "صدا تشخیص داده نشد. لطفاً دوباره تلاش کنید."))
                return@launch
            }
            isVoiceMode = true
            addMessage(ChatMessage(role = MessageRole.USER, text = text, isVoice = true))
            runAgent(text, voice = true)
        }
    }

    fun cancelRecording() {
        audioRecorder.cancel()
        _uiState.update { it.copy(isRecording = false) }
    }

    fun stopSpeaking() {
        tts.stop()
        _uiState.update { it.copy(isSpeaking = false) }
    }

    fun clearHistory() {
        apiHistory.clear()
        viewModelScope.launch { memory.clearMemory() }
        _uiState.update { it.copy(messages = emptyList(), statusText = "") }
    }

    private fun runAgent(userInput: String, voice: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusText = "در حال فکر کردن...") }
            runCatching {
                val response = orchestrator.run(
                    userInput = userInput,
                    conversationHistory = apiHistory.toList(),
                    isVoice = voice,
                )
                apiHistory.add(ApiMessage("user", userInput))
                apiHistory.add(ApiMessage("assistant", response))
                addMessage(ChatMessage(role = MessageRole.AGENT, text = response))

                if (voice && response.isNotBlank()) {
                    _uiState.update { it.copy(isLoading = false, statusText = "", isSpeaking = true) }
                    tts.speak(response)
                    _uiState.update { it.copy(isSpeaking = false) }
                    return@launch
                }
            }.onFailure { e ->
                addMessage(
                    ChatMessage(
                        role = MessageRole.AGENT,
                        text = "خطا: ${e.message ?: "مشکل ناشناخته"}"
                    )
                )
            }
            _uiState.update { it.copy(isLoading = false, statusText = "") }
        }
    }

    private fun addMessage(msg: ChatMessage) {
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    override fun onCleared() {
        super.onCleared()
        tts.destroy()
    }
}
