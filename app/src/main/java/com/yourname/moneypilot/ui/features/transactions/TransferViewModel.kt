package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.domain.usecase.transaction.PerformTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class TransferState(
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val date: LocalDateTime = LocalDateTime.now(),
    val accounts: List<AccountEntity> = emptyList()
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val performTransferUseCase: PerformTransferUseCase
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
        accountRepository.getAllAccounts().onEach { accounts ->
            _state.value = _state.value.copy(
                accounts = accounts,
                fromAccountId = _state.value.fromAccountId ?: accounts.firstOrNull()?.id,
                toAccountId = _state.value.toAccountId ?: accounts.getOrNull(1)?.id
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.FromAccountChanged -> _state.value = _state.value.copy(fromAccountId = event.value)
            is TransferEvent.ToAccountChanged -> _state.value = _state.value.copy(toAccountId = event.value)
            is TransferEvent.EnteredAmount -> _state.value = _state.value.copy(amount = event.value)
            is TransferEvent.EnteredDescription -> _state.value = _state.value.copy(description = event.value)
            is TransferEvent.PerformTransfer -> performTransfer()
        }
    }

    private fun performTransfer() {
        viewModelScope.launch {
            val fromId = _state.value.fromAccountId
            val toId = _state.value.toAccountId
            val amount = _state.value.amount.toDoubleOrNull() ?: 0.0

            if (fromId == null || toId == null) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Please select both accounts"))
                return@launch
            }
            if (fromId == toId) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Source and destination accounts must be different"))
                return@launch
            }
            if (amount <= 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount"))
                return@launch
            }

            try {
                performTransferUseCase(
                    fromAccountId = fromId,
                    toAccountId = toId,
                    amount = amount,
                    description = _state.value.description,
                    date = _state.value.date
                )
                _eventFlow.emit(UiEvent.TransferSuccess)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Transfer failed: ${e.message}"))
            }
        }
    }
}

sealed class TransferEvent {
    data class FromAccountChanged(val value: Long) : TransferEvent()
    data class ToAccountChanged(val value: Long) : TransferEvent()
    data class EnteredAmount(val value: String) : TransferEvent()
    data class EnteredDescription(val value: String) : TransferEvent()
    object PerformTransfer : TransferEvent()
}
