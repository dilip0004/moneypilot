package com.yourname.moneypilot.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class DashboardHubState(
    val wallets: List<WalletEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentMonth: YearMonth = YearMonth.now()
)

@HiltViewModel
class DashboardHubViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    
    private val _hubState = MutableStateFlow(DashboardHubState())
    val state: StateFlow<DashboardHubState> = _hubState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                walletRepository.getAllWallets(),
                _currentMonth
            ) { wallets, month ->
                Pair(wallets, month)
            }.collect { (wallets, month) ->
                updateTotals(wallets, month)
            }
        }
    }

    private suspend fun updateTotals(wallets: List<WalletEntity>, month: YearMonth) {
        val start = month.atDay(1).atStartOfDay()
        val end = month.atEndOfMonth().atTime(LocalTime.MAX)
        
        val income = transactionRepository.getTotalSumByType(TransactionType.Income, start, end) ?: 0.0
        val expense = transactionRepository.getTotalSumByType(TransactionType.Expense, start, end) ?: 0.0

        _hubState.value = DashboardHubState(
            wallets = wallets,
            totalBalance = wallets.sumOf { it.currentBalance },
            monthlyIncome = income,
            monthlyExpense = expense,
            currentMonth = month
        )
    }

    fun onMonthChange(month: YearMonth) {
        _currentMonth.value = month
    }
}
