package com.br1ansouza.chromix.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game_prefs")

/**
 * Persistência local do progresso e das preferências via DataStore.
 * Único estado salvo do app: nível atual, recorde e vibração.
 */
class GamePreferences(private val context: Context) {

    data class GameProgress(
        val currentLevel: Int,
        val bestLevelReached: Int,
        val vibrationEnabled: Boolean,
    )

    val progress: Flow<GameProgress> = context.dataStore.data.map { prefs ->
        GameProgress(
            currentLevel = prefs[KEY_CURRENT_LEVEL] ?: 1,
            bestLevelReached = prefs[KEY_BEST_LEVEL] ?: 1,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
        )
    }

    suspend fun setCurrentLevel(level: Int) {
        context.dataStore.edit { it[KEY_CURRENT_LEVEL] = level }
    }

    suspend fun setBestLevelReached(level: Int) {
        context.dataStore.edit { it[KEY_BEST_LEVEL] = level }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    private companion object {
        val KEY_CURRENT_LEVEL = intPreferencesKey("current_level")
        val KEY_BEST_LEVEL = intPreferencesKey("best_level_reached")
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    }
}
