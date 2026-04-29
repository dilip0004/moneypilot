package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.SavedStateHandle
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
    val icon: String = "💰", // Default to an emoji
    // New fields for billing cycle (TASK-28)
    val billingStartDay: String = "",
    val dueDate: String = "",
    val creditLimit: String = ""
)

sealed class AddEditAccountEvent {
    data class EnteredName(val value: String) : AddEditAccountEvent()
    data class TypeChanged(val value: String) : AddEditAccountEvent()
    data class EnteredBalance(val value: String) : AddEditAccountEvent()
    data class EnteredMinBalance(val value: String) : AddEditAccountEvent()
    data class IconChanged(val value: String) : AddEditAccountEvent() // Added for #3
    object TogglePrimary : AddEditAccountEvent()
    // New events for TASK-28
    data class EnteredBillingStartDay(val value: String) : AddEditAccountEvent()
    data class EnteredDueDate(val value: String) : AddEditAccountEvent()
    data class EnteredCreditLimit(val value: String) : AddEditAccountEvent()
    object SaveAccount : AddEditAccountEvent()
}

@HiltViewModel
class AddEditAccountViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditAccountState())
    val state: StateFlow<AddEditAccountState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentWalletId: Long? = null

    sealed class UiEvent {
        object SaveAccount : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        // Load existing wallet if ID is provided in navigation
        val walletId = savedStateHandle.get<Long>("walletId")
        if (walletId != null && walletId != -1L) {
            viewModelScope.launch {
                walletRepository.getWalletById(walletId)?.let { wallet ->
                    currentWalletId = wallet.id
                    _state.update { it.copy(
                        name = wallet.name,
                        type = wallet.type,
                        initialBalance = wallet.initialBalance.toString(),
                        minBalance = wallet.minBalance.toString(),
                        isPrimary = wallet.isPrimary,
                        currency = wallet.currency,
                        color = wallet.color,
                        icon = wallet.icon,
                        billingStartDay = wallet.billingStartDay?.toString() ?: "",
                        dueDate = wallet.dueDate?.toString() ?: "",
                        creditLimit = wallet.creditLimit?.toString() ?: ""
                    ) }
                }
            }
        }
    }

    fun onEvent(event: AddEditAccountEvent) {
        when (event) {
            is AddEditAccountEvent.EnteredName -> _state.update { it.copy(name = event.value) }
            is AddEditAccountEvent.TypeChanged -> _state.update { it.copy(type = event.value) }
            is AddEditAccountEvent.EnteredBalance -> _state.update { it.copy(initialBalance = event.value) }
            is AddEditAccountEvent.EnteredMinBalance -> _state.update { it.copy(minBalance = event.value) }
            is AddEditAccountEvent.IconChanged -> _state.update { it.copy(icon = event.value) }
            is AddEditAccountEvent.TogglePrimary -> _state.update { it.copy(isPrimary = !it.isPrimary) }
            is AddEditAccountEvent.EnteredBillingStartDay -> _state.update { it.copy(billingStartDay = event.value) }
            is AddEditAccountEvent.EnteredDueDate -> _state.update { it.copy(dueDate = event.value) }
            is AddEditAccountEvent.EnteredCreditLimit -> _state.update { it.copy(creditLimit = event.value) }
            is AddEditAccountEvent.SaveAccount -> saveAccount()
        }
    }

    private fun saveAccount() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                if (currentState.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Account name cannot be empty"))
                    return@launch
                }
                val balance = currentState.initialBalance.toDoubleOrNull() ?: 0.0
                val minBal = currentState.minBalance.toDoubleOrNull() ?: 0.0
                
                val billingDay = currentState.billingStartDay.toIntOrNull()
                val dueDay = currentState.dueDate.toIntOrNull()
                val limit = currentState.creditLimit.toDoubleOrNull()

                // Validation for credit card specific fields
                if (currentState.type == "CREDIT") {
                    if (billingDay == null || billingDay !in 1..31) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid billing start day (1-31)"))
                        return@launch
                    }
                    if (dueDay == null || dueDay !in 1..31) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid due day (1-31)"))
                        return@launch
                    }
                }

                val wallet = WalletEntity(
                    id = currentWalletId ?: 0L,
                    name = currentState.name,
                    type = if (currentState.type == "CREDIT") "CREDIT_CARD" else currentState.type,
                    initialBalance = balance,
                    currentBalance = if (currentWalletId == null) balance else {
                        walletRepository.getWalletById(currentWalletId!!)?.currentBalance ?: balance
                    },
                    minBalance = minBal,
                    isPrimary = currentState.isPrimary,
                    currency = currentState.currency,
                    color = currentState.color,
                    icon = currentState.icon,
                    billingStartDay = billingDay,
                    billingEndDay = if (billingDay != null) if (billingDay == 1) 31 else billingDay - 1 else null,
                    dueDate = dueDay,
                    creditLimit = limit
                )

                if (currentWalletId == null) {
                    walletRepository.insertWallet(wallet)
                } else {
                    walletRepository.updateWallet(wallet)
                }
                
                _eventFlow.emit(UiEvent.SaveAccount)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save account"))
            }
        }
    }
}
