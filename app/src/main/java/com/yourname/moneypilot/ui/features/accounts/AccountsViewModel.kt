package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.domain.usecase.ledger.VerifyLedgerIntegrityUseCase
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AccountsState(
    val accounts: List<WalletEntity> = emptyList(),
    val mismatchedWalletIds: Set<Long> = emptySet()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val verifyLedgerIntegrityUseCase: VerifyLedgerIntegrityUseCase
) : BaseViewModel<AccountsState>() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            walletRepository.getAllWallets().collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    // Section 9.0: Governance Integrity Audit for the Position Layer
                    val mismatches = verifyLedgerIntegrityUseCase()
                    val mismatchedIds = mismatches.map { it.walletId }.toSet()
                    
                    _uiState.value = ScreenState.Success(
                        AccountsState(
                            accounts = list,
                            mismatchedWalletIds = mismatchedIds
                        )
                    )
                }
            }
        }
    }

    fun archiveAccount(account: WalletEntity) {
        viewModelScope.launch {
            walletRepository.updateWallet(account.copy(isArchived = true))
        }
    }

    fun deleteWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            val count = transactionRepository.getTransactionCountForWallet(wallet.id)
            if (count > 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot delete wallet with ${count} transactions. Please archive it instead."))
            } else {
                walletRepository.deleteWallet(wallet)
            }
        }
    }
}
