package com.yourname.moneypilot.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class DashboardHubState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentMonth: YearMonth = YearMonth.now()
)

@HiltViewModel
class DashboardHubViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    
    private val _hubState = MutableStateFlow(DashboardHubState())
    val state: StateFlow<DashboardHubState> = _hubState.asStateFlow()

    init {
        // Observe accounts and month changes
        combine(
            accountRepository.getAllAccounts(),
            _currentMonth
        ) { accounts, month ->
            Pair(accounts, month)
        }.onEach { (accounts, month) ->
            updateTotals(accounts, month)
        }.launchIn(viewModelScope)
    }

    private suspend fun updateTotals(accounts: List<AccountEntity>, month: YearMonth) {
        val start = month.atDay(1).atStartOfDay()
        val end = month.atEndOfMonth().atTime(LocalTime.MAX)
        
        // Use repository to get global totals for the month
        val income = transactionRepository.getTotalSumByType("INCOME", start, end)
        val expense = transactionRepository.getTotalSumByType("EXPENSE", start, end)

        _hubState.value = DashboardHubState(
            accounts = accounts,
            totalBalance = accounts.sumOf { it.currentBalance },
            monthlyIncome = income,
            monthlyExpense = expense,
            currentMonth = month
        )
    }

    fun onMonthChange(month: YearMonth) {
        _currentMonth.value = month
    }
}
