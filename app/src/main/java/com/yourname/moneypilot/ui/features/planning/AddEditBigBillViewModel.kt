package com.yourname.moneypilot.ui.features.planning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import com.yourname.moneypilot.data.local.database.entities.BillRecurrence
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class AddEditBigBillState(
    val id: Long = 0L,
    val name: String = "",
    val amount: String = "",
    val dueDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isPaid: Boolean = false,
    val categoryId: Long? = null,
    val reserveWalletId: Long? = null,
    val recurrenceType: BillRecurrence = BillRecurrence.ONCE,
    val reminderDaysBefore: String = "3",
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

sealed class AddEditBigBillEvent {
    data class EnteredName(val value: String) : AddEditBigBillEvent()
    data class EnteredAmount(val value: String) : AddEditBigBillEvent()
    data class DueDateChanged(val value: LocalDate) : AddEditBigBillEvent()
    data class EnteredNotes(val value: String) : AddEditBigBillEvent()
    data class CategoryChanged(val value: Long?) : AddEditBigBillEvent()
    data class ReserveWalletChanged(val value: Long?) : AddEditBigBillEvent()
    data class RecurrenceChanged(val value: BillRecurrence) : AddEditBigBillEvent()
    object SaveBill : AddEditBigBillEvent()
}

@HiltViewModel
class AddEditBigBillViewModel @Inject constructor(
    private val bigBillRepository: BigBillRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditBigBillState())
    val state: StateFlow<AddEditBigBillState> = _state.asStateFlow()

    init {
        loadData()
        val id = savedStateHandle.get<Long>("bigBillId")
        if (id != null && id != -1L) {
            viewModelScope.launch {
                bigBillRepository.getBigBillById(id)?.also { bill ->
                    _state.update { it.copy(
                        id = bill.id,
                        name = bill.name,
                        amount = bill.amount.toString(),
                        dueDate = bill.dueDate,
                        notes = bill.notes,
                        isPaid = bill.isPaid,
                        categoryId = bill.categoryId,
                        reserveWalletId = bill.reserveWalletId,
                        recurrenceType = bill.recurrenceType,
                        reminderDaysBefore = bill.reminderDaysBefore.toString()
                    ) }
                }
            }
        }
    }

    private fun loadData() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.update { it.copy(wallets = wallets) }
        }.launchIn(viewModelScope)

        categoryRepository.getCategoriesByType("EXPENSE").onEach { categories ->
            _state.update { it.copy(categories = categories) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AddEditBigBillEvent) {
        when (event) {
            is AddEditBigBillEvent.EnteredName -> _state.update { it.copy(name = event.value) }
            is AddEditBigBillEvent.EnteredAmount -> _state.update { it.copy(amount = event.value) }
            is AddEditBigBillEvent.DueDateChanged -> _state.update { it.copy(dueDate = event.value) }
            is AddEditBigBillEvent.EnteredNotes -> _state.update { it.copy(notes = event.value) }
            is AddEditBigBillEvent.CategoryChanged -> _state.update { it.copy(categoryId = event.value) }
            is AddEditBigBillEvent.ReserveWalletChanged -> _state.update { it.copy(reserveWalletId = event.value) }
            is AddEditBigBillEvent.RecurrenceChanged -> _state.update { it.copy(recurrenceType = event.value) }
            is AddEditBigBillEvent.SaveBill -> saveBill()
        }
    }

    private fun saveBill() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                val amountValue = currentState.amount.replace(",", "").toDoubleOrNull() ?: 0.0
                val reminderDays = currentState.reminderDaysBefore.toIntOrNull() ?: 3
                
                if (currentState.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a name."))
                    return@launch
                }
                if (amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount."))
                    return@launch
                }

                bigBillRepository.insertBigBill(
                    BigBillEntity(
                        id = currentState.id,
                        name = currentState.name,
                        amount = amountValue,
                        dueDate = currentState.dueDate,
                        categoryId = currentState.categoryId,
                        reserveWalletId = currentState.reserveWalletId,
                        recurrenceType = currentState.recurrenceType,
                        reminderDaysBefore = reminderDays,
                        isPaid = currentState.isPaid,
                        notes = currentState.notes,
                        updatedAt = LocalDateTime.now()
                    )
                )
                _eventFlow.emit(UiEvent.SaveBill)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save planned expense: ${e.message}"))
            }
        }
    }

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveBill : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
