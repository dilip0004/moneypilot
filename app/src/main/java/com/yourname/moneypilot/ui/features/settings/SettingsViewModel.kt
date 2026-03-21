package com.yourname.moneypilot.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.preferences.AppTheme
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    val userPreferences = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateDarkMode(mode: String) {
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

    fun updateUseBiometrics(use: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateUseBiometrics(use)
        }
    }

    fun updateDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDailySummaryEnabled(enabled)
            val prefs = preferencesRepository.userPreferencesFlow.first()
            notificationScheduler.scheduleDailySummary(prefs.copy(dailySummaryEnabled = enabled))
        }
    }

    fun updateDailySummaryTime(time: String) {
        viewModelScope.launch {
            preferencesRepository.updateDailySummaryTime(time)
            val prefs = preferencesRepository.userPreferencesFlow.first()
            notificationScheduler.scheduleDailySummary(prefs.copy(dailySummaryTime = time))
        }
    }

    fun updateTrueBlack(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateUseTrueBlack(enabled)
        }
    }

    fun updateFontFamily(font: String) {
        viewModelScope.launch {
            preferencesRepository.updateFontFamily(font)
        }
    }
}
