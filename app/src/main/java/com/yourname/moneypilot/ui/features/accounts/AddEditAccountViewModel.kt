package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditAccountState(
    val name: String = "",
    val type: String = "BANK",
    val initialBalance: String = "",
    val currency: String = "USD",
    val color: Int = 0xFF0067FF.toInt(),
    val icon: String = "account_balance"
)

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = mutableStateOf(AddEditAccountState())
    val state: State<AddEditAccountState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveAccount : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    fun onEvent(event: AddEditAccountEvent) {
        when (event) {
            is AddEditAccountEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditAccountEvent.TypeChanged -> _state.value = _state.value.copy(type = event.value)
            is AddEditAccountEvent.EnteredBalance -> _state.value = _state.value.copy(initialBalance = event.value)
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
                
                accountRepository.insertAccount(
                    AccountEntity(
                        name = _state.value.name,
                        type = _state.value.type,
                        initialBalance = balance,
                        currentBalance = balance,
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
    object SaveAccount : AddEditAccountEvent()
}
