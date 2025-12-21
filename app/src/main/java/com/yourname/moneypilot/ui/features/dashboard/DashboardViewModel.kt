package com.yourname.moneypilot.ui.features.dashboard

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.domain.usecase.transaction.CalculateMonthlySummaryUseCase
import com.yourname.moneypilot.domain.usecase.transaction.MonthlySummary
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class DashboardState(
    val totalBalance: Double = 0.0,
    val recentTransactions: List<com.yourname.moneypilot.data.local.database.entities.TransactionEntity> = emptyList(),
    val monthlySummary: MonthlySummary? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
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
                calculateMonthlySummaryUseCase(now.monthValue, now.year)
            ) { accounts, transactions, summary ->
                val balance = accounts.sumOf { it.currentBalance }
                val recent = transactions.take(5)
                DashboardState(balance, recent, summary)
            }.collect { state ->
                _uiState.value = ScreenState.Success(state)
            }
        }
    }
}
