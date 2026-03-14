package com.yourname.moneypilot.ui.features.budgets

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetsState(
    val budgets: List<BudgetWithDetails> = emptyList()
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : BaseViewModel<BudgetsState>() {

    init {
        refreshAndLoadBudgets()
    }

    private fun refreshAndLoadBudgets() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            // Self-healing: Recalculate spent amounts from ledger on load
            budgetRepository.refreshActiveBudgets(LocalDate.now())
            
            budgetRepository.getActiveBudgetsWithDetails(LocalDate.now()).collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(BudgetsState(list))
                }
            }
        }
    }
}
