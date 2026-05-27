package com.nova.assistant.core.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nova.assistant.service.NovaForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val svc = Intent(context, NovaForegroundService::class.java)
            context.startForegroundService(svc)
        }
    }
}
