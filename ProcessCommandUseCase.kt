package com.nova.assistant.domain.usecase
import com.nova.assistant.domain.model.NovaResponse
import com.nova.assistant.engine.ai.AIEngine
import com.nova.assistant.service.NovaAccessibilityService
import javax.inject.Inject

class ProcessCommandUseCase @Inject constructor(
    private val aiEngine: AIEngine
) {
    suspend operator fun invoke(userInput: String): Result<NovaResponse> {
        val screenContext = NovaAccessibilityService.instance?.extractScreenText() ?: ""
        return aiEngine.processCommand(userInput, screenContext)
    }
}
