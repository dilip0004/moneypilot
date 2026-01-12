package com.yourname.moneypilot.ui.features.planning

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import com.yourname.moneypilot.data.repository.BigBillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class AddEditBigBillState(
    val name: String = "",
    val amount: String = "",
    val dueDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isPaid: Boolean = false,
    val categoryId: Long? = null
)

@HiltViewModel
class AddEditBigBillViewModel @Inject constructor(
    private val bigBillRepository: BigBillRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(AddEditBigBillState())
    val state: State<AddEditBigBillState> = _state

    private var currentBigBillId: Long? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveBigBill : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        savedStateHandle.get<Long>("bigBillId")?.let { id ->
            if (id != -1L) {
                viewModelScope.launch {
                    bigBillRepository.getBigBillById(id)?.also { bill ->
                        currentBigBillId = bill.id
                        _state.value = _state.value.copy(
                            name = bill.name,
                            amount = bill.amount.toString(),
                            dueDate = bill.dueDate,
                            notes = bill.notes,
                            isPaid = bill.isPaid,
                            categoryId = bill.categoryId
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditBigBillEvent) {
        when (event) {
            is AddEditBigBillEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditBigBillEvent.EnteredAmount -> _state.value = _state.value.copy(amount = event.value)
            is AddEditBigBillEvent.DateChanged -> _state.value = _state.value.copy(dueDate = event.value)
            is AddEditBigBillEvent.EnteredNotes -> _state.value = _state.value.copy(notes = event.value)
            is AddEditBigBillEvent.StatusChanged -> _state.value = _state.value.copy(isPaid = event.value)
            is AddEditBigBillEvent.SaveBigBill -> saveBigBill()
        }
    }

    private fun saveBigBill() {
        viewModelScope.launch {
            try {
                val amountValue = _state.value.amount.toDoubleOrNull() ?: 0.0
                if (_state.value.name.isBlank() || amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid name and amount."))
                    return@launch
                }

                bigBillRepository.insertBigBill(
                    BigBillEntity(
                        id = currentBigBillId ?: 0L,
                        name = _state.value.name,
                        amount = amountValue,
                        dueDate = _state.value.dueDate,
                        categoryId = _state.value.categoryId,
                        isPaid = _state.value.isPaid,
                        notes = _state.value.notes,
                        updatedAt = LocalDateTime.now()
                    )
                )
                _eventFlow.emit(UiEvent.SaveBigBill)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save big bill"))
            }
        }
    }
}

sealed class AddEditBigBillEvent {
    data class EnteredName(val value: String) : AddEditBigBillEvent()
    data class EnteredAmount(val value: String) : AddEditBigBillEvent()
    data class DateChanged(val value: LocalDate) : AddEditBigBillEvent()
    data class EnteredNotes(val value: String) : AddEditBigBillEvent()
    data class StatusChanged(val value: Boolean) : AddEditBigBillEvent()
    object SaveBigBill : AddEditBigBillEvent()
}
