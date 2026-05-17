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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.Year
import javax.inject.Inject

data class DashboardHubState(
    val wallets: List<WalletEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val netSurplus: Double = 0.0,
    val savingsRate: Double = 0.0,
    val yearlyIncome: Double = 0.0,
    val yearlyExpense: Double = 0.0,
    val currentMonth: YearMonth = YearMonth.now(),
    val currentYear: Year = Year.now(),
    val selectedDate: LocalDate = LocalDate.now()
)

@HiltViewModel
class DashboardHubViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _currentYear = MutableStateFlow(Year.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    
    private val _hubState = MutableStateFlow(DashboardHubState())
    val state: StateFlow<DashboardHubState> = _hubState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                walletRepository.getAllWallets(),
                _currentMonth,
                _currentYear,
                _selectedDate
            ) { wallets, month, year, selectedDate ->
                updateTotals(wallets, month, year, selectedDate)
            }.collect()
        }
    }

    private suspend fun updateTotals(wallets: List<WalletEntity>, month: YearMonth, year: Year, selectedDate: LocalDate) {
        // Monthly Totals
        val mStart = month.atDay(1).atStartOfDay()
        val mEnd = month.atEndOfMonth().atTime(LocalTime.MAX)
        val mIncome = transactionRepository.getTotalSumByType(TransactionType.Income, mStart, mEnd) ?: 0.0
        val mExpense = transactionRepository.getTotalSumByType(TransactionType.Expense, mStart, mEnd) ?: 0.0
        
        val surplus = mIncome - mExpense
        val rate = if (mIncome > 0) (surplus / mIncome) * 100.0 else 0.0

        // Yearly Totals
        val yStart = year.atDay(1).atStartOfDay()
        val yEnd = year.atMonth(12).atEndOfMonth().atTime(LocalTime.MAX)
        val yIncome = transactionRepository.getTotalSumByType(TransactionType.Income, yStart, yEnd) ?: 0.0
        val yExpense = transactionRepository.getTotalSumByType(TransactionType.Expense, yStart, yEnd) ?: 0.0

        _hubState.value = DashboardHubState(
            wallets = wallets,
            totalBalance = wallets.sumOf { it.currentBalance },
            monthlyIncome = mIncome,
            monthlyExpense = mExpense,
            netSurplus = surplus,
            savingsRate = rate,
            yearlyIncome = yIncome,
            yearlyExpense = yExpense,
            currentMonth = month,
            currentYear = year,
            selectedDate = selectedDate
        )
    }

    fun onMonthChange(month: YearMonth) {
        _currentMonth.value = month
        // Reset selected date to 1st of that month if it's not the current month
        if (month != YearMonth.now()) {
            _selectedDate.value = month.atDay(1)
        } else {
            _selectedDate.value = LocalDate.now()
        }
    }

    fun onYearChange(year: Year) {
        _currentYear.value = year
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }
}
