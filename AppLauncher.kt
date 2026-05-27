package com.nova.assistant.engine.automation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ══════════════════════════════════════════════
//  APP LAUNCHER
// ══════════════════════════════════════════════
@Singleton
class AppLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager

    // Common app name → package name mappings
    private val appMap = mapOf(
        "whatsapp"    to "com.whatsapp",
        "instagram"   to "com.instagram.android",
        "twitter"     to "com.twitter.android",
        "x"           to "com.twitter.android",
        "facebook"    to "com.facebook.katana",
        "youtube"     to "com.google.android.youtube",
        "spotify"     to "com.spotify.music",
        "gmail"       to "com.google.android.gm",
        "maps"        to "com.google.android.apps.maps",
        "chrome"      to "com.android.chrome",
        "camera"      to "com.android.camera2",
        "settings"    to "com.android.settings",
        "calculator"  to "com.google.android.calculator",
        "clock"       to "com.google.android.deskclock",
        "calendar"    to "com.google.android.calendar",
        "contacts"    to "com.google.android.contacts",
        "phone"       to "com.google.android.dialer",
        "messages"    to "com.google.android.apps.messaging",
        "photos"      to "com.google.android.apps.photos",
        "drive"       to "com.google.android.apps.docs",
        "netflix"     to "com.netflix.mediaclient",
        "amazon"      to "com.amazon.mShop.android.shopping",
        "uber"        to "com.ubercab",
        "linkedin"    to "com.linkedin.android",
        "snapchat"    to "com.snapchat.android",
        "telegram"    to "org.telegram.messenger",
        "discord"     to "com.discord",
        "tiktok"      to "com.zhiliaoapp.musically",
        "reddit"      to "com.reddit.frontpage",
        "amazon music" to "com.amazon.mp3",
        "play store"  to "com.android.vending"
    )

    suspend fun launch(appName: String): Boolean {
        val packageName = resolvePackage(appName) ?: return false
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: return false
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun resolvePackage(appName: String): String? {
        val normalized = appName.lowercase().trim()

        // Direct map lookup
        appMap[normalized]?.let { return it }

        // Fuzzy search in installed apps
        val apps = packageManager.getInstalledApplications(0)
        for (app in apps) {
            val label = packageManager.getApplicationLabel(app).toString().lowercase()
            if (label.contains(normalized) || normalized.contains(label)) {
                return app.packageName
            }
        }
        return null
    }

    fun isInstalled(appName: String): Boolean {
        val pkg = resolvePackage(appName) ?: return false
        return try {
            packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getInstalledApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, 0).map { ri ->
            val label = ri.loadLabel(packageManager).toString()
            val pkg = ri.activityInfo.packageName
            label to pkg
        }.sortedBy { it.first }
    }
}

// ══════════════════════════════════════════════
//  SYSTEM CONTROLLER
// ══════════════════════════════════════════════
@Singleton
class SystemController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    // ── WiFi ──────────────────────────────────
    fun setWifi(enabled: Boolean): Boolean {
        // Android 10+: WiFi can only be toggled via Settings panel
        val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    // ── Bluetooth ─────────────────────────────
    fun setBluetooth(enabled: Boolean): Boolean {
        val bm = context.getSystemService(BluetoothManager::class.java)
        val adapter = bm?.adapter ?: return false
        return if (enabled) {
            // Android 12+ requires user consent via Intent
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } else {
            val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        }
    }

    // ── Flashlight ────────────────────────────
    private var flashlightOn = false
    fun setFlashlight(enabled: Boolean): Boolean {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enabled)
            flashlightOn = enabled
            true
        } catch (e: Exception) { false }
    }

    fun toggleFlashlight() = setFlashlight(!flashlightOn)

    // ── Brightness ────────────────────────────
    fun setBrightness(percent: Int): Boolean {
        return try {
            val value = (percent / 100f * 255).toInt().coerceIn(0, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value
            )
            true
        } catch (e: Exception) { false }
    }

    // ── Volume ────────────────────────────────
    fun setVolume(percent: Int, stream: String = "music"): Boolean {
        return try {
            val streamType = when (stream.lowercase()) {
                "music"   -> AudioManager.STREAM_MUSIC
                "ring"    -> AudioManager.STREAM_RING
                "alarm"   -> AudioManager.STREAM_ALARM
                "notification" -> AudioManager.STREAM_NOTIFICATION
                else      -> AudioManager.STREAM_MUSIC
            }
            val max = audioManager.getStreamMaxVolume(streamType)
            val target = (percent / 100f * max).toInt()
            audioManager.setStreamVolume(streamType, target, 0)
            true
        } catch (e: Exception) { false }
    }

    fun adjustVolume(increase: Boolean): Boolean {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI
        )
        return true
    }

    fun mute(): Boolean {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0
        )
        return true
    }

    // ── Do Not Disturb ────────────────────────
    fun setDoNotDisturb(enabled: Boolean): Boolean {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    // ── Media Controls ────────────────────────
    fun mediaPlay()     = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    fun mediaPause()    = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    fun mediaNext()     = sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun mediaPrevious() = sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    fun mediaPlayPause()= sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

    private fun sendMediaKey(keyCode: Int): Boolean {
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        )
        audioManager.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, keyCode)
        )
        return true
    }
}

// ══════════════════════════════════════════════
//  UI INTERACTOR  (wraps AccessibilityService)
// ══════════════════════════════════════════════
@Singleton
class UIInteractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val svc get() = com.nova.assistant.service.NovaAccessibilityService.instance

    fun tap(text: String)         = svc?.tapByText(text) ?: false
    fun tapId(resourceId: String) = svc?.tapById(resourceId) ?: false
    fun tapDesc(desc: String)     = svc?.tapByContentDescription(desc) ?: false
    fun tapAt(x: Float, y: Float) = svc?.tapAt(x, y) ?: false
    fun type(text: String)        = svc?.typeText(text) ?: false
    fun scrollDown()              = svc?.scrollDown() ?: false
    fun scrollUp()                = svc?.scrollUp() ?: false
    fun back()                    = svc?.pressBack()
    fun home()                    = svc?.pressHome()
    fun getScreenText()           = svc?.extractScreenText() ?: ""

    suspend fun waitForText(text: String, timeoutMs: Long = 4000) =
        svc?.waitForText(text, timeoutMs) ?: false

    suspend fun waitForPackage(pkg: String, timeoutMs: Long = 4000) =
        svc?.waitForPackage(pkg, timeoutMs) ?: false
}
