package com.nova.assistant.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nova.assistant.ui.components.*
import com.nova.assistant.ui.theme.*
import com.nova.assistant.ui.viewmodel.HomeViewModel
import com.nova.assistant.ui.viewmodel.NovaUiState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToRoutines: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBlack)
    ) {
        // ── Background mesh gradient ──────────────
        BackgroundMesh()

        // ── Main content ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            NovaTopBar(
                isServiceRunning = uiState.isServiceActive,
                onSettingsClick = onNavigateToSettings,
                onMemoryClick = onNavigateToMemory,
                onRoutinesClick = onNavigateToRoutines
            )

            // Conversation history
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
            ) {
                items(
                    items = conversation.reversed(),
                    key = { it.id }
                ) { message ->
                    ChatBubble(
                        message = message,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Suggestions
            SuggestionChips(
                suggestions = suggestions,
                onSuggestionClick = { viewModel.sendTextCommand(it) },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Subtitle overlay
            SubtitleOverlay(
                text = uiState.currentTranscription,
                isVisible = uiState.isListening,
                isNova = false,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SubtitleOverlay(
                text = uiState.currentResponse,
                isVisible = uiState.isSpeaking,
                isNova = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // ── Nova Orb + controls ───────────────
            OrbSection(
                uiState = uiState,
                onOrbTap = { viewModel.toggleListening() },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// ── Background gradient mesh ──────────────────────────
@Composable
fun BackgroundMesh() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NovaDarkSurface,
                        NovaBlack,
                        NovaBlack
                    )
                )
            )
    )
    // Subtle cyan glow top-right
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        NovaCyan.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 800f
                )
            )
    )
}

// ── Top Bar ───────────────────────────────────────────
@Composable
fun NovaTopBar(
    isServiceRunning: Boolean,
    onSettingsClick: () -> Unit,
    onMemoryClick: () -> Unit,
    onRoutinesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nova branding
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "NOVA",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaCyan,
                letterSpacing = 6.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusDot(isActive = isServiceRunning)
                Text(
                    text = if (isServiceRunning) "ACTIVE" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isServiceRunning) NovaGreen else NovaTextMuted
                )
            }
        }

        // Action icons
        IconButton(onClick = onMemoryClick) {
            Icon(Icons.Default.Memory, "Memory", tint = NovaTextSecondary)
        }
        IconButton(onClick = onRoutinesClick) {
            Icon(Icons.Default.AutoAwesome, "Routines", tint = NovaTextSecondary)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, "Settings", tint = NovaTextSecondary)
        }
    }
}

// ── Orb Section ──────────────────────────────────────
@Composable
fun OrbSection(
    uiState: NovaUiState,
    onOrbTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orbState = when {
        uiState.isListening -> OrbState.LISTENING
        uiState.isThinking  -> OrbState.THINKING
        uiState.isSpeaking  -> OrbState.SPEAKING
        uiState.hasError    -> OrbState.ERROR
        else                -> OrbState.IDLE
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Status text
        Text(
            text = when (orbState) {
                OrbState.IDLE      -> "Say  \"Hey Nova\"  to begin"
                OrbState.LISTENING -> "Listening..."
                OrbState.THINKING  -> "Processing..."
                OrbState.SPEAKING  -> "Nova is speaking"
                OrbState.ERROR     -> "Something went wrong"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = NovaTextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tap orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onOrbTap() }
        ) {
            NovaOrb(
                state = orbState,
                audioLevel = uiState.audioLevel,
                size = 200.dp
            )
        }

        Spacer(Modifier.height(20.dp))

        // Waveform strip
        AnimatedVisibility(visible = uiState.isListening || uiState.isSpeaking) {
            WaveformVisualizer(
                audioLevels = uiState.audioLevels,
                barColor = if (uiState.isListening) NovaGreen else NovaCyan,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(40.dp)
            )
        }
    }
}

// ── Chat Bubble ──────────────────────────────────────
@Composable
fun ChatBubble(
    message: com.nova.assistant.domain.model.ChatMessage,
    modifier: Modifier = Modifier
) {
    val isNova = message.role == "assistant"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isNova) Arrangement.Start else Arrangement.End
    ) {
        if (isNova) {
            // Nova icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(NovaCyanGlow, RoundedCornerShape(50))
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text("N", style = MaterialTheme.typography.labelSmall, color = NovaCyan)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalAlignment = if (isNova) Alignment.Start else Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isNova) NovaCard else NovaViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(
                            topStart = if (isNova) 4.dp else 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = if (isNova) 16.dp else 4.dp
                        )
                    )
                    .border(
                        1.dp,
                        if (isNova) NovaCyan.copy(alpha = 0.15f) else NovaViolet.copy(alpha = 0.3f),
                        RoundedCornerShape(
                            topStart = if (isNova) 4.dp else 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = if (isNova) 16.dp else 4.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaTextPrimary
                )
            }
        }
    }
}
