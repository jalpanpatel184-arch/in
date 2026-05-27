package com.nova.assistant.engine.voice

import com.nova.assistant.data.remote.api.WhisperApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

// ══════════════════════════════════════════════
//  WHISPER ENGINE  (Online STT via OpenAI API)
// ══════════════════════════════════════════════
@Singleton
class WhisperEngine @Inject constructor(
    private val whisperApi: WhisperApi
) {
    /**
     * Transcribes PCM 16-bit mono 16kHz audio data
     * by uploading it to OpenAI Whisper API as a WAV file.
     */
    suspend fun transcribe(audioData: ShortArray): String {
        val wavBytes = pcmToWav(audioData, sampleRate = 16000)
        val requestBody = wavBytes.toRequestBody("audio/wav".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "audio.wav", requestBody)
        val modelPart = "whisper-1".toRequestBody("text/plain".toMediaType())

        val response = whisperApi.transcribe(
            file = part,
            model = modelPart
        )
        return response.text ?: ""
    }

    /** Convert raw PCM shorts to WAV byte array */
    private fun pcmToWav(pcm: ShortArray, sampleRate: Int): ByteArray {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = pcm.size * 2
        val headerSize = 44

        val out = ByteArrayOutputStream(headerSize + dataSize)
        val header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)          // PCM chunk size
        header.putShort(1)         // Audio format: PCM
        header.putShort(numChannels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)

        out.write(header.array())

        val dataBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { dataBuffer.putShort(it) }
        out.write(dataBuffer.array())

        return out.toByteArray()
    }
}

// ══════════════════════════════════════════════
//  VOSK ENGINE  (Offline STT)
// ══════════════════════════════════════════════
@Singleton
class VoskEngine @Inject constructor() {

    private var isInitialized = false

    /**
     * Offline speech recognition using Vosk.
     *
     * Integration steps:
     * 1. Add vosk-android-demo AAR to libs/
     * 2. Download Vosk model (vosk-model-small-en-us-0.15) from alphacephei.com
     * 3. Extract to assets/vosk-model/
     *
     * Full Vosk implementation:
     *
     * private var model: Model? = null
     * private var recognizer: KaldiRecognizer? = null
     *
     * fun init(context: Context) {
     *     StorageService.unpack(context, "vosk-model", "model", { model ->
     *         this.model = model
     *         recognizer = KaldiRecognizer(model, 16000.0f)
     *         isInitialized = true
     *     }, { exception -> })
     * }
     *
     * fun transcribe(audioData: ShortArray): String {
     *     if (!isInitialized) return ""
     *     recognizer?.acceptWaveForm(audioData, audioData.size)
     *     val result = JSONObject(recognizer?.finalResult ?: "{}")
     *     return result.optString("text", "")
     * }
     */
    suspend fun transcribe(audioData: ShortArray): String {
        // Placeholder — replace with actual Vosk KaldiRecognizer call
        // See integration steps above
        return ""
    }
}
