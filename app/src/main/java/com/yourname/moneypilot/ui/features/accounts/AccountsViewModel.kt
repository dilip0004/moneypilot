package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AccountsState(
    val accounts: List<WalletEntity> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
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
                    _uiState.value = ScreenState.Success(AccountsState(list))
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
