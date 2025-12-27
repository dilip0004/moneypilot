package com.yourname.moneypilot.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    
    val state: StateFlow<DashboardHubState> = combine(
        accountRepository.getAllAccounts(),
        _currentMonth
    ) { accounts, month ->
        val start = month.atDay(1).atStartOfDay()
        val end = month.atEndOfMonth().atTime(LocalTime.MAX)
        
        // Use the repository method we built earlier for budget sync
        val income = 0.0 // Placeholder for sum query if not exists
        val expense = transactionRepository.getCategoryExpenseSum(-1L, start, end) // Need to adjust repository for total sum

        DashboardHubState(
            accounts = accounts,
            totalBalance = accounts.sumOf { it.currentBalance },
            monthlyIncome = 0.0, // Calculate from transactions
            monthlyExpense = expense,
            currentMonth = month
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardHubState())

    fun onMonthChange(month: YearMonth) {
        _currentMonth.value = month
    }
}
