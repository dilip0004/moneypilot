package com.yourname.moneypilot.ui.features.investments

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.data.repository.InvestmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class AddEditInvestmentState(
    val name: String = "",
    val type: String = "STOCKS",
    val symbol: String = "",
    val quantity: String = "",
    val averagePrice: String = "",
    val currentPrice: String = "",
    val currency: String = "INR"
)

@HiltViewModel
class AddEditInvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    savedStateHandle: SavedStateHandle
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
        savedStateHandle.get<Long>("investmentId")?.let { id ->
            if (id != -1L) {
                viewModelScope.launch {
                    // Fetch investment logic here
                }
            }
        }
    }

    fun onEvent(event: AddEditInvestmentEvent) {
        when (event) {
            is AddEditInvestmentEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditInvestmentEvent.EnteredSymbol -> _state.value = _state.value.copy(symbol = event.value)
            is AddEditInvestmentEvent.EnteredQuantity -> _state.value = _state.value.copy(quantity = event.value)
            is AddEditInvestmentEvent.EnteredAvgPrice -> _state.value = _state.value.copy(averagePrice = event.value)
            is AddEditInvestmentEvent.EnteredCurrentPrice -> _state.value = _state.value.copy(currentPrice = event.value)
            is AddEditInvestmentEvent.TypeChanged -> _state.value = _state.value.copy(type = event.value)
            is AddEditInvestmentEvent.SaveInvestment -> saveInvestment()
        }
    }

    private fun saveInvestment() {
        viewModelScope.launch {
            try {
                if (_state.value.name.isBlank() || _state.value.quantity.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please fill required fields."))
                    return@launch
                }

                investmentRepository.insertInvestment(
                    InvestmentEntity(
                        id = currentInvestmentId ?: 0L,
                        name = _state.value.name,
                        type = _state.value.type,
                        symbol = _state.value.symbol,
                        quantity = _state.value.quantity.toDoubleOrNull() ?: 0.0,
                        averagePrice = _state.value.averagePrice.toDoubleOrNull() ?: 0.0,
                        currentPrice = _state.value.currentPrice.toDoubleOrNull() ?: 0.0,
                        currency = _state.value.currency,
                        lastUpdated = LocalDateTime.now()
                    )
                )
                _eventFlow.emit(UiEvent.SaveInvestment)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Save failed."))
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
    object SaveInvestment : AddEditInvestmentEvent()
}
