package com.example.proswing.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.proswing.ProSwingApp
import com.example.proswing.viewmodel.UserSettings

// Create a single DataStore instance scoped to the application
private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(
    private val context: Context = ProSwingApp.appContext
) {
    // Preference keys
    private object Keys {
        val USE_METERS = booleanPreferencesKey("use_meters")
        val USE_CELSIUS = booleanPreferencesKey("use_celsius")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val HANDICAP = intPreferencesKey("handicap") // ✅ Added handicap key
    }

    // Default settings for initialization
    val defaultSettings = UserSettings(
        useMeters = false,
        useCelsius = true,
        darkTheme = false,
        handicap = 25 //
    )

    // Flow of settings updates
    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            useMeters = prefs[Keys.USE_METERS] ?: defaultSettings.useMeters,
            useCelsius = prefs[Keys.USE_CELSIUS] ?: defaultSettings.useCelsius,
            darkTheme = prefs[Keys.DARK_THEME] ?: defaultSettings.darkTheme,
            handicap = prefs[Keys.HANDICAP] ?: defaultSettings.handicap
        )
    }

    // Update distance unit
    suspend fun updateUseMeters(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_METERS] = value
        }
    }

    // Update temperature unit
    suspend fun updateUseCelsius(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_CELSIUS] = value
        }
    }

    // Update theme mode
    suspend fun updateDarkTheme(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = value
        }
    }

    // ✅ Update handicap
    suspend fun updateHandicap(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HANDICAP] = value
        }
    }
}
