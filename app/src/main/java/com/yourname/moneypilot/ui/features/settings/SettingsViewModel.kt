package com.yourname.moneypilot.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.preferences.AppTheme
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateDarkMode(mode: String) {
        // This is a legacy function, keeping it for compatibility or redirecting
        val theme = when(mode) {
            "LIGHT" -> AppTheme.LIGHT
            "DARK" -> AppTheme.DARK
            "OLED" -> AppTheme.OLED
            else -> AppTheme.SYSTEM
        }
        updateTheme(theme)
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(theme)
        }
    }

    fun updatePrimaryColor(color: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePrimaryColor(color)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateUseDynamicColor(enabled)
        }
    }

    fun updateTrueBlack(enabled: Boolean) {
        if (enabled) updateTheme(AppTheme.OLED)
        else updateTheme(AppTheme.DARK)
    }
}
