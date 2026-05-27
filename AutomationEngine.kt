package com.nova.assistant.engine.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nova.assistant.domain.model.Command
import com.nova.assistant.domain.model.CommandType
import com.nova.assistant.service.NovaAccessibilityService
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

// ══════════════════════════════════════════════
//  AUTOMATION ENGINE
// ══════════════════════════════════════════════
@Singleton
class AutomationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLauncher: AppLauncher,
    private val systemController: SystemController,
    private val uiInteractor: UIInteractor
) {
    suspend fun execute(command: Command): Boolean {
        // Execute multi-step sequences
        if (command.steps.isNotEmpty()) {
            for (step in command.steps) {
                val success = executeSingle(step)
                if (!success) return false
                delay(800)  // Allow UI to settle between steps
            }
            return true
        }
        return executeSingle(command)
    }

    private suspend fun executeSingle(command: Command): Boolean {
        return when (command.type) {
            CommandType.APP_LAUNCH     -> handleAppLaunch(command)
            CommandType.UI_INTERACT    -> handleUIInteract(command)
            CommandType.SYSTEM_CONTROL -> handleSystemControl(command)
            CommandType.MESSAGE        -> handleMessage(command)
            CommandType.ALARM          -> handleAlarm(command)
            CommandType.SEARCH         -> handleSearch(command)
            CommandType.NAVIGATION     -> handleNavigation(command)
            else -> false
        }
    }

    // ── App Launch ────────────────────────────────────
    private suspend fun handleAppLaunch(cmd: Command): Boolean {
        return appLauncher.launch(cmd.app)
    }

    // ── UI Interaction ────────────────────────────────
    private suspend fun handleUIInteract(cmd: Command): Boolean {
        val svc = NovaAccessibilityService.instance ?: return false
        return when (cmd.action) {
            "tap"      -> cmd.params["text"]?.let { svc.tapByText(it) } ?: false
            "type"     -> cmd.params["text"]?.let { svc.typeText(it) } ?: false
            "scroll_down" -> svc.scrollDown()
            "scroll_up"   -> svc.scrollUp()
            "back"        -> svc.pressBack().let { true }
            "home"        -> svc.pressHome().let { true }
            "tap_id"   -> cmd.params["id"]?.let { svc.tapById(it) } ?: false
            else -> false
        }
    }

    // ── System Controls ───────────────────────────────
    private suspend fun handleSystemControl(cmd: Command): Boolean {
        return when (cmd.action) {
            "wifi_on"         -> systemController.setWifi(true)
            "wifi_off"        -> systemController.setWifi(false)
            "bluetooth_on"    -> systemController.setBluetooth(true)
            "bluetooth_off"   -> systemController.setBluetooth(false)
            "flashlight_on"   -> systemController.setFlashlight(true)
            "flashlight_off"  -> systemController.setFlashlight(false)
            "brightness"      -> {
                val level = cmd.params["level"]?.toIntOrNull() ?: 50
                systemController.setBrightness(level)
            }
            "volume"          -> {
                val level = cmd.params["level"]?.toIntOrNull() ?: 50
                val stream = cmd.params["stream"] ?: "music"
                systemController.setVolume(level, stream)
            }
            "volume_up"       -> systemController.adjustVolume(true)
            "volume_down"     -> systemController.adjustVolume(false)
            "mute"            -> systemController.mute()
            "do_not_disturb"  -> systemController.setDoNotDisturb(true)
            "ringer_normal"   -> systemController.setDoNotDisturb(false)
            "play_media"      -> systemController.mediaPlay()
            "pause_media"     -> systemController.mediaPause()
            "next_track"      -> systemController.mediaNext()
            "prev_track"      -> systemController.mediaPrevious()
            else -> false
        }
    }

    // ── Messaging ─────────────────────────────────────
    private suspend fun handleMessage(cmd: Command): Boolean {
        val to = cmd.params["to"] ?: return false
        val message = cmd.params["message"] ?: return false

        return when (cmd.app.lowercase()) {
            "whatsapp" -> sendWhatsApp(to, message)
            "sms", "messages" -> sendSMS(to, message)
            else -> sendWhatsApp(to, message)  // Default to WhatsApp
        }
    }

    private suspend fun sendWhatsApp(contact: String, message: String): Boolean {
        val svc = NovaAccessibilityService.instance ?: return false

        // 1. Open WhatsApp
        appLauncher.launch("whatsapp")
        delay(1500)

        // 2. Tap search/new chat
        svc.tapByContentDescription("New chat")
        delay(800)

        // 3. Search for contact
        svc.typeText(contact)
        delay(1000)

        // 4. Wait for and tap contact
        val found = svc.waitForText(contact, timeoutMs = 3000)
        if (!found) return false
        svc.tapByText(contact)
        delay(800)

        // 5. Type and send message
        svc.tapByContentDescription("Message")
        delay(300)
        svc.typeText(message)
        delay(300)
        svc.tapByContentDescription("Send")
        return true
    }

    private fun sendSMS(to: String, message: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$to")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    // ── Alarms ────────────────────────────────────────
    private fun handleAlarm(cmd: Command): Boolean {
        val hour = cmd.params["hour"]?.toIntOrNull() ?: return false
        val minute = cmd.params["minute"]?.toIntOrNull() ?: 0
        val message = cmd.params["message"] ?: "Nova Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    // ── Web Search ────────────────────────────────────
    private fun handleSearch(cmd: Command): Boolean {
        val query = cmd.params["query"] ?: return false
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    // ── Navigation ────────────────────────────────────
    private fun handleNavigation(cmd: Command): Boolean {
        val destination = cmd.params["destination"] ?: return false
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }
}
