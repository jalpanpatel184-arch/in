package com.nova.assistant.data.local.database
import androidx.room.Database
import androidx.room.RoomDatabase
import com.nova.assistant.data.local.database.dao.*
import com.nova.assistant.data.local.database.entities.*

@Database(
    entities = [ConversationEntity::class, MemoryEntity::class, RoutineEntity::class],
    version = 1, exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun routineDao(): RoutineDao
}
