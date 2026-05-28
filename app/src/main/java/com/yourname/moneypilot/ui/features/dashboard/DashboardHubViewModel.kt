package com.yourname.moneypilot.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.data.repository.*
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
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val totalNetWorth: Double = 0.0,
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
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val investmentRepository: InvestmentRepository,
    private val loanRepository: LoanRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _currentYear = MutableStateFlow(Year.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    
    private val _hubState = MutableStateFlow(DashboardHubState())
    val state: StateFlow<DashboardHubState> = _hubState.asStateFlow()

    init {
        viewModelScope.launch {
            // Group 1: Data Pillars
            val dataFlow = combine(
                walletRepository.getAllWallets(),
                investmentRepository.getAllInvestments(),
                loanRepository.getAllLoans(),
                goalRepository.getAllGoals(),
                preferencesRepository.userPreferencesFlow
            ) { wallets, investments, loans, goals, prefs ->
                DataSnapshot(wallets, investments, loans, goals, prefs)
            }

            // Group 2: Time Controls
            val timeFlow = combine(_currentMonth, _currentYear, _selectedDate) { month, year, date ->
                TimeSnapshot(month, year, date)
            }

            // Final Merge
            dataFlow.combine(timeFlow) { data, time ->
                calculateState(data, time)
            }.collect { newState ->
                _hubState.value = newState
            }
        }
    }

    private suspend fun calculateState(data: DataSnapshot, time: TimeSnapshot): DashboardHubState {
        val month = time.currentMonth
        val year = time.currentYear

        // 1. Monthly Totals
        val mStart = month.atDay(1).atStartOfDay()
        val mEnd = month.atEndOfMonth().atTime(LocalTime.MAX)
        val mIncome = transactionRepository.getTotalSumByType(TransactionType.Income, mStart, mEnd) ?: 0.0
        val mExpense = transactionRepository.getTotalSumByType(TransactionType.Expense, mStart, mEnd) ?: 0.0
        
        val surplus = mIncome - mExpense
        val rate = if (mIncome > 0) (surplus / mIncome) * 100.0 else 0.0

        // 2. Yearly Totals
        val yStart = year.atDay(1).atStartOfDay()
        val yEnd = year.atMonth(12).atEndOfMonth().atTime(LocalTime.MAX)
        val yIncome = transactionRepository.getTotalSumByType(TransactionType.Income, yStart, yEnd) ?: 0.0
        val yExpense = transactionRepository.getTotalSumByType(TransactionType.Expense, yStart, yEnd) ?: 0.0

        // 3. Net Worth Calculation
        val activeWallets = data.wallets.filter { !it.isArchived }
        val walletAssets = activeWallets.filter { it.type != "CREDIT" && it.type != "CREDIT_CARD" }.sumOf { it.currentBalance }
        val walletLiabilities = activeWallets.filter { it.type == "CREDIT" || it.type == "CREDIT_CARD" }.sumOf { kotlin.math.abs(it.currentBalance) }
        
        val investmentAssets = data.investments.sumOf { it.quantity * it.currentPrice }
        val loanLiabilities = data.loans.filter { it.status == "ACTIVE" && it.type == "BORROWED" }.sumOf { it.currentBalance }
        
        val goalAssets = if (data.prefs.includeGoalsInNetWorth) {
            data.goals.filter { it.status == "ACTIVE" }.sumOf { it.currentAmount }
        } else 0.0

        val totalAssets = walletAssets + investmentAssets + goalAssets
        val totalLiabilities = walletLiabilities + loanLiabilities

        return DashboardHubState(
            wallets = activeWallets,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            totalNetWorth = totalAssets - totalLiabilities,
            monthlyIncome = mIncome,
            monthlyExpense = mExpense,
            netSurplus = surplus,
            savingsRate = rate,
            yearlyIncome = yIncome,
            yearlyExpense = yExpense,
            currentMonth = month,
            currentYear = year,
            selectedDate = time.selectedDate
        )
    }

    private data class DataSnapshot(
        val wallets: List<WalletEntity>,
        val investments: List<InvestmentEntity>,
        val loans: List<LoanEntity>,
        val goals: List<GoalEntity>,
        val prefs: com.yourname.moneypilot.data.local.preferences.UserPreferences
    )

    private data class TimeSnapshot(
        val currentMonth: YearMonth,
        val currentYear: Year,
        val selectedDate: LocalDate
    )

    fun onMonthChange(month: YearMonth) {
        _currentMonth.value = month
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
