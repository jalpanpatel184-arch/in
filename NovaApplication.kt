package com.nova.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NovaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)

        // Foreground service channel
        NotificationChannel(
            CHANNEL_SERVICE,
            "Nova Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Nova is listening in the background"
            setShowBadge(false)
        }.also { nm.createNotificationChannel(it) }

        // Alert channel for Nova responses
        NotificationChannel(
            CHANNEL_ALERTS,
            "Nova Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important notifications from Nova"
        }.also { nm.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_SERVICE = "nova_service"
        const val CHANNEL_ALERTS = "nova_alerts"
    }
}
