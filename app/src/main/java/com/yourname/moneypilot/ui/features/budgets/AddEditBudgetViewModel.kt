package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddEditBudgetState(
    val categoryId: Long? = null,
    val amount: String = "",
    val period: String = "MONTHLY",
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
    val rolloverEnabled: Boolean = false,
    val alertThreshold: Int = 90,
    val categories: List<CategoryEntity> = emptyList()
)

@HiltViewModel
class AddEditBudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = mutableStateOf(AddEditBudgetState())
    val state: State<AddEditBudgetState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveBudget : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadCategories()
    }

    private fun loadCategories() {
        categoryRepository.getAllCategories().onEach { categories ->
            _state.value = _state.value.copy(
                categories = categories,
                categoryId = _state.value.categoryId ?: categories.firstOrNull()?.id
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AddEditBudgetEvent) {
        when (event) {
            is AddEditBudgetEvent.CategoryChanged -> _state.value = _state.value.copy(categoryId = event.value)
            is AddEditBudgetEvent.EnteredAmount -> _state.value = _state.value.copy(amount = event.value)
            is AddEditBudgetEvent.PeriodChanged -> _state.value = _state.value.copy(period = event.value)
            is AddEditBudgetEvent.ToggleRollover -> _state.value = _state.value.copy(rolloverEnabled = !state.value.rolloverEnabled)
            is AddEditBudgetEvent.AlertThresholdChanged -> _state.value = _state.value.copy(alertThreshold = event.value)
            is AddEditBudgetEvent.SaveBudget -> saveBudget()
        }
    }

    private fun saveBudget() {
        viewModelScope.launch {
            try {
                val amount = _state.value.amount.toDoubleOrNull() ?: 0.0
                if (amount <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount"))
                    return@launch
                }
                if (_state.value.categoryId == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select a category"))
                    return@launch
                }

                budgetRepository.insertBudget(
                    BudgetEntity(
                        categoryId = _state.value.categoryId!!,
                        amount = amount,
                        period = _state.value.period,
                        startDate = _state.value.startDate,
                        endDate = _state.value.endDate,
                        rolloverEnabled = _state.value.rolloverEnabled,
                        alertThreshold = _state.value.alertThreshold
                    )
                )
                _eventFlow.emit(UiEvent.SaveBudget)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save budget"))
            }
        }
    }
}

sealed class AddEditBudgetEvent {
    data class CategoryChanged(val value: Long) : AddEditBudgetEvent()
    data class EnteredAmount(val value: String) : AddEditBudgetEvent()
    data class PeriodChanged(val value: String) : AddEditBudgetEvent()
    object ToggleRollover : AddEditBudgetEvent()
    data class AlertThresholdChanged(val value: Int) : AddEditBudgetEvent()
    object SaveBudget : AddEditBudgetEvent()
}
