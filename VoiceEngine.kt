package com.nova.assistant.engine.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── Voice State ───────────────────────────────────────
sealed class VoiceState {
    object Idle : VoiceState()
    object WakeWordDetected : VoiceState()
    data class Listening(val partialText: String = "", val amplitude: Float = 0f) : VoiceState()
    data class Transcribed(val text: String) : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

@Singleton
class VoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whisperEngine: WhisperEngine,
    private val voskEngine: VoskEngine
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isOnline = true
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    init {
        initTTS()
        checkConnectivity()
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.US
                    setSpeechRate(0.95f)
                    setPitch(1.0f)
                    // Use a neural voice if available
                    val voices = voices ?: return@apply
                    val neuralVoice = voices.firstOrNull {
                        it.name.contains("en-us", ignoreCase = true) &&
                        it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) == true
                    }
                    neuralVoice?.let { voice = it }
                }
            }
        }
    }

    // ── Start listening ───────────────────────────────
    fun startListening() {
        if (isRecording) return
        engineScope.launch {
            _state.value = VoiceState.Listening()
            recordAndTranscribe()
        }
    }

    fun stopListening() {
        isRecording = false
        audioRecord?.stop()
        _state.value = VoiceState.Idle
    }

    private suspend fun recordAndTranscribe() {
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val audioData = mutableListOf<Short>()
        val buffer = ShortArray(bufferSize / 2)
        var silenceFrames = 0
        val maxSilenceFrames = 30  // ~1.5 seconds of silence

        audioRecord?.startRecording()
        isRecording = true

        while (isRecording) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
            if (read > 0) {
                audioData.addAll(buffer.take(read))

                // Calculate amplitude for UI
                val rms = buffer.take(read).map { it * it.toLong() }.average()
                val amplitude = (rms / (Short.MAX_VALUE * Short.MAX_VALUE)).toFloat()
                    .coerceIn(0f, 1f)

                _state.value = VoiceState.Listening(amplitude = amplitude)

                // VAD: detect silence
                if (amplitude < 0.01f) silenceFrames++ else silenceFrames = 0
                if (silenceFrames > maxSilenceFrames && audioData.size > sampleRate) {
                    break  // Stop recording after silence
                }
            }
        }

        audioRecord?.stop()
        isRecording = false

        if (audioData.size > sampleRate / 2) {  // At least 0.5s of audio
            transcribe(audioData.toShortArray())
        } else {
            _state.value = VoiceState.Idle
        }
    }

    private suspend fun transcribe(audioData: ShortArray) {
        try {
            val text = if (isOnline) {
                whisperEngine.transcribe(audioData)
            } else {
                voskEngine.transcribe(audioData)
            }

            if (text.isNotBlank()) {
                _state.value = VoiceState.Transcribed(text.trim())
            } else {
                _state.value = VoiceState.Idle
            }
        } catch (e: Exception) {
            // Fallback to Vosk on network error
            try {
                val text = voskEngine.transcribe(audioData)
                if (text.isNotBlank()) {
                    _state.value = VoiceState.Transcribed(text.trim())
                } else {
                    _state.value = VoiceState.Idle
                }
            } catch (e2: Exception) {
                _state.value = VoiceState.Error("Could not transcribe audio: ${e2.message}")
            }
        }
    }

    // ── TTS ───────────────────────────────────────────
    fun speak(text: String) {
        _state.value = VoiceState.Speaking(text)
        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                _state.value = VoiceState.Idle
            }
            override fun onError(utteranceId: String?) {
                _state.value = VoiceState.Idle
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _state.value = VoiceState.Idle
    }

    private fun checkConnectivity() {
        // Simple check — full impl uses ConnectivityManager
        isOnline = true
    }

    fun release() {
        engineScope.cancel()
        audioRecord?.release()
        tts?.shutdown()
    }
}
