package com.nova.assistant.data.remote.api
import com.nova.assistant.data.remote.models.ChatRequest
import com.nova.assistant.data.remote.models.ChatResponse
import retrofit2.http.*

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
