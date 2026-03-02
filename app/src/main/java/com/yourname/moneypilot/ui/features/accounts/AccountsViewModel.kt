package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


data class AccountsState(
    val accounts: List<WalletEntity> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : BaseViewModel<AccountsState>() {

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
}
