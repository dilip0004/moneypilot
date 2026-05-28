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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetsState(
    val budgets: List<BudgetWithDetails> = emptyList(),
    val advisories: List<BudgetAdvisory> = emptyList()
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val getBudgetAdvisoryUseCase: GetBudgetAdvisoryUseCase
) : BaseViewModel<BudgetsState>() {

    init {
        refreshAndLoadBudgets()
        loadAdvisories()
    }

    private fun refreshAndLoadBudgets() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            budgetRepository.refreshActiveBudgets(LocalDate.now())
            
            budgetRepository.getActiveBudgetsWithDetails(LocalDate.now()).collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    val currentState = (uiState.value as? ScreenState.Success)?.data ?: BudgetsState()
                    _uiState.value = ScreenState.Success(currentState.copy(budgets = list))
                }
            }
        }
    }

    private fun loadAdvisories() {
        viewModelScope.launch {
            val advisories = getBudgetAdvisoryUseCase()
            val currentState = (uiState.value as? ScreenState.Success)?.data ?: BudgetsState()
            _uiState.value = ScreenState.Success(currentState.copy(advisories = advisories))
        }
    }
}
