package com.nova.assistant.engine.ai

import com.nova.assistant.BuildConfig
import com.nova.assistant.data.remote.api.OpenAIApi
import com.nova.assistant.data.remote.models.ChatMessage as ApiMessage
import com.nova.assistant.data.remote.models.ChatRequest
import com.nova.assistant.domain.model.Command
import com.nova.assistant.domain.model.CommandType
import com.nova.assistant.domain.model.NovaResponse
import com.nova.assistant.engine.automation.AutomationEngine
import javax.inject.Inject
import javax.inject.Singleton

// ══════════════════════════════════════════════
//  AI ENGINE
// ══════════════════════════════════════════════
@Singleton
class AIEngine @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val contextManager: ContextManager,
    private val commandParser: CommandParser,
    private val automationEngine: AutomationEngine
) {
    private val SYSTEM_PROMPT = """
You are Nova, an advanced AI voice assistant running on Android.
You have full control over the user's phone through an accessibility service.

You can:
- Open, close, and control any app
- Send messages (WhatsApp, SMS, email)
- Control system settings (WiFi, Bluetooth, brightness, volume, flashlight)
- Read notifications
- Set alarms and reminders
- Search the web and answer questions
- Remember context from conversation history
- Execute multi-step automation sequences

When the user asks you to perform a phone action, respond with:
1. A brief acknowledgment ("Opening Instagram now...")
2. A JSON action block (if needed) in this format:
<action>
{
  "type": "APP_LAUNCH|UI_INTERACT|SYSTEM_CONTROL|MESSAGE|ALARM|SEARCH|RECALL",
  "app": "app_name",
  "action": "specific_action",
  "params": { "key": "value" },
  "steps": [ ... ]  // for multi-step sequences
}
</action>

For questions and conversation, respond naturally without action blocks.
Be concise, friendly, and efficient. Address the user by their name if known.
Today's date and time: ${java.util.Date()}
    """.trimIndent()

    suspend fun processCommand(
        userInput: String,
        screenContext: String = ""
    ): Result<NovaResponse> {
        return try {
            // Build message history with context
            val messages = buildMessages(userInput, screenContext)

            // Call GPT-4o
            val request = ChatRequest(
                model = "gpt-4o",
                messages = messages,
                maxTokens = 500,
                temperature = 0.7
            )

            val response = openAIApi.chat(request)
            val content = response.choices.firstOrNull()?.message?.content ?: ""

            // Parse response for action blocks
            val parsed = parseAIResponse(content)

            // Store in context
            contextManager.addMessage("user", userInput)
            contextManager.addMessage("assistant", content)

            // Execute automation if needed
            if (parsed.command != null) {
                automationEngine.execute(parsed.command)
            }

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildMessages(userInput: String, screenContext: String): List<ApiMessage> {
        val messages = mutableListOf<ApiMessage>()

        // System prompt
        val systemWithContext = if (screenContext.isNotBlank()) {
            "$SYSTEM_PROMPT\n\nCurrent screen content:\n$screenContext"
        } else {
            SYSTEM_PROMPT
        }
        messages.add(ApiMessage("system", systemWithContext))

        // Conversation history
        messages.addAll(contextManager.getHistory())

        // Current user message
        messages.add(ApiMessage("user", userInput))

        return messages
    }

    private fun parseAIResponse(content: String): NovaResponse {
        val actionRegex = Regex("""<action>([\s\S]*?)</action>""")
        val match = actionRegex.find(content)

        val textResponse = content.replace(actionRegex, "").trim()
        val suggestions = extractSuggestions(textResponse)

        val command = match?.groupValues?.get(1)?.let { json ->
            commandParser.parseJson(json.trim())
        }

        return NovaResponse(
            text = textResponse,
            command = command,
            suggestions = suggestions
        )
    }

    private fun extractSuggestions(text: String): List<String> {
        // Generate contextual follow-up suggestions based on response
        return listOf(
            "Do something else",
            "Read notifications",
            "What can you do?"
        )
    }
}

// ══════════════════════════════════════════════
//  COMMAND PARSER
// ══════════════════════════════════════════════
@Singleton
class CommandParser @Inject constructor() {

    fun parseJson(json: String): Command? {
        return try {
            val obj = org.json.JSONObject(json)
            val type = CommandType.valueOf(obj.optString("type", "UNKNOWN"))
            Command(
                type = type,
                app = obj.optString("app"),
                action = obj.optString("action"),
                params = parseParams(obj.optJSONObject("params")),
                steps = parseSteps(obj.optJSONArray("steps"))
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseParams(obj: org.json.JSONObject?): Map<String, String> {
        obj ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key ->
            map[key] = obj.optString(key)
        }
        return map
    }

    private fun parseSteps(arr: org.json.JSONArray?): List<Command> {
        arr ?: return emptyList()
        val steps = mutableListOf<Command>()
        for (i in 0 until arr.length()) {
            parseJson(arr.getJSONObject(i).toString())?.let { steps.add(it) }
        }
        return steps
    }
}

// ══════════════════════════════════════════════
//  CONTEXT MANAGER  (Conversation Memory)
// ══════════════════════════════════════════════
@Singleton
class ContextManager @Inject constructor() {

    private val history = mutableListOf<com.nova.assistant.data.remote.models.ChatMessage>()
    private val MAX_HISTORY = 20  // Keep last 20 exchanges

    fun addMessage(role: String, content: String) {
        history.add(com.nova.assistant.data.remote.models.ChatMessage(role, content))
        if (history.size > MAX_HISTORY * 2) {
            history.removeAt(0)
        }
    }

    fun getHistory(): List<com.nova.assistant.data.remote.models.ChatMessage> =
        history.toList()

    fun getRecentHistory(count: Int = 10): List<com.nova.assistant.domain.model.ChatMessage> {
        return history.takeLast(count).map {
            com.nova.assistant.domain.model.ChatMessage(role = it.role, content = it.content)
        }
    }

    fun clear() = history.clear()
}
