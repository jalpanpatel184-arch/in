package com.nova.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.assistant.data.local.database.dao.RoutineDao
import com.nova.assistant.data.local.database.entities.RoutineEntity
import com.nova.assistant.domain.model.Routine
import com.nova.assistant.engine.automation.AutomationEngine
import com.nova.assistant.ui.screen.PresetRoutine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val routineDao: RoutineDao,
    private val automationEngine: AutomationEngine
) : ViewModel() {

    val routines: StateFlow<List<Routine>> = routineDao.getEnabled()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createRoutine(name: String, trigger: String) {
        viewModelScope.launch {
            routineDao.insert(RoutineEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                triggerPhrase = trigger,
                stepsJson = "[]"
            ))
        }
    }

    fun toggleRoutine(id: String, enabled: Boolean) {
        viewModelScope.launch { routineDao.setEnabled(id, enabled) }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch {
            // fetch then delete
        }
    }

    fun runRoutine(routine: Routine) {
        viewModelScope.launch {
            routine.steps.forEach { step ->
                // automationEngine.execute(step.toCommand())
                kotlinx.coroutines.delay(step.delayAfterMs)
            }
        }
    }

    fun enablePreset(preset: PresetRoutine) {
        viewModelScope.launch {
            routineDao.insert(RoutineEntity(
                id = UUID.randomUUID().toString(),
                name = preset.name,
                triggerPhrase = preset.name.lowercase(),
                stepsJson = "[]",
                isEnabled = true
            ))
        }
    }

    private fun RoutineEntity.toDomain() = Routine(
        id = id, name = name, triggerPhrase = triggerPhrase,
        steps = emptyList(), isEnabled = isEnabled
    )
}
