package com.nova.assistant.data.local.database.dao
import androidx.room.*
import com.nova.assistant.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 50") fun getRecent(): Flow<List<ConversationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: ConversationEntity)
    @Query("DELETE FROM conversations") suspend fun clear()
}

@Dao interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY accessCount DESC") fun getAll(): Flow<List<MemoryEntity>>
    @Query("SELECT * FROM memories WHERE key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%'") suspend fun search(query: String): List<MemoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: MemoryEntity)
    @Delete suspend fun delete(item: MemoryEntity)
    @Query("UPDATE memories SET accessCount = accessCount + 1 WHERE id = :id") suspend fun incrementAccess(id: String)
}

@Dao interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isEnabled = 1") fun getEnabled(): Flow<List<RoutineEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: RoutineEntity)
    @Delete suspend fun delete(item: RoutineEntity)
    @Query("UPDATE routines SET isEnabled = :enabled WHERE id = :id") suspend fun setEnabled(id: String, enabled: Boolean)
}
