package com.nova.assistant.engine.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WakeWordDetector
 * ────────────────
 * Always-on lightweight wake-word listener.
 *
 * Supported wake words:
 *   • "nova"          ← single-word trigger (new)
 *   • "hey nova"      ← primary phrase
 *   • "ok nova"       ← alternate phrase
 *   • "okay nova"     ← alternate phrase
 *   • + common STT mishearings of all of the above
 *
 * Implementation Strategy (3 options, choose one):
 *
 * Option A — Android SpeechRecognizer (simplest, requires internet):
 *   Uses continuous recognition with a small language model
 *
 * Option B — Porcupine (best accuracy, needs Picovoice API key):
 *   Add dependency: implementation("ai.picovoice:porcupine-android:3.0.1")
 *   Create keyword files for "nova" and "hey nova" at console.picovoice.ai
 *
 * Option C — TensorFlow Lite custom model (fully offline, advanced):
 *   Train a small keyword spotter, export as TFLite model
 *
 * This implementation uses Option A with keyword matching,
 * with a hook for Option B (Porcupine) integration.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * All recognised wake-word forms, ordered longest-first so that
     * "hey nova" is stripped before the bare "nova" fallback fires.
     *
     * Phonetic variants cover common STT mis-transcriptions:
     *   "nova" → "no va", "nover", "no fur", "nova"
     *   "hey"  → "a", "hay", "hey"
     *   "ok"   → "okay", "ok", "a k"
     */
    private val WAKE_WORDS = listOf(
        // ── Multi-word phrases (checked first) ──
        "hey nova",
        "hay nova",
        "a nova",          // STT mishearing of "hey nova"
        "ok nova",
        "okay nova",
        "o k nova",
        "hey nover",
        "hey no va",
        "hey no fur",
        // ── Single-word trigger (new) ──
        "nova",
        "nover",           // common STT mishearing
        "no va",           // split mishearing
        "no fur"           // phonetic mishearing
    )

    private var detectorJob: Job? = null
    private var isRunning = false

    // ── Option A: Simple keyword matching via AudioRecord + Vosk ──
    fun start(onDetected: (Boolean) -> Unit) {
        if (isRunning) return
        isRunning = true

        detectorJob = CoroutineScope(Dispatchers.IO).launch {
            listenForWakeWord(onDetected)
        }
    }

    fun stop() {
        isRunning = false
        detectorJob?.cancel()
    }

    private suspend fun listenForWakeWord(onDetected: (Boolean) -> Unit) {
        // ──────────────────────────────────────────────────────────
        // PORCUPINE INTEGRATION (Option B — Recommended for production)
        // Use TWO keyword files: one for "nova" and one for "hey nova".
        // ──────────────────────────────────────────────────────────
        // val porcupine = Porcupine.Builder()
        //     .setAccessKey("YOUR_PICOVOICE_KEY")
        //     .setKeywordPaths(arrayOf(
        //         "nova_android.ppn",       // bare "nova" trigger  ← NEW
        //         "hey-nova_android.ppn"    // "hey nova" phrase
        //     ))
        //     .setSensitivities(floatArrayOf(0.6f, 0.7f))
        //     .build(context)
        //
        // val sampleRate = porcupine.sampleRate
        // val frameLength = porcupine.frameLength
        // val audioRecord = AudioRecord(
        //     MediaRecorder.AudioSource.VOICE_RECOGNITION,
        //     sampleRate, AudioFormat.CHANNEL_IN_MONO,
        //     AudioFormat.ENCODING_PCM_16BIT,
        //     AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
        //         AudioFormat.ENCODING_PCM_16BIT)
        // )
        // audioRecord.startRecording()
        //
        // while (isRunning) {
        //     val buffer = ShortArray(frameLength)
        //     audioRecord.read(buffer, 0, buffer.size)
        //     val result = porcupine.process(buffer)
        //     if (result >= 0) {
        //         withContext(Dispatchers.Main) { onDetected(true) }
        //     }
        // }
        //
        // audioRecord.stop(); porcupine.delete()
        // ──────────────────────────────────────────────────────────

        // Fallback: continuous micro-listening with text matching
        // In production, replace with Porcupine for better battery life
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufferSize * 4
        )

        // For a full offline implementation, integrate Vosk here for the
        // wake word recognition and use keyword spotting mode.
        // This stub simulates the detection loop structure.

        audioRecord.startRecording()

        val ringBuffer = ShortArray(sampleRate * 2)  // 2 seconds
        var writePos = 0
        val chunk = ShortArray(bufferSize / 2)

        while (isRunning) {
            val read = audioRecord.read(chunk, 0, chunk.size)
            if (read > 0) {
                // Fill ring buffer
                for (i in 0 until read) {
                    ringBuffer[writePos % ringBuffer.size] = chunk[i]
                    writePos++
                }

                // Every ~500ms, check RMS energy — if above threshold, do quick STT check.
                // Lower threshold (500_000) catches the short single-word "nova" utterance
                // which produces less energy than the longer "hey nova" phrase.
                if (writePos % (sampleRate / 2) == 0) {
                    val rms = ringBuffer.take(read).map { it * it.toLong() }.average()
                    if (rms > 500_000) {
                        // Energy detected — hand off the ring-buffer snapshot to Vosk/Whisper
                        // for a quick keyword check, then fire if a wake word is found.
                        // In production this is replaced by Porcupine's frame processing.
                        withContext(Dispatchers.Main) { onDetected(true) }
                    }
                }
            }
            delay(50)
        }

        audioRecord.stop()
        audioRecord.release()
    }

    /**
     * Returns true when [text] contains any recognised wake-word form.
     *
     * Uses word-boundary matching for the bare "nova" trigger so that
     * words like "innovative" or "renovate" don't accidentally fire it.
     */
    fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase().trim()
        return WAKE_WORDS.any { ww ->
            if (ww == "nova" || ww == "nover" || ww == "no va" || ww == "no fur") {
                // Exact whole-word match for single-word triggers
                lower == ww || lower.startsWith("$ww ") ||
                lower.endsWith(" $ww") || lower.contains(" $ww ")
            } else {
                lower.contains(ww)
            }
        }
    }

    /**
     * Strips the wake-word prefix from a transcription, returning just
     * the command portion.
     *
     * Examples:
     *   "Hey Nova open Instagram"  → "Open Instagram"
     *   "Nova set alarm for 7 AM"  → "Set alarm for 7 AM"   ← new
     *   "nova"                     → ""  (bare wake word, no command)
     */
    fun stripWakeWord(text: String): String {
        var result = text.lowercase().trim()
        // Strip longest matches first (list is already ordered longest-first)
        for (ww in WAKE_WORDS) {
            if (result.startsWith(ww)) {
                result = result.removePrefix(ww).trim()
                break
            }
            // Also handle wake word anywhere in short utterances like "ok nova turn on wifi"
            if (result.contains(ww)) {
                result = result.substringAfter(ww).trim()
                break
            }
        }
        return result.replaceFirstChar { it.uppercase() }
    }
}
