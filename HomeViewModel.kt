package com.nova.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.assistant.domain.model.ChatMessage
import com.nova.assistant.domain.usecase.ProcessCommandUseCase
import com.nova.assistant.engine.ai.ContextManager
import com.nova.assistant.engine.voice.VoiceEngine
import com.nova.assistant.engine.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────
data class NovaUiState(
    val isServiceActive: Boolean = false,
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val hasError: Boolean = false,
    val currentTranscription: String = "",
    val currentResponse: String = "",
    val audioLevel: Float = 0f,
    val audioLevels: FloatArray = FloatArray(32),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val voiceEngine: VoiceEngine,
    private val processCommandUseCase: ProcessCommandUseCase,
    private val contextManager: ContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NovaUiState())
    val uiState: StateFlow<NovaUiState> = _uiState.asStateFlow()

    private val _conversation = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversation: StateFlow<List<ChatMessage>> = _conversation.asStateFlow()

    private val _suggestions = MutableStateFlow(DEFAULT_SUGGESTIONS)
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    init {
        observeVoiceEngine()
        loadConversationHistory()
    }

    private fun observeVoiceEngine() {
        viewModelScope.launch {
            voiceEngine.state.collect { state ->
                when (state) {
                    is VoiceState.Idle -> _uiState.update {
                        it.copy(isListening = false, isThinking = false, isSpeaking = false,
                            currentTranscription = "", audioLevel = 0f)
                    }
                    is VoiceState.WakeWordDetected -> _uiState.update {
                        it.copy(isListening = true)
                    }
                    is VoiceState.Listening -> _uiState.update {
                        it.copy(isListening = true, audioLevel = state.amplitude,
                            currentTranscription = state.partialText)
                    }
                    is VoiceState.Transcribed -> handleTranscription(state.text)
                    is VoiceState.Speaking -> _uiState.update {
                        it.copy(isSpeaking = true, currentResponse = state.text,
                            isListening = false, isThinking = false)
                    }
                    is VoiceState.Error -> _uiState.update {
                        it.copy(hasError = true, errorMessage = state.message,
                            isListening = false, isThinking = false)
                    }
                }
            }
        }
    }

    private fun handleTranscription(text: String) {
        viewModelScope.launch {
            // Add user message
            addMessage(ChatMessage(role = "user", content = text))

            _uiState.update {
                it.copy(isListening = false, isThinking = true, currentTranscription = "")
            }

            // Process command
            val result = processCommandUseCase(text)
            result.onSuccess { response ->
                addMessage(ChatMessage(role = "assistant", content = response.text))
                updateSuggestions(response.suggestions)
                _uiState.update { it.copy(isThinking = false, hasError = false) }
                voiceEngine.speak(response.text)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isThinking = false, hasError = true, errorMessage = error.message)
                }
            }
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            voiceEngine.stopListening()
        } else {
            voiceEngine.startListening()
        }
    }

    fun sendTextCommand(command: String) {
        viewModelScope.launch {
            handleTranscription(command)
        }
    }

    private fun addMessage(message: ChatMessage) {
        _conversation.update { current ->
            (current + message).takeLast(50)  // Keep last 50 messages
        }
    }

    private fun updateSuggestions(newSuggestions: List<String>) {
        if (newSuggestions.isNotEmpty()) {
            _suggestions.value = newSuggestions
        }
    }

    private fun loadConversationHistory() {
        viewModelScope.launch {
            val history = contextManager.getRecentHistory()
            _conversation.value = history
        }
    }

    companion object {
        private val DEFAULT_SUGGESTIONS = listOf(
            "Open WhatsApp",
            "Set alarm for 7 AM",
            "Read notifications"
        )
    }
}
