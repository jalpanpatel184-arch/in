package com.nova.assistant.engine.routine

import com.nova.assistant.data.local.database.dao.RoutineDao
import com.nova.assistant.domain.model.*
import com.nova.assistant.engine.automation.AutomationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineEngine @Inject constructor(
    private val routineDao: RoutineDao,
    private val automationEngine: AutomationEngine
) {
    suspend fun findMatchingRoutine(command: String): Routine? {
        val lower = command.lowercase()
        return routineDao.getEnabled().first()
            .map { e -> Routine(id = e.id, name = e.name, triggerPhrase = e.triggerPhrase, steps = emptyList(), isEnabled = e.isEnabled) }
            .firstOrNull { lower.contains(it.triggerPhrase.lowercase()) }
    }

    suspend fun executeRoutine(routine: Routine): Boolean {
        routine.steps.forEach { step ->
            automationEngine.execute(Command(type = step.type, action = step.action, params = step.params))
            delay(step.delayAfterMs)
        }
        return true
    }
}
