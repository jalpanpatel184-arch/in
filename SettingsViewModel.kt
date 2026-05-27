package com.nova.assistant.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NovaSettings(
    val wakeWordEnabled: Boolean   = true,
    val onlineAI: Boolean          = true,
    val offlineSTT: Boolean        = true,
    val neuralTTS: Boolean         = true,
    val accessibilityEnabled: Boolean = false,
    val floatingBubble: Boolean    = false,
    val subtitles: Boolean         = true,
    val localMemoryOnly: Boolean   = false,
    val saveHistory: Boolean       = true,
    val batteryOptimized: Boolean  = true,
    val screenOCR: Boolean         = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _settings = MutableStateFlow(NovaSettings())
    val settings: StateFlow<NovaSettings> = _settings

    fun setWakeWord(v: Boolean)         { _settings.value = _settings.value.copy(wakeWordEnabled = v) }
    fun setOnlineAI(v: Boolean)         { _settings.value = _settings.value.copy(onlineAI = v) }
    fun setOfflineSTT(v: Boolean)       { _settings.value = _settings.value.copy(offlineSTT = v) }
    fun setNeuralTTS(v: Boolean)        { _settings.value = _settings.value.copy(neuralTTS = v) }
    fun setFloatingBubble(v: Boolean)   { _settings.value = _settings.value.copy(floatingBubble = v) }
    fun setSubtitles(v: Boolean)        { _settings.value = _settings.value.copy(subtitles = v) }
    fun setLocalMemory(v: Boolean)      { _settings.value = _settings.value.copy(localMemoryOnly = v) }
    fun setSaveHistory(v: Boolean)      { _settings.value = _settings.value.copy(saveHistory = v) }
    fun setBatteryOptimized(v: Boolean) { _settings.value = _settings.value.copy(batteryOptimized = v) }
    fun setScreenOCR(v: Boolean)        { _settings.value = _settings.value.copy(screenOCR = v) }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Clear Room DB, DataStore, etc.
        }
    }
}
