package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class TransferState(
    val accounts: List<AccountEntity> = emptyList(),
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val amount: String = "",
    val description: String = ""
)

sealed class TransferEvent {
    data class FromAccountChanged(val accountId: Long) : TransferEvent()
    data class ToAccountChanged(val accountId: Long) : TransferEvent()
    data class EnteredAmount(val amount: String) : TransferEvent()
    data class EnteredDescription(val description: String) : TransferEvent()
    object PerformTransfer : TransferEvent()
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = mutableStateOf(TransferState())
    val state: State<TransferState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object TransferSuccess : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().first()
            _state.value = _state.value.copy(accounts = accounts)
        }
    }

    fun onEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.FromAccountChanged -> _state.value = _state.value.copy(fromAccountId = event.accountId)
            is TransferEvent.ToAccountChanged -> _state.value = _state.value.copy(toAccountId = event.accountId)
            is TransferEvent.EnteredAmount -> _state.value = _state.value.copy(amount = event.amount)
            is TransferEvent.EnteredDescription -> _state.value = _state.value.copy(description = event.description)
            is TransferEvent.PerformTransfer -> performTransfer()
        }
    }

    private fun performTransfer() {
        viewModelScope.launch {
            val fromAccountId = _state.value.fromAccountId
            val toAccountId = _state.value.toAccountId
            val amount = _state.value.amount.toDoubleOrNull()

            if (fromAccountId == null || toAccountId == null) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Please select both accounts."))
                return@launch
            }

            if (fromAccountId == toAccountId) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot transfer to the same account."))
                return@launch
            }

            if (amount == null || amount <= 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount."))
                return@launch
            }

            val now = LocalDateTime.now()

            val withdrawal = TransactionEntity(
                accountId = fromAccountId,
                type = "EXPENSE", // Internal transfer, but logged as expense for the source
                amount = amount,
                description = "Transfer to ${getAccountName(toAccountId)}",
                date = now
            )

            val deposit = TransactionEntity(
                accountId = toAccountId,
                type = "INCOME", // And as income for the destination
                amount = amount,
                description = "Transfer from ${getAccountName(fromAccountId)}",
                date = now
            )

            transactionRepository.insertTransaction(withdrawal)
            transactionRepository.insertTransaction(deposit)
            
            // Update account balances
            accountRepository.updateBalance(fromAccountId, -amount)
            accountRepository.updateBalance(toAccountId, amount)

            _eventFlow.emit(UiEvent.TransferSuccess)
        }
    }

    private fun getAccountName(accountId: Long): String {
        return _state.value.accounts.find { it.id == accountId }?.name ?: "Unknown Account"
    }
}
