package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditAccountState(
    val name: String = "",
    val type: String = "BANK",
    val initialBalance: String = "",
    val minBalance: String = "0",
    val isPrimary: Boolean = false,
    val currency: String = "INR",
    val color: Int = 0xFF0067FF.toInt(),
    val icon: String = "account_balance"
)

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditAccountState())
    val state: StateFlow<AddEditAccountState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveAccount : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    fun onEvent(event: AddEditAccountEvent) {
        when (event) {
            is AddEditAccountEvent.EnteredName -> _state.update { it.copy(name = event.value) }
            is AddEditAccountEvent.TypeChanged -> _state.update { it.copy(type = event.value) }
            is AddEditAccountEvent.EnteredBalance -> _state.update { it.copy(initialBalance = event.value) }
            is AddEditAccountEvent.EnteredMinBalance -> _state.update { it.copy(minBalance = event.value) }
            is AddEditAccountEvent.TogglePrimary -> _state.update { it.copy(isPrimary = !it.isPrimary) }
            is AddEditAccountEvent.SaveAccount -> saveAccount()
        }
    }

    private fun saveAccount() {
        viewModelScope.launch {
            try {
                if (_state.value.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Account name cannot be empty"))
                    return@launch
                }
                val balance = _state.value.initialBalance.toDoubleOrNull() ?: 0.0
                val minBal = _state.value.minBalance.toDoubleOrNull() ?: 0.0
                
                walletRepository.insertWallet(
                    WalletEntity(
                        name = _state.value.name,
                        type = _state.value.type,
                        initialBalance = balance,
                        currentBalance = balance,
                        minBalance = minBal,
                        isPrimary = _state.value.isPrimary,
                        currency = _state.value.currency,
                        color = _state.value.color,
                        icon = _state.value.icon
                    )
                )
                _eventFlow.emit(UiEvent.SaveAccount)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save account"))
            }
        }
    }
}

sealed class AddEditAccountEvent {
    data class EnteredName(val value: String) : AddEditAccountEvent()
    data class TypeChanged(val value: String) : AddEditAccountEvent()
    data class EnteredBalance(val value: String) : AddEditAccountEvent()
    data class EnteredMinBalance(val value: String) : AddEditAccountEvent()
    object TogglePrimary : AddEditAccountEvent()
    object SaveAccount : AddEditAccountEvent()
}
