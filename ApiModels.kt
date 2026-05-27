package com.nova.assistant.data.remote.models
import com.google.gson.annotations.SerializedName

data class ChatMessage(val role: String, val content: String)
data class ChatRequest(val model: String, val messages: List<ChatMessage>, @SerializedName("max_tokens") val maxTokens: Int = 1000, val temperature: Double = 0.7)
data class ChatResponse(val choices: List<Choice> = emptyList())
data class Choice(val message: ChatMessage, @SerializedName("finish_reason") val finishReason: String = "")
data class WhisperResponse(val text: String? = null)
