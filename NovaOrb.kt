package com.nova.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nova.assistant.ui.theme.*
import kotlin.math.*

// ── Orb State ─────────────────────────────────────────
enum class OrbState {
    IDLE,       // Subtle breathing
    LISTENING,  // Expanding green rings
    THINKING,   // Amber spinning arcs
    SPEAKING,   // Cyan pulsing waves
    ERROR       // Red flash
}

@Composable
fun NovaOrb(
    state: OrbState = OrbState.IDLE,
    audioLevel: Float = 0f,        // 0..1 for waveform reactivity
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    // ── Animations ──────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Breathing pulse (all states)
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breath"
    )

    // Rotation for thinking arcs
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ), label = "rotation"
    )

    // Ring expansion for listening
    val ringRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ring"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    // State-driven colors
    val coreColor = when (state) {
        OrbState.IDLE      -> NovaCyan
        OrbState.LISTENING -> OrbListening
        OrbState.THINKING  -> OrbThinking
        OrbState.SPEAKING  -> OrbSpeaking
        OrbState.ERROR     -> NovaRed
    }

    val glowColor = when (state) {
        OrbState.IDLE      -> NovaCyanGlow
        OrbState.LISTENING -> Color(0x4400FF88)
        OrbState.THINKING  -> Color(0x44FFAA00)
        OrbState.SPEAKING  -> Color(0x44FF6B35)
        OrbState.ERROR     -> Color(0x44FF3366)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.65f

            // ── Outer glow rings ───────────────────
            if (state == OrbState.LISTENING) {
                for (i in 0..2) {
                    val offset = (ringRadius + i * 0.33f) % 1f
                    val ringR = baseRadius * (1f + offset * 0.8f)
                    val alpha = (1f - offset) * 0.4f
                    drawCircle(
                        color = coreColor.copy(alpha = alpha),
                        radius = ringR,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // ── Ambient glow backdrop ──────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowAlpha * 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.0f
                ),
                radius = baseRadius * 2.0f,
                center = center
            )

            // ── Thinking arcs ─────────────────────
            if (state == OrbState.THINKING) {
                rotate(degrees = rotation, pivot = center) {
                    for (i in 0..2) {
                        val startAngle = i * 120f
                        drawArc(
                            color = coreColor.copy(alpha = 0.8f),
                            startAngle = startAngle,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - baseRadius * 1.2f,
                                center.y - baseRadius * 1.2f
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                baseRadius * 2.4f,
                                baseRadius * 2.4f
                            ),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            // ── Core orb gradient ─────────────────
            val scale = if (state == OrbState.SPEAKING) {
                breathScale + audioLevel * 0.15f
            } else {
                breathScale
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        coreColor.copy(alpha = 0.9f),
                        NovaViolet.copy(alpha = 0.7f),
                        NovaBlack.copy(alpha = 0.95f)
                    ),
                    center = Offset(
                        center.x - baseRadius * 0.2f,
                        center.y - baseRadius * 0.3f
                    ),
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * scale,
                center = center
            )

            // ── Specular highlight ─────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(
                        center.x - baseRadius * 0.25f,
                        center.y - baseRadius * 0.3f
                    ),
                    radius = baseRadius * 0.4f
                ),
                radius = baseRadius * 0.38f * scale,
                center = Offset(
                    center.x - baseRadius * 0.25f,
                    center.y - baseRadius * 0.3f
                )
            )

            // ── Audio waveform ring ────────────────
            if (state == OrbState.SPEAKING && audioLevel > 0.01f) {
                drawOrbWaveform(
                    center = center,
                    radius = baseRadius * scale * 1.05f,
                    audioLevel = audioLevel,
                    color = coreColor
                )
            }

            // ── Inner ring border ─────────────────
            drawCircle(
                color = coreColor.copy(alpha = 0.5f),
                radius = baseRadius * scale,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawOrbWaveform(
    center: Offset,
    radius: Float,
    audioLevel: Float,
    color: Color
) {
    val segments = 64
    val points = mutableListOf<Offset>()

    for (i in 0..segments) {
        val angle = (i.toFloat() / segments) * 2f * PI.toFloat()
        val wave = sin(angle * 8f) * audioLevel * radius * 0.12f
        val r = radius + wave
        points.add(
            Offset(
                center.x + r * cos(angle),
                center.y + r * sin(angle)
            )
        )
    }

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
        close()
    }

    drawPath(
        path = path,
        color = color.copy(alpha = 0.6f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}
