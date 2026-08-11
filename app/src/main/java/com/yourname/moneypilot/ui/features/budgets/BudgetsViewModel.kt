package com.yourname.moneypilot.ui.features.budgets

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.domain.usecase.budget.BudgetAdvisory
import com.yourname.moneypilot.domain.usecase.budget.GetBudgetAdvisoryUseCase
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetsState(
    val budgets: List<BudgetWithDetails> = emptyList(),
    val advisories: List<BudgetAdvisory> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val getBudgetAdvisoryUseCase: GetBudgetAdvisoryUseCase
) : BaseViewModel<BudgetsState>() {

    private val _state = MutableStateFlow(BudgetsState())
    val state = _state.asStateFlow()

    init {
        loadData()
        loadAdvisories()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.map { it.selectedDate }.distinctUntilChanged().collectLatest { date ->
                _uiState.value = ScreenState.Loading
                budgetRepository.refreshActiveBudgets(date)
                
                budgetRepository.getActiveBudgetsWithDetails(date).collect { list ->
                    val totalB = list.sumOf { it.budget.amount }
                    val totalS = list.sumOf { it.budget.spentAmount }
                    
                    _state.update { it.copy(
                        budgets = list,
                        totalBudget = totalB,
                        totalSpent = totalS
                    ) }
                    
                    if (list.isEmpty()) {
                        _uiState.value = ScreenState.Empty
                    } else {
                        _uiState.value = ScreenState.Success(_state.value)
                    }
                }
            }
        }
    }

    fun onDateChange(newDate: LocalDate) {
        _state.update { it.copy(selectedDate = newDate) }
    }

    private fun loadAdvisories() {
        viewModelScope.launch {
            val advisories = getBudgetAdvisoryUseCase()
            val currentState = (uiState.value as? ScreenState.Success)?.data ?: BudgetsState()
            _uiState.value = ScreenState.Success(currentState.copy(advisories = advisories))
        }
    }
}
