package com.yourname.moneypilot.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.util.SecurityPreferences
import com.yourname.moneypilot.worker.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val securityPreferences: SecurityPreferences,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    val userPreferences = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Use the reactive flow from securityPreferences
    val isPinSet = securityPreferences.isPinSetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateUseBiometrics(use: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateUseBiometrics(use)
        }
    }

    fun updatePrivacyMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePrivacyMode(enabled)
        }
    }

    fun updateIncludeGoalsInNetWorth(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateIncludeGoalsInNetWorth(enabled)
        }
    }

    suspend fun setPin(pin: String) {
        securityPreferences.setPin(pin)
    }

    suspend fun clearPin() {
        securityPreferences.clearPin()
        preferencesRepository.updateUseBiometrics(false)
    }
}
