package com.nova.assistant.engine.automation

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCREngine
 * ─────────
 * Uses ML Kit on-device text recognition to extract text from screenshots.
 * Combined with NovaAccessibilityService for full screen understanding.
 *
 * Usage:
 *   val text = ocrEngine.extractText(bitmap)
 *   val intent = aiEngine.processCommand("What does my screen say? $text")
 */
@Singleton
class OCREngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract all text from a Bitmap (e.g. screenshot).
     */
    suspend fun extractText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.textBlocks
                        .joinToString("\n") { block ->
                            block.lines.joinToString(" ") { it.text }
                        }
                    cont.resume(text)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    /**
     * Extract structured text blocks with their bounding boxes.
     * Useful for finding clickable regions by text.
     */
    suspend fun extractTextBlocks(bitmap: Bitmap): List<TextBlock> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val blocks = result.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            confidence = block.lines.firstOrNull()?.confidence ?: 0f
                        )
                    }
                    cont.resume(blocks)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    fun release() = recognizer.close()
}

data class TextBlock(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val confidence: Float
)
