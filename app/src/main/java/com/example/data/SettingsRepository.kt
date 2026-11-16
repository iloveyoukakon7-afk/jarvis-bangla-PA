package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chhotu_boss_prefs"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WAKE_NAME = stringPreferencesKey("wake_name")
        val UNLOCK_PHRASE = stringPreferencesKey("unlock_phrase")
        val DIALECT_MODE = stringPreferencesKey("dialect_mode")
        val MEMORY_RETENTION_DAYS = intPreferencesKey("memory_retention_days")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PERMISSIONS_GRANTED_ONCE = booleanPreferencesKey("permissions_granted_once")
        val CALL_SCREENING_ENABLED = booleanPreferencesKey("call_screening_enabled")
        val WAKE_WORD_ACTIVE = booleanPreferencesKey("wake_word_active")
        val AUTO_RECORD_CALLS = booleanPreferencesKey("auto_record_calls")
        val BOSS_SCHEDULE = stringPreferencesKey("boss_schedule")
        val FIRST_LAUNCH_MILLIS = longPreferencesKey("first_launch_millis")
        val GRANTED_CATEGORIES = stringSetPreferencesKey("granted_categories")
    }

    val settingsFlow: Flow<AssistantSettings> = context.jarvisDataStore.data.map { prefs ->
        val dialect = runCatching {
            DialectMode.valueOf(prefs[Keys.DIALECT_MODE] ?: DialectMode.DHAKAIYA.name)
        }.getOrDefault(DialectMode.DHAKAIYA)

        val schedule = runCatching {
            BossScheduleStatus.valueOf(
                prefs[Keys.BOSS_SCHEDULE] ?: BossScheduleStatus.FREE.name
            )
        }.getOrDefault(BossScheduleStatus.FREE)

        AssistantSettings(
            wakeName = prefs[Keys.WAKE_NAME]?.takeIf { it.isNotBlank() } ?: "ওই ছোটু",
            unlockPhrase = prefs[Keys.UNLOCK_PHRASE]?.takeIf { it.isNotBlank() }
                ?: "ওই ছোটু, স্ক্রিন খোলো",
            dialectMode = dialect,
            memoryRetentionDays = prefs[Keys.MEMORY_RETENTION_DAYS] ?: 10,
            speechSpeed = prefs[Keys.SPEECH_SPEED] ?: 1.0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            permissionsGrantedOnce = prefs[Keys.PERMISSIONS_GRANTED_ONCE] ?: false,
            callScreeningEnabled = prefs[Keys.CALL_SCREENING_ENABLED] ?: true,
            wakeWordActive = prefs[Keys.WAKE_WORD_ACTIVE] ?: true,
            autoRecordCalls = prefs[Keys.AUTO_RECORD_CALLS] ?: false,
            bossSchedule = schedule,
            firstLaunchMillis = prefs[Keys.FIRST_LAUNCH_MILLIS] ?: System.currentTimeMillis(),
            grantedCategories = prefs[Keys.GRANTED_CATEGORIES] ?: emptySet()
        )
    }

    suspend fun setDialectMode(mode: DialectMode) {
        context.jarvisDataStore.edit { it[Keys.DIALECT_MODE] = mode.name }
    }

    suspend fun setMemoryRetentionDays(days: Int) {
        val valid = if (days == 5) 5 else 10
        context.jarvisDataStore.edit { it[Keys.MEMORY_RETENTION_DAYS] = valid }
    }

    suspend fun setSpeechSpeed(speed: Float) {
        context.jarvisDataStore.edit { it[Keys.SPEECH_SPEED] = speed.coerceIn(0.75f, 1.35f) }
    }

    suspend fun setPermissionsGrantedOnce(granted: Boolean) {
        context.jarvisDataStore.edit { it[Keys.PERMISSIONS_GRANTED_ONCE] = granted }
    }

    suspend fun grantPermissionCategory(category: PermissionCategory) {
        context.jarvisDataStore.edit { prefs ->
            val current = prefs[Keys.GRANTED_CATEGORIES] ?: emptySet()
            prefs[Keys.GRANTED_CATEGORIES] = current + category.name
        }
    }

    suspend fun setAutoRecordCalls(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.AUTO_RECORD_CALLS] = enabled }
    }

    suspend fun setBossSchedule(status: BossScheduleStatus) {
        context.jarvisDataStore.edit { it[Keys.BOSS_SCHEDULE] = status.name }
    }

    suspend fun completeOnboarding(dialectMode: DialectMode) {
        context.jarvisDataStore.edit { prefs ->
            prefs[Keys.WAKE_NAME] = "ওই ছোটু"
            prefs[Keys.UNLOCK_PHRASE] = "ওই ছোটু, স্ক্রিন খোলো"
            prefs[Keys.DIALECT_MODE] = dialectMode.name
            prefs[Keys.WAKE_WORD_ACTIVE] = true
            prefs[Keys.ONBOARDING_COMPLETED] = true
            if (prefs[Keys.FIRST_LAUNCH_MILLIS] == null) {
                prefs[Keys.FIRST_LAUNCH_MILLIS] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setCallScreeningEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.CALL_SCREENING_ENABLED] = enabled }
    }

    suspend fun setWakeWordActive(active: Boolean) {
        context.jarvisDataStore.edit { it[Keys.WAKE_WORD_ACTIVE] = active }
    }
}
