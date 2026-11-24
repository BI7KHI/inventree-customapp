package xyz.bi7khi.inventree.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appearanceDataStore by preferencesDataStore(name = "inventree_appearance")

data class ThemeSettings(
    val seedArgb: Int = DEFAULT_SEED,
) {
    companion object {
        const val DEFAULT_SEED = 0xFFF2E900.toInt()
        const val LEGACY_DEFAULT_SEED = 0xFF146B54.toInt()
    }
}

class ThemeStore(private val context: Context) {

    val settings: Flow<ThemeSettings> = context.appearanceDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs ->
            val stored = prefs[KEY_SEED]
            val seed = when (stored) {
                null, ThemeSettings.LEGACY_DEFAULT_SEED -> ThemeSettings.DEFAULT_SEED
                else -> stored
            }
            ThemeSettings(seed)
        }

    suspend fun save(seedArgb: Int) {
        context.appearanceDataStore.edit { it[KEY_SEED] = seedArgb }
    }

    suspend fun reset() {
        context.appearanceDataStore.edit { it.remove(KEY_SEED) }
    }

    private companion object {
        val KEY_SEED = intPreferencesKey("theme_seed")
    }
}
