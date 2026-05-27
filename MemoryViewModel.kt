package com.nova.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.assistant.data.local.database.dao.MemoryDao
import com.nova.assistant.data.local.database.entities.MemoryEntity
import com.nova.assistant.domain.model.Memory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryDao: MemoryDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val memories: StateFlow<List<Memory>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                memoryDao.getAll().map { list -> list.map { it.toDomain() } }
            } else {
                flow { emit(memoryDao.search(query).map { it.toDomain() }) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String) { _searchQuery.value = query }

    fun addMemory(key: String, value: String, category: String) {
        viewModelScope.launch {
            memoryDao.insert(MemoryEntity(id = UUID.randomUUID().toString(),
                key = key, value = value, category = category))
        }
    }

    fun deleteMemory(memory: Memory) {
        viewModelScope.launch {
            memoryDao.delete(MemoryEntity(id = memory.id, key = memory.key,
                value = memory.value, category = memory.category, timestamp = memory.timestamp,
                accessCount = memory.accessCount))
        }
    }

    private fun MemoryEntity.toDomain() = Memory(id = id, key = key, value = value,
        category = category, timestamp = timestamp, accessCount = accessCount)
}
