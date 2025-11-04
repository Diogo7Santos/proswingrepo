package com.example.proswing.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import com.example.proswing.ProSwingApp // your Application class, or use a context provider

private val Context.dataStore by preferencesDataStore("user_settings")

class SettingsRepository(private val context: Context = ProSwingApp.appContext) {

    private object Keys {
        val USE_METERS = booleanPreferencesKey("use_meters")
        val USE_CELSIUS = booleanPreferencesKey("use_celsius")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            useMeters = prefs[Keys.USE_METERS] ?: false,
            useCelsius = prefs[Keys.USE_CELSIUS] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: false
        )
    }

    suspend fun updateUseMeters(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_METERS] = value }
    }

    suspend fun updateUseCelsius(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_CELSIUS] = value }
    }

    suspend fun updateDarkTheme(value: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = value }
    }
}
