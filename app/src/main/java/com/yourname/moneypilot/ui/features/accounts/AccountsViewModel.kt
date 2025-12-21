package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsState(
    val accounts: List<AccountEntity> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : BaseViewModel<AccountsState>() {

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            accountRepository.getAllAccounts().collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(AccountsState(list))
                }
            }
        }
    }

    fun archiveAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.updateAccount(account.copy(isArchived = true))
        }
    }
}
