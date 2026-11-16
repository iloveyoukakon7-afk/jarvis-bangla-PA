package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "memories")
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bossText: String,
    val chhotuReply: String,
    val category: String,
    val moodTag: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val isPinnedForever: Boolean = false
)

@Entity(tableName = "learned_habits")
data class LearnedHabitEntity(
    @PrimaryKey val habitKey: String,
    val descriptionBangla: String,
    val frequencyCount: Int = 1,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "forbidden_actions")
data class ForbiddenActionEntity(
    @PrimaryKey val actionKey: String,
    val reasonBangla: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerDisplay: String,
    val channelLabel: String,
    val filePath: String,
    val durationSec: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val keepForever: Boolean = false
)

@Dao
interface ChhotuMemoryDao {
    @Query("SELECT * FROM memories ORDER BY isPinnedForever DESC, createdAt DESC")
    fun observeMemories(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT 20")
    suspend fun getRecentMemories(): List<MemoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(entry: MemoryEntryEntity)

    @Query("UPDATE memories SET isPinnedForever = 1 WHERE id = :id")
    suspend fun pinMemory(id: Long)

    @Query("UPDATE memories SET isPinnedForever = 1 WHERE id = (SELECT id FROM memories ORDER BY createdAt DESC LIMIT 1)")
    suspend fun pinLatestMemory()

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memories WHERE isPinnedForever = 0")
    suspend fun deleteAllUnpinned()

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()

    @Query("DELETE FROM memories WHERE isPinnedForever = 0 AND expiresAt <= :nowMillis")
    suspend fun purgeExpired(nowMillis: Long)

    @Query("SELECT * FROM learned_habits ORDER BY frequencyCount DESC, lastUpdated DESC")
    fun observeHabits(): Flow<List<LearnedHabitEntity>>

    @Query("SELECT * FROM learned_habits ORDER BY frequencyCount DESC LIMIT 12")
    suspend fun getTopHabits(): List<LearnedHabitEntity>

    @Query("SELECT * FROM learned_habits WHERE habitKey = :key LIMIT 1")
    suspend fun findHabit(key: String): LearnedHabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: LearnedHabitEntity)

    @Query("SELECT * FROM forbidden_actions ORDER BY createdAt DESC")
    fun observeForbiddenActions(): Flow<List<ForbiddenActionEntity>>

    @Query("SELECT * FROM forbidden_actions")
    suspend fun getAllForbiddenActions(): List<ForbiddenActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForbidden(action: ForbiddenActionEntity)

    @Query("DELETE FROM forbidden_actions WHERE actionKey = :key")
    suspend fun removeForbidden(key: String)

    @Query("SELECT * FROM call_recordings ORDER BY createdAt DESC")
    fun observeRecordings(): Flow<List<CallRecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecordingEntity): Long

    @Query("UPDATE call_recordings SET keepForever = 1 WHERE id = :id")
    suspend fun keepRecording(id: Long)

    @Query("UPDATE call_recordings SET keepForever = 1 WHERE id = (SELECT id FROM call_recordings ORDER BY createdAt DESC LIMIT 1)")
    suspend fun keepLatestRecording()

    @Query("SELECT * FROM call_recordings WHERE keepForever = 0")
    suspend fun getTransientRecordings(): List<CallRecordingEntity>

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)
}

@Database(
    entities = [
        MemoryEntryEntity::class,
        LearnedHabitEntity::class,
        ForbiddenActionEntity::class,
        CallRecordingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChhotuDatabase : RoomDatabase() {
    abstract fun memoryDao(): ChhotuMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: ChhotuDatabase? = null

        fun getInstance(context: Context): ChhotuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChhotuDatabase::class.java,
                    "chhotu_boss_memory.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
