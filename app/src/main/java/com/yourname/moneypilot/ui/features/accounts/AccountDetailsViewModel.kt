package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountDetailsState(
    val wallet: WalletEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AccountDetailsViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDetailsState())
    val state: StateFlow<AccountDetailsState> = _state.asStateFlow()

    private val walletId: Long = checkNotNull(savedStateHandle["walletId"])

    init {
        loadWalletDetails()
    }

    private fun loadWalletDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val wallet = walletRepository.getWalletById(walletId)
                _state.value = _state.value.copy(wallet = wallet, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to load wallet details.", isLoading = false)
            }
        }
    }
}
