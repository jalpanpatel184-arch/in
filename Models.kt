package com.nova.assistant.domain.model
import java.util.UUID
enum class CommandType { APP_LAUNCH, UI_INTERACT, SYSTEM_CONTROL, MESSAGE, ALARM, SEARCH, NAVIGATION, RECALL, ROUTINE, UNKNOWN }
data class Command(val type: CommandType, val app: String = "", val action: String = "", val params: Map<String,String> = emptyMap(), val steps: List<Command> = emptyList())
data class NovaResponse(val text: String, val command: Command? = null, val suggestions: List<String> = emptyList(), val confidence: Float = 1.0f)
data class ChatMessage(val id: String = UUID.randomUUID().toString(), val role: String, val content: String, val timestamp: Long = System.currentTimeMillis())
data class Memory(val id: String = UUID.randomUUID().toString(), val key: String, val value: String, val category: String = "general", val timestamp: Long = System.currentTimeMillis(), val accessCount: Int = 0)
data class Routine(val id: String = UUID.randomUUID().toString(), val name: String, val triggerPhrase: String, val steps: List<RoutineStep>, val isEnabled: Boolean = true)
data class RoutineStep(val type: CommandType, val action: String, val params: Map<String,String> = emptyMap(), val delayAfterMs: Long = 500)
