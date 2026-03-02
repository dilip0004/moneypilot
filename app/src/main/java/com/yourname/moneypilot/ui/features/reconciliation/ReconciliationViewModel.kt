package com.yourname.moneypilot.ui.features.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.domain.usecase.ledger.CreateAdjustmentTransactionUseCase
import com.yourname.moneypilot.domain.usecase.ledger.IntegrityMismatch
import com.yourname.moneypilot.domain.usecase.ledger.VerifyLedgerIntegrityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReconciliationState(
    val mismatches: List<IntegrityMismatch> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReconciliationViewModel @Inject constructor(
    private val verifyLedgerIntegrityUseCase: VerifyLedgerIntegrityUseCase,
    private val createAdjustmentTransactionUseCase: CreateAdjustmentTransactionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReconciliationState())
    val state: StateFlow<ReconciliationState> = _state.asStateFlow()

    fun runIntegrityCheck() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val mismatches = verifyLedgerIntegrityUseCase()
            _state.update { it.copy(mismatches = mismatches, isLoading = false) }
        }
    }

    fun repairMismatch(mismatch: IntegrityMismatch) {
        viewModelScope.launch {
            val discrepancy = mismatch.calculatedBalance - mismatch.storedBalance
            createAdjustmentTransactionUseCase(mismatch.walletId, discrepancy)
            // Re-run the check to confirm the fix
            runIntegrityCheck()
        }
    }
}
