package com.yourname.moneypilot.ui.features.investments

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class AddEditInvestmentState(
    val name: String = "",
    val type: String = "STOCKS",
    val symbol: String = "",
    val quantity: String = "",
    val averagePrice: String = "",
    val currentPrice: String = "",
    val currency: String = "INR",
    val linkedWalletId: Long? = null,
    val wallets: List<WalletEntity> = emptyList()
)

@HiltViewModel
class AddEditInvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(AddEditInvestmentState())
    val state: State<AddEditInvestmentState> = _state

    private var currentInvestmentId: Long? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveInvestment : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadWallets()
        val id = savedStateHandle.get<Long>("investmentId")
        if (id != null && id != -1L) {
            viewModelScope.launch {
                investmentRepository.getInvestmentById(id)?.let { investment ->
                    currentInvestmentId = investment.id
                    _state.value = _state.value.copy(
                        name = investment.name,
                        type = investment.type,
                        symbol = investment.symbol,
                        quantity = investment.quantity.toString(),
                        averagePrice = investment.averagePrice.toString(),
                        currentPrice = investment.currentPrice.toString(),
                        currency = investment.currency,
                        linkedWalletId = investment.linkedWalletId
                    )
                }
            }
        }
    }

    private fun loadWallets() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.value = _state.value.copy(wallets = wallets)
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AddEditInvestmentEvent) {
        when (event) {
            is AddEditInvestmentEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditInvestmentEvent.EnteredSymbol -> _state.value = _state.value.copy(symbol = event.value)
            is AddEditInvestmentEvent.EnteredQuantity -> _state.value = _state.value.copy(quantity = event.value)
            is AddEditInvestmentEvent.EnteredAvgPrice -> _state.value = _state.value.copy(averagePrice = event.value)
            is AddEditInvestmentEvent.EnteredCurrentPrice -> _state.value = _state.value.copy(currentPrice = event.value)
            is AddEditInvestmentEvent.TypeChanged -> _state.value = _state.value.copy(type = event.value)
            is AddEditInvestmentEvent.WalletLinked -> _state.value = _state.value.copy(linkedWalletId = event.value)
            is AddEditInvestmentEvent.SaveInvestment -> saveInvestment()
        }
    }

    private fun saveInvestment() {
        viewModelScope.launch {
            try {
                val s = _state.value
                val qty = s.quantity.toDoubleOrNull() ?: 0.0
                val avgPrice = s.averagePrice.toDoubleOrNull() ?: 0.0
                val investedAmount = qty * avgPrice

                if (s.name.isBlank() || qty <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please fill required fields (Name, Quantity)."))
                    return@launch
                }

                val investment = InvestmentEntity(
                    id = currentInvestmentId ?: 0L,
                    name = s.name,
                    type = s.type,
                    symbol = s.symbol,
                    quantity = qty,
                    averagePrice = avgPrice,
                    currentPrice = s.currentPrice.toDoubleOrNull() ?: avgPrice,
                    currency = s.currency,
                    linkedWalletId = s.linkedWalletId,
                    lastUpdated = LocalDateTime.now()
                )

                // 1. Record the Investment
                investmentRepository.insertInvestment(investment)

                // 2. Ledger Integration (Only for new buys)
                if (currentInvestmentId == null && s.linkedWalletId != null) {
                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            walletFromId = s.linkedWalletId,
                            type = TransactionType.Expense,
                            amount = investedAmount,
                            note = "Investment Purchase: ${s.name} (${s.symbol})",
                            dateTime = LocalDateTime.now(),
                            transactionSourceType = "INVESTMENT_BUY"
                        )
                    )
                }

                _eventFlow.emit(UiEvent.SaveInvestment)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Save failed: ${e.message}"))
            }
        }
    }
}

sealed class AddEditInvestmentEvent {
    data class EnteredName(val value: String) : AddEditInvestmentEvent()
    data class EnteredSymbol(val value: String) : AddEditInvestmentEvent()
    data class EnteredQuantity(val value: String) : AddEditInvestmentEvent()
    data class EnteredAvgPrice(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentPrice(val value: String) : AddEditInvestmentEvent()
    data class TypeChanged(val value: String) : AddEditInvestmentEvent()
    data class WalletLinked(val value: Long?) : AddEditInvestmentEvent()
    object SaveInvestment : AddEditInvestmentEvent()
}
