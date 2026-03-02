package com.yourname.moneypilot.ui.features.reports

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.domain.usecase.analytics.CalculateKeyAnalyticsUseCase
import com.yourname.moneypilot.domain.usecase.analytics.KeyAnalytics
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.TreeMap
import javax.inject.Inject

enum class TimeRange { WEEKLY, MONTHLY, YEARLY }
enum class ReportType { EXPENSE, INCOME, CASH_FLOW }
enum class WeatherState { SUNNY, CLOUDY, RAINY, STORMY }

data class ReportState(
    val timeRange: TimeRange = TimeRange.MONTHLY,
    val reportType: ReportType = ReportType.EXPENSE,
    val totalAmount: Double = 0.0,
    val secondaryAmount: Double = 0.0,
    val categoryBreakdown: List<CategoryRank> = emptyList(),
    val chartData: Map<Int, Double> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(),
    val rangeStart: LocalDate = LocalDate.now(),
    val rangeEnd: LocalDate = LocalDate.now(),
    val weatherSummary: Map<WeatherState, Int> = emptyMap(),
    val weatherInsight: String = "",
    val keyAnalytics: KeyAnalytics = KeyAnalytics(), // Added
    val reflectionPrompts: List<String> = emptyList()
)

data class CategoryRank(
    val categoryId: Long?,
    val name: String,
    val icon: String,
    val amount: Double,
    val percentage: Float
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val calculateKeyAnalyticsUseCase: CalculateKeyAnalyticsUseCase // Added
) : BaseViewModel<ReportState>() {

    private val _reportState = MutableStateFlow(ReportState())
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private var reportJob: Job? = null

    init {
        loadReport()
    }

    fun onTimeRangeChange(range: TimeRange) {
        _reportState.update { it.copy(timeRange = range) }
        loadReport()
    }

    fun onReportTypeChange(type: ReportType) {
        _reportState.update { it.copy(reportType = type) }
        loadReport()
    }

    fun onDateChange(date: LocalDate) {
        _reportState.update { it.copy(selectedDate = date) }
        loadReport()
    }

    private fun loadReport() {
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            val currentState = _reportState.value
            val (start, end) = calculateRange(currentState.selectedDate, currentState.timeRange)
            
            val (prevStart, prevEnd) = calculatePreviousRange(currentState.selectedDate, currentState.timeRange)

            val currentFlow = transactionRepository.getTransactionsWithDetailsByDateRange(start, end)
            val prevFlow = transactionRepository.getTransactionsWithDetailsByDateRange(prevStart, prevEnd)

            combine(currentFlow, prevFlow) { current, previous ->
                Pair(current, previous)
            }.collect { (transactions, prevTransactions) ->
                val nonTransferTransactions = transactions.filter { it.transaction.type != TransactionType.Transfer }
                val prevNonTransfer = prevTransactions.filter { it.transaction.type != TransactionType.Transfer }

                val incomeSum = nonTransferTransactions.filter { it.transaction.type == TransactionType.Income }.sumOf { it.transaction.amount }
                val expenseSum = nonTransferTransactions.filter { it.transaction.type == TransactionType.Expense }.sumOf { it.transaction.amount }
                val prevExpenseSum = prevNonTransfer.filter { it.transaction.type == TransactionType.Expense }.sumOf { it.transaction.amount }
                
                val totalOutflow = expenseSum

                val totalDisplay = when (currentState.reportType) {
                    ReportType.INCOME -> incomeSum
                    ReportType.EXPENSE -> expenseSum
                    ReportType.CASH_FLOW -> incomeSum - totalOutflow
                }

                val listFiltered = when (currentState.reportType) {
                    ReportType.INCOME -> nonTransferTransactions.filter { it.transaction.type == TransactionType.Income }
                    ReportType.EXPENSE -> nonTransferTransactions.filter { it.transaction.type == TransactionType.Expense }
                    ReportType.CASH_FLOW -> nonTransferTransactions.filter { it.transaction.type != TransactionType.Income }
                }

                val ranks = listFiltered
                    .groupBy { it.transaction.categoryId }
                    .map { (id, items) ->
                        val sum = items.sumOf { it.transaction.amount }
                        val denom = when (currentState.reportType) {
                            ReportType.INCOME -> incomeSum
                            ReportType.EXPENSE -> expenseSum
                            ReportType.CASH_FLOW -> totalOutflow
                        }
                        CategoryRank(
                            categoryId = id,
                            name = items.first().category?.name ?: "Uncategorized",
                            icon = items.first().category?.icon ?: "❓",
                            amount = sum,
                            percentage = if (denom > 0) (sum / denom).toFloat() else 0f
                        )
                    }
                    .sortedByDescending { it.amount }

                val prompts = generateReflectionPrompts(expenseSum, prevExpenseSum, ranks, currentState.timeRange)
                val chartDataMap = generateSequentialChartData(listFiltered, currentState.timeRange, start.toLocalDate())
                val weatherData = calculateWeather(nonTransferTransactions, start.toLocalDate(), end.toLocalDate())
                val keyAnalytics = calculateKeyAnalyticsUseCase(nonTransferTransactions, ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()).toInt() + 1)

                _reportState.update { it.copy(
                    totalAmount = totalDisplay,
                    secondaryAmount = if (currentState.reportType == ReportType.CASH_FLOW) totalOutflow else 0.0,
                    categoryBreakdown = ranks,
                    chartData = chartDataMap,
                    rangeStart = start.toLocalDate(),
                    rangeEnd = end.toLocalDate(),
                    weatherSummary = weatherData.first,
                    weatherInsight = weatherData.second,
                    keyAnalytics = keyAnalytics,
                    reflectionPrompts = prompts
                ) }
                _uiState.value = ScreenState.Success(_reportState.value)
            }
        }
    }

    private fun generateReflectionPrompts(
        currentTotal: Double, 
        prevTotal: Double, 
        ranks: List<CategoryRank>,
        range: TimeRange
    ): List<String> {
        val prompts = mutableListOf<String>()
        val periodName = when(range) {
            TimeRange.WEEKLY -> "last week"
            TimeRange.MONTHLY -> "last month"
            TimeRange.YEARLY -> "last year"
        }

        if (prevTotal > 0) {
            val diff = ((currentTotal - prevTotal) / prevTotal) * 100
            if (diff > 10) {
                prompts.add("Consumption is up ${diff.toInt()}% compared to $periodName. Reflect on what changed.")
            } else if (diff < -10) {
                prompts.add("Great job! You've reduced spending by ${Math.abs(diff.toInt())}% vs $periodName.")
            }
        }

        ranks.firstOrNull()?.let { top ->
            if (top.percentage > 0.4f) {
                prompts.add("${top.icon} ${top.name} is dominating your outflows at ${(top.percentage * 100).toInt()}%.")
            }
        }

        val income = _reportState.value.totalAmount
        if (income > 0 && currentTotal / income > 0.9) {
            prompts.add("Warning: High burn rate. You've consumed over 90% of your income this period.")
        }

        return prompts
    }

    private fun calculateWeather(transactions: List<TransactionWithDetails>, start: LocalDate, end: LocalDate): Pair<Map<WeatherState, Int>, String> {
        val dailyNet = transactions.groupBy { it.transaction.dateTime.toLocalDate() }
            .mapValues { entry ->
                entry.value.sumOf { 
                    when (it.transaction.type) {
                        TransactionType.Income -> it.transaction.amount
                        else -> -it.transaction.amount 
                    }
                }
            }

        val summary = mutableMapOf<WeatherState, Int>()
        var currentDate = start
        while (!currentDate.isAfter(end)) {
            val net = dailyNet[currentDate] ?: 0.0
            val state = when {
                net > 100 -> WeatherState.SUNNY
                net < -1000 -> WeatherState.STORMY
                net < 0 -> WeatherState.RAINY
                else -> WeatherState.CLOUDY
            }
            summary[state] = summary.getOrDefault(state, 0) + 1
            currentDate = currentDate.plusDays(1)
        }

        val sunnyDays = summary.getOrDefault(WeatherState.SUNNY, 0)
        val rainyDays = summary.getOrDefault(WeatherState.RAINY, 0) + summary.getOrDefault(WeatherState.STORMY, 0)
        
        val insight = when {
            sunnyDays > rainyDays -> "Predicting a clear horizon: Inflows are outpacing outflows."
            rainyDays > sunnyDays -> "Caution: High frequency of outflows detected."
            else -> "A balanced financial climate."
        }

        return Pair(summary, insight)
    }

    private fun generateSequentialChartData(
        transactions: List<TransactionWithDetails>, 
        range: TimeRange,
        startDate: LocalDate
    ): Map<Int, Double> {
        val rawMap = transactions.groupBy { 
            when (range) {
                TimeRange.WEEKLY -> it.transaction.dateTime.dayOfWeek.value
                TimeRange.MONTHLY -> it.transaction.dateTime.dayOfMonth
                TimeRange.YEARLY -> it.transaction.dateTime.monthValue
            }
        }.mapValues { entry -> entry.value.sumOf { it.transaction.amount } }

        val sortedMap = TreeMap<Int, Double>()
        when (range) {
            TimeRange.WEEKLY -> (1..7).forEach { sortedMap[it] = rawMap[it] ?: 0.0 }
            TimeRange.MONTHLY -> {
                val daysInMonth = startDate.lengthOfMonth()
                (1..daysInMonth).forEach { sortedMap[it] = rawMap[it] ?: 0.0 }
            }
            TimeRange.YEARLY -> (1..12).forEach { sortedMap[it] = rawMap[it] ?: 0.0 }
        }
        return sortedMap
    }

    private fun calculateRange(date: LocalDate, range: TimeRange): Pair<LocalDateTime, LocalDateTime> {
        return when (range) {
            TimeRange.WEEKLY -> {
                val start = date.minusDays(date.dayOfWeek.value.toLong() - 1).atStartOfDay()
                Pair(start, start.plusDays(7).minusNanos(1))
            }
            TimeRange.MONTHLY -> {
                val start = date.withDayOfMonth(1).atStartOfDay()
                Pair(start, start.plusMonths(1).minusNanos(1))
            }
            TimeRange.YEARLY -> {
                val start = date.withDayOfYear(1).atStartOfDay()
                Pair(start, start.plusYears(1).minusNanos(1))
            }
        }
    }

    private fun calculatePreviousRange(date: LocalDate, range: TimeRange): Pair<LocalDateTime, LocalDateTime> {
        return when (range) {
            TimeRange.WEEKLY -> calculateRange(date.minusWeeks(1), range)
            TimeRange.MONTHLY -> calculateRange(date.minusMonths(1), range)
            TimeRange.YEARLY -> calculateRange(date.minusYears(1), range)
        }
    }
}
