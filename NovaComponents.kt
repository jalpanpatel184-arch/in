package com.nova.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.assistant.ui.theme.*
import kotlin.math.*

// ══════════════════════════════════════════════
//  WAVEFORM VISUALIZER
// ══════════════════════════════════════════════
@Composable
fun WaveformVisualizer(
    audioLevels: FloatArray,      // Array of amplitude values 0..1
    modifier: Modifier = Modifier,
    barColor: Color = NovaCyan,
    barCount: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_idle")
    val idleWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing)
        ), label = "idle"
    )

    Canvas(modifier = modifier) {
        val barWidth = (size.width / barCount) * 0.6f
        val gap = (size.width / barCount) * 0.4f
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val level = if (audioLevels.isNotEmpty() && i < audioLevels.size) {
                audioLevels[i].coerceIn(0f, 1f)
            } else {
                // Idle sine wave
                (sin(idleWave + i * 0.4f) * 0.15f + 0.05f).coerceIn(0f, 1f)
            }

            val barHeight = maxBarHeight * level.coerceAtLeast(0.04f)
            val x = i * (barWidth + gap) + barWidth / 2f
            val alpha = 0.4f + level * 0.6f

            drawLine(
                color = barColor.copy(alpha = alpha),
                start = androidx.compose.ui.geometry.Offset(x, size.height / 2f - barHeight / 2f),
                end = androidx.compose.ui.geometry.Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

// ══════════════════════════════════════════════
//  REAL-TIME SUBTITLE OVERLAY
// ══════════════════════════════════════════════
@Composable
fun SubtitleOverlay(
    text: String,
    isVisible: Boolean,
    isNova: Boolean = false,       // true = Nova speaking, false = user speaking
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && text.isNotBlank(),
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(400)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(
                    color = if (isNova) NovaCyanGlow else NovaVioletGlow,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = if (isNova) "◉ NOVA" else "◎ YOU",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isNova) NovaCyan else NovaViolet,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaTextPrimary,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

// ══════════════════════════════════════════════
//  SUGGESTION CHIPS
// ══════════════════════════════════════════════
@Composable
fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(3).forEach { suggestion ->
            androidx.compose.material3.SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                },
                colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                    containerColor = NovaCard,
                    labelColor = NovaTextSecondary
                ),
                border = androidx.compose.material3.SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = NovaCyan.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// ══════════════════════════════════════════════
//  STATUS INDICATOR DOT
// ══════════════════════════════════════════════
@Composable
fun StatusDot(
    isActive: Boolean,
    activeColor: Color = NovaGreen,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot_alpha"
    )
    Box(
        modifier = modifier.size(8.dp)
            .background(
                color = if (isActive) activeColor.copy(alpha = alpha) else NovaTextMuted,
                shape = RoundedCornerShape(50)
            )
    )
}
