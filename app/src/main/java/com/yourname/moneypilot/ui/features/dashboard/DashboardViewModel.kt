package com.yourname.moneypilot.ui.features.dashboard

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.domain.usecase.transaction.CalculateMonthlySummaryUseCase
import com.yourname.moneypilot.domain.usecase.transaction.MonthlySummary
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class DashboardState(
    val totalBalance: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val monthlySummary: MonthlySummary? = null,
    val activeBudgets: List<BudgetEntity> = emptyList(),
    val highPriorityGoals: List<GoalEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val calculateMonthlySummaryUseCase: CalculateMonthlySummaryUseCase
) : BaseViewModel<DashboardState>() {

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val now = LocalDateTime.now()
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            combine(
                accountRepository.getAllAccounts(),
                transactionRepository.getAllTransactions(),
                calculateMonthlySummaryUseCase(now.monthValue, now.year),
                budgetRepository.getActiveBudgets(LocalDate.now()),
                goalRepository.getAllGoals()
            ) { accounts, transactions, summary, budgets, goals ->
                DashboardState(
                    totalBalance = accounts.sumOf { it.currentBalance },
                    recentTransactions = transactions.take(5),
                    monthlySummary = summary,
                    activeBudgets = budgets.take(3),
                    highPriorityGoals = goals.filter { it.status == "ACTIVE" }.sortedBy { it.priority }.take(2)
                )
            }.collect { state ->
                _uiState.value = ScreenState.Success(state)
            }
        }
    }
}
