package com.example.proswing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proswing.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserSettings(
    val useMeters: Boolean = false,
    val useCelsius: Boolean = true,
    val darkTheme: Boolean = false,
    val handicap: Int = 25 // ✅ Default handicap (Casual Player)
)

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepository()
) : ViewModel() {

    // Expose current settings as a StateFlow
    val settings = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repo.defaultSettings
    )

    // Distance unit (yards/meters)
    fun setUseMeters(value: Boolean) {
        viewModelScope.launch { repo.updateUseMeters(value) }
    }

    // Temperature unit (C/F)
    fun setUseCelsius(value: Boolean) {
        viewModelScope.launch { repo.updateUseCelsius(value) }
    }

    // Theme (light/dark)
    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch { repo.updateDarkTheme(value) }
    }

    // ✅ Handicap
    fun setHandicap(value: Int) {
        viewModelScope.launch { repo.updateHandicap(value) }
    }
}
