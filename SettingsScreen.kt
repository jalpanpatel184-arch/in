package com.nova.assistant.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nova.assistant.ui.theme.*
import com.nova.assistant.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", color = NovaTextPrimary,
                        style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = NovaCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaDarkSurface)
            )
        },
        containerColor = NovaBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { SectionHeader("AI & Voice") }
            item {
                SettingsCard {
                    SettingToggle("Wake Word Detection", "Listen for \"Hey Nova\"",
                        Icons.Default.Hearing, settings.wakeWordEnabled) {
                        viewModel.setWakeWord(it)
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Online AI (GPT-4o)", "Use OpenAI for better responses",
                        Icons.Default.Cloud, settings.onlineAI) {
                        viewModel.setOnlineAI(it)
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Offline Fallback (Vosk)", "Use local STT when offline",
                        Icons.Default.OfflineBolt, settings.offlineSTT) {
                        viewModel.setOfflineSTT(it)
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Neural TTS Voice", "Use high-quality voice synthesis",
                        Icons.Default.RecordVoiceOver, settings.neuralTTS) {
                        viewModel.setNeuralTTS(it)
                    }
                }
            }

            item { SectionHeader("Automation") }
            item {
                SettingsCard {
                    SettingToggle("Accessibility Service", "Required for UI automation",
                        Icons.Default.Accessibility, settings.accessibilityEnabled) {
                        viewModel.openAccessibilitySettings()
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Floating Bubble", "Show overlay button",
                        Icons.Default.BubbleChart, settings.floatingBubble) {
                        viewModel.setFloatingBubble(it)
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Real-time Subtitles", "Show transcription overlay",
                        Icons.Default.ClosedCaption, settings.subtitles) {
                        viewModel.setSubtitles(it)
                    }
                }
            }

            item { SectionHeader("Privacy & Security") }
            item {
                SettingsCard {
                    SettingToggle("Local Memory Only", "Don't send memory to AI",
                        Icons.Default.Lock, settings.localMemoryOnly) {
                        viewModel.setLocalMemory(it)
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Conversation History", "Remember past conversations",
                        Icons.Default.History, settings.saveHistory) {
                        viewModel.setSaveHistory(it)
                    }
                }
            }

            item { SectionHeader("Performance") }
            item {
                SettingsCard {
                    SettingToggle("Battery Optimization", "Reduce background processing",
                        Icons.Default.BatteryChargingFull, settings.batteryOptimized) {
                        viewModel.setBatteryOptimized(it)
                    }
                    Divider(color = NovaTextMuted.copy(alpha = 0.2f))
                    SettingToggle("Screen OCR", "Understand screen content",
                        Icons.Default.Visibility, settings.screenOCR) {
                        viewModel.setScreenOCR(it)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                OutlinedButton(
                    onClick = { viewModel.clearAllData() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NovaRed.copy(alpha = 0.5f))
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All Data & Memory")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = NovaCyan,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NovaDarkSurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(4.dp), content = content)
    }
}

@Composable
fun SettingToggle(
    title: String, subtitle: String, icon: ImageVector,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NovaCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = NovaTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
        }
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NovaBlack,
                checkedTrackColor = NovaCyan
            )
        )
    }
}
