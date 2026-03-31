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
    val name: String = "",
    val amount: String = "",
    val dueDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isPaid: Boolean = false,
    val categoryId: Long? = null,
    val linkedWalletId: Long? = null,
    val recurrenceType: BillRecurrence = BillRecurrence.ONCE,
    val autoReserveFlag: Boolean = false,
    val reminderDaysBefore: String = "3",
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

sealed class AddEditBigBillEvent {
    data class EnteredName(val value: String) : AddEditBigBillEvent()
    data class EnteredAmount(val value: String) : AddEditBigBillEvent()
    data class DateChanged(val value: LocalDate) : AddEditBigBillEvent()
    data class EnteredNotes(val value: String) : AddEditBigBillEvent()
    data class StatusChanged(val value: Boolean) : AddEditBigBillEvent()
    data class CategoryChanged(val value: Long?) : AddEditBigBillEvent()
    data class WalletChanged(val value: Long?) : AddEditBigBillEvent()
    data class RecurrenceChanged(val value: BillRecurrence) : AddEditBigBillEvent()
    data class AutoReserveChanged(val value: Boolean) : AddEditBigBillEvent()
    data class ReminderDaysChanged(val value: String) : AddEditBigBillEvent()
    object SaveBigBill : AddEditBigBillEvent()
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

    private var currentBigBillId: Long? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveBigBill : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadData()
        val id = savedStateHandle.get<Long>("bigBillId")
        if (id != null && id != -1L) {
            viewModelScope.launch {
                bigBillRepository.getBigBillById(id)?.also { bill ->
                    currentBigBillId = bill.id
                    _state.update { it.copy(
                        name = bill.name,
                        amount = bill.amount.toString(),
                        dueDate = bill.dueDate,
                        notes = bill.notes,
                        isPaid = bill.isPaid,
                        categoryId = bill.categoryId,
                        linkedWalletId = bill.linkedWalletId,
                        recurrenceType = bill.recurrenceType,
                        autoReserveFlag = bill.autoReserveFlag,
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
            is AddEditBigBillEvent.DateChanged -> _state.update { it.copy(dueDate = event.value) }
            is AddEditBigBillEvent.EnteredNotes -> _state.update { it.copy(notes = event.value) }
            is AddEditBigBillEvent.StatusChanged -> _state.update { it.copy(isPaid = event.value) }
            is AddEditBigBillEvent.CategoryChanged -> _state.update { it.copy(categoryId = event.value) }
            is AddEditBigBillEvent.WalletChanged -> _state.update { it.copy(linkedWalletId = event.value) }
            is AddEditBigBillEvent.RecurrenceChanged -> _state.update { it.copy(recurrenceType = event.value) }
            is AddEditBigBillEvent.AutoReserveChanged -> _state.update { it.copy(autoReserveFlag = event.value) }
            is AddEditBigBillEvent.ReminderDaysChanged -> _state.update { it.copy(reminderDaysBefore = event.value) }
            is AddEditBigBillEvent.SaveBigBill -> saveBigBill()
        }
    }

    private fun saveBigBill() {
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
                        id = currentBigBillId ?: 0L,
                        name = currentState.name,
                        amount = amountValue,
                        dueDate = currentState.dueDate,
                        categoryId = currentState.categoryId,
                        linkedWalletId = currentState.linkedWalletId,
                        recurrenceType = currentState.recurrenceType,
                        autoReserveFlag = currentState.autoReserveFlag,
                        reminderDaysBefore = reminderDays,
                        isPaid = currentState.isPaid,
                        notes = currentState.notes,
                        updatedAt = LocalDateTime.now()
                    )
                )
                _eventFlow.emit(UiEvent.SaveBigBill)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save big bill: ${e.message}"))
            }
        }
    }
}
