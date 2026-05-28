package com.yourname.moneypilot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.domain.usecase.ledger.VerifyLedgerIntegrityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val verifyLedgerIntegrityUseCase: VerifyLedgerIntegrityUseCase
) : ViewModel() {

    private val _integrityAlert = MutableSharedFlow<Int>()
    val integrityAlert = _integrityAlert.asSharedFlow()

    val userPreferences = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        performStartupIntegrityCheck()
    }

    private fun performStartupIntegrityCheck() {
        viewModelScope.launch {
            try {
                val mismatches = verifyLedgerIntegrityUseCase()
                if (mismatches.isNotEmpty()) {
                    Timber.e("GOVERNANCE ALERT: ${mismatches.size} integrity mismatches detected on startup!")
                    mismatches.forEach { 
                        Timber.e("Mismatch in ${it.walletName}: Stored=${it.storedBalance}, Calculated=${it.calculatedBalance}")
                    }
                    _integrityAlert.emit(mismatches.size)
                } else {
                    Timber.d("Integrity check passed. Ledger and balances are in sync.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Integrity check failed to execute")
            }
        }
    }
}
