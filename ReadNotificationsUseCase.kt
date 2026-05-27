package com.nova.assistant.domain.usecase

import com.nova.assistant.service.NovaNotificationListener
import javax.inject.Inject

class ReadNotificationsUseCase @Inject constructor() {
    operator fun invoke(count: Int = 5): String {
        val notifs = NovaNotificationListener.notifications.value.takeLast(count)
        return if (notifs.isEmpty()) {
            "You have no recent notifications."
        } else {
            "You have ${notifs.size} notifications. " +
            notifs.joinToString(". ") { "${it.title}: ${it.text}" }
        }
    }
}
