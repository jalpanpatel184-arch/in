package com.nova.assistant.service
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NovaNotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
        val notifications: StateFlow<List<NotificationInfo>> = _notifications
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text  = extras.getCharSequence("android.text")?.toString() ?: ""
        val app   = sbn.packageName

        val info = NotificationInfo(app = app, title = title, text = text, time = sbn.postTime)
        _notifications.value = (_notifications.value + info).takeLast(50)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    fun readAllAloud(): String {
        val notifs = _notifications.value.takeLast(5)
        if (notifs.isEmpty()) return "You have no recent notifications."
        return notifs.joinToString(". ") { "${it.title}: ${it.text}" }
    }
}

data class NotificationInfo(val app: String, val title: String, val text: String, val time: Long)
