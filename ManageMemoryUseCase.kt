package com.nova.assistant.domain.usecase

import com.nova.assistant.data.local.database.dao.MemoryDao
import com.nova.assistant.data.local.database.entities.MemoryEntity
import com.nova.assistant.domain.model.Memory
import java.util.UUID
import javax.inject.Inject

class ManageMemoryUseCase @Inject constructor(
    private val memoryDao: MemoryDao
) {
    /** Save a key-value memory. */
    suspend fun remember(key: String, value: String, category: String = "general") {
        memoryDao.insert(MemoryEntity(
            id = UUID.randomUUID().toString(),
            key = key, value = value, category = category
        ))
    }

    /** Recall a memory by keyword search. */
    suspend fun recall(query: String): List<Memory> {
        return memoryDao.search(query).map { e ->
            Memory(id = e.id, key = e.key, value = e.value,
                category = e.category, timestamp = e.timestamp, accessCount = e.accessCount)
                .also { memoryDao.incrementAccess(e.id) }
        }
    }

    /** Format recall results as a spoken response. */
    suspend fun recallAsText(query: String): String {
        val results = recall(query)
        return if (results.isEmpty()) {
            "I don't have any memory about \"$query\"."
        } else {
            results.joinToString(". ") { "${it.key}: ${it.value}" }
        }
    }
}
