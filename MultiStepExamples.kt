package com.nova.assistant.engine.automation

import kotlinx.coroutines.delay
import com.nova.assistant.service.NovaAccessibilityService

/**
 * MultiStepExamples
 * ─────────────────
 * Production-ready multi-step automation sequences.
 * These are the exact flows Nova executes for complex commands.
 */
object MultiStepExamples {

    /**
     * "Open Instagram and send 'hello' to Rahul"
     * ─────────────────────────────────────────
     * Step 1: Open Instagram
     * Step 2: Navigate to DMs
     * Step 3: Search for Rahul
     * Step 4: Open chat
     * Step 5: Type and send message
     */
    suspend fun sendInstagramMessage(
        appLauncher: AppLauncher,
        contact: String,
        message: String
    ): Boolean {
        val svc = NovaAccessibilityService.instance ?: return false

        // 1. Launch Instagram
        appLauncher.launch("instagram")
        delay(2500)

        // 2. Tap the messenger/DM icon
        if (!svc.tapByContentDescription("Direct") &&
            !svc.tapByContentDescription("Messenger") &&
            !svc.tapById("com.instagram.android:id/action_bar_inbox_button")) {
            return false
        }
        delay(1000)

        // 3. Tap search
        svc.tapByContentDescription("Search")
        delay(500)

        // 4. Type contact name
        svc.typeText(contact)
        delay(1500)

        // 5. Tap contact result
        val found = svc.waitForText(contact, 4000)
        if (!found) return false
        svc.tapByText(contact)
        delay(1000)

        // 6. Tap message input
        svc.tapByContentDescription("Message")
            || svc.tapById("com.instagram.android:id/row_thread_composer_edittext")
        delay(400)

        // 7. Type message
        svc.typeText(message)
        delay(300)

        // 8. Send
        svc.tapByContentDescription("Send")
            || svc.tapById("com.instagram.android:id/row_thread_composer_button_send")

        return true
    }

    /**
     * "Open YouTube and search for lo-fi music"
     */
    suspend fun searchYouTube(appLauncher: AppLauncher, query: String): Boolean {
        val svc = NovaAccessibilityService.instance ?: return false
        appLauncher.launch("youtube")
        delay(2000)
        svc.tapByContentDescription("Search") || svc.tapById("com.google.android.youtube:id/menu_item_1")
        delay(600)
        svc.typeText(query)
        delay(300)
        // Simulate Enter key press
        svc.tapByContentDescription("Search")
        return true
    }

    /**
     * "Call Mom on WhatsApp"
     */
    suspend fun whatsappVoiceCall(appLauncher: AppLauncher, contact: String): Boolean {
        val svc = NovaAccessibilityService.instance ?: return false
        appLauncher.launch("whatsapp")
        delay(2000)
        svc.tapByContentDescription("Search")
        delay(500)
        svc.typeText(contact)
        delay(1000)
        svc.tapByText(contact)
        delay(1000)
        svc.tapByContentDescription("Voice call")
            || svc.tapById("com.whatsapp:id/voice_call_btn")
        return true
    }

    /**
     * "Take a screenshot and describe it"
     */
    suspend fun describeScreen(
        ocrEngine: OCREngine,
        aiEngine: com.nova.assistant.engine.ai.AIEngine
    ): String {
        val svc = NovaAccessibilityService.instance ?: return "Accessibility service not active."
        val screenText = svc.extractScreenText()
        val result = aiEngine.processCommand(
            "Describe what's on screen in one sentence. Screen content: $screenText"
        )
        return result.getOrNull()?.text ?: "I couldn't read the screen."
    }

    /**
     * "Open Spotify and play [song/artist]"
     */
    suspend fun playSpotify(appLauncher: AppLauncher, query: String): Boolean {
        val svc = NovaAccessibilityService.instance ?: return false
        appLauncher.launch("spotify")
        delay(2500)
        svc.tapByContentDescription("Search") || svc.tapById("com.spotify.music:id/search_tab")
        delay(600)
        svc.tapByContentDescription("Search Songs, Artists and Podcasts")
            || svc.tapById("com.spotify.music:id/query")
        delay(400)
        svc.typeText(query)
        delay(1500)
        svc.tapByText("Songs")
        delay(500)
        // Tap first result
        svc.tapByContentDescription("Play")
        return true
    }
}
