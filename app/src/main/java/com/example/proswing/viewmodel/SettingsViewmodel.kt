package com.example.proswing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserSettings(
    val useMeters: Boolean = false,
    val useCelsius: Boolean = true,
    val darkTheme: Boolean = false
)

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepository()
) : ViewModel() {

    val settings = repo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserSettings()
    )

    fun setUseMeters(value: Boolean) = viewModelScope.launch {
        repo.updateUseMeters(value)
    }

    fun setUseCelsius(value: Boolean) = viewModelScope.launch {
        repo.updateUseCelsius(value)
    }

    fun setDarkTheme(value: Boolean) = viewModelScope.launch {
        repo.updateDarkTheme(value)
    }
}
