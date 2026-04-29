package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class TransferState(
    val wallets: List<WalletEntity> = emptyList(),
    val fromWalletId: Long? = null,
    val toWalletId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now() // #37: Added date field
)

sealed class TransferEvent {
    data class FromWalletChanged(val walletId: Long) : TransferEvent()
    data class ToWalletChanged(val walletId: Long) : TransferEvent()
    data class EnteredAmount(val amount: String) : TransferEvent()
    data class EnteredDescription(val description: String) : TransferEvent()
    data class DateChanged(val dateTime: LocalDateTime) : TransferEvent() // #37
    object PerformTransfer : TransferEvent()
}

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
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
        loadWallets()
    }

    private fun loadWallets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(wallets = walletRepository.getAllWallets().first())
        }
    }

    fun onEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.FromWalletChanged -> _state.value = _state.value.copy(fromWalletId = event.walletId)
            is TransferEvent.ToWalletChanged -> _state.value = _state.value.copy(toWalletId = event.walletId)
            is TransferEvent.EnteredAmount -> _state.value = _state.value.copy(amount = event.amount)
            is TransferEvent.EnteredDescription -> _state.value = _state.value.copy(description = event.description)
            is TransferEvent.DateChanged -> _state.value = _state.value.copy(dateTime = event.dateTime)
            is TransferEvent.PerformTransfer -> performTransfer()
        }
    }

    private fun performTransfer() {
        viewModelScope.launch {
            val fromWalletId = _state.value.fromWalletId
            val toWalletId = _state.value.toWalletId
            val amount = _state.value.amount.toDoubleOrNull()

            if (fromWalletId == null || toWalletId == null) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Please select both wallets."))
                return@launch
            }

            if (fromWalletId == toWalletId) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Cannot transfer to the same wallet."))
                return@launch
            }

            if (amount == null || amount <= 0) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount."))
                return@launch
            }

            val transfer = TransactionEntity(
                id = UUID.randomUUID().toString(),
                dateTime = _state.value.dateTime, // #37: Use selected date
                amount = amount,
                type = TransactionType.Transfer,
                walletFromId = fromWalletId,
                walletToId = toWalletId,
                transactionSourceType = "MANUAL_TRANSFER",
                note = _state.value.description
            )

            try {
                transactionRepository.createTransfer(transfer)
                _eventFlow.emit(UiEvent.TransferSuccess)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Transfer failed: ${e.message}"))
            }
        }
    }
}
