package com.nova.assistant.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nova.assistant.MainActivity
import com.nova.assistant.NovaApplication
import com.nova.assistant.R
import com.nova.assistant.engine.voice.VoiceEngine
import com.nova.assistant.engine.voice.WakeWordDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * NovaForegroundService
 * ─────────────────────
 * Always-running foreground service that:
 * • Keeps a partial wake lock so the CPU stays alive
 * • Runs wake-word detection ("Hey Nova")
 * • Coordinates VoiceEngine for STT/TTS
 * • Survives screen-off and app backgrounding
 */
@AndroidEntryPoint
class NovaForegroundService : Service() {

    @Inject lateinit var voiceEngine: VoiceEngine
    @Inject lateinit var wakeWordDetector: WakeWordDetector

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START    = "com.nova.START"
        const val ACTION_STOP     = "com.nova.STOP"
        const val ACTION_LISTEN   = "com.nova.LISTEN"
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification("Listening for \"Hey Nova\""))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP  -> stopSelf()
            ACTION_LISTEN -> voiceEngine.startListening()
            else          -> startWakeWordDetection()
        }
        return START_STICKY   // Restart if killed
    }

    private fun startWakeWordDetection() {
        serviceScope.launch {
            wakeWordDetector.start { detected ->
                if (detected) {
                    updateNotification("Listening...")
                    voiceEngine.startListening()
                }
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Nova::WakeLock"
        ).also { it.acquire(10 * 60 * 1000L /*10 min max*/) }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, NovaForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NovaApplication.CHANNEL_SERVICE)
            .setContentTitle("Nova Assistant")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_nova_notification)  // Add this drawable
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeWordDetector.stop()
        voiceEngine.release()
        wakeLock?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
