package com.yourname.moneypilot.ui.features.reports

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    val savingsPercentage: Float = 0f,
    val dailyAverage: Double = 0.0,
    val transactionCount: Int = 0
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
    private val transactionRepository: TransactionRepository
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
            
            transactionRepository.getTransactionsWithCategoryByDateRange(start, end).collect { transactions ->
                val typeString = if (currentState.reportType == ReportType.INCOME) "INCOME" else "EXPENSE"
                
                val confirmedTransactions = transactions.filter { it.transaction.type != "TRANSFER" }

                val incomeSum = confirmedTransactions.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount }
                val expenseSum = confirmedTransactions.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                
                val totalDisplay = when (currentState.reportType) {
                    ReportType.INCOME -> incomeSum
                    ReportType.EXPENSE -> expenseSum
                    ReportType.CASH_FLOW -> incomeSum - expenseSum
                }

                val listFiltered = if (currentState.reportType == ReportType.CASH_FLOW) {
                    confirmedTransactions.filter { it.transaction.type == "EXPENSE" } 
                } else {
                    confirmedTransactions.filter { it.transaction.type == typeString }
                }

                val ranks = listFiltered
                    .groupBy { it.transaction.categoryId }
                    .map { (id, items) ->
                        val sum = items.sumOf { it.transaction.amount }
                        val denom = if (currentState.reportType == ReportType.INCOME) incomeSum else expenseSum
                        CategoryRank(
                            categoryId = id,
                            name = items.first().category?.name ?: "Uncategorized",
                            icon = items.first().category?.icon ?: "❓",
                            amount = sum,
                            percentage = if (denom > 0) (sum / denom).toFloat() else 0f
                        )
                    }
                    .sortedByDescending { it.amount }

                val chartDataMap = generateSequentialChartData(listFiltered, currentState.timeRange, start.toLocalDate())
                
                val weatherData = calculateWeather(confirmedTransactions, start.toLocalDate(), end.toLocalDate())

                // New Insight Calculations
                val savingsPct = if (incomeSum > 0) ((incomeSum - expenseSum) / incomeSum).toFloat() else 0f
                val days = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1
                val dailyAvg = if (days > 0) expenseSum / days else 0.0
                val count = confirmedTransactions.size

                _reportState.update { it.copy(
                    totalAmount = totalDisplay,
                    secondaryAmount = if (currentState.reportType == ReportType.CASH_FLOW) expenseSum else 0.0,
                    categoryBreakdown = ranks,
                    chartData = chartDataMap,
                    rangeStart = start.toLocalDate(),
                    rangeEnd = end.toLocalDate(),
                    weatherSummary = weatherData.first,
                    weatherInsight = weatherData.second,
                    savingsPercentage = savingsPct,
                    dailyAverage = dailyAvg,
                    transactionCount = count
                ) }
                _uiState.value = ScreenState.Success(_reportState.value)
            }
        }
    }

    private fun calculateWeather(transactions: List<TransactionWithCategory>, start: LocalDate, end: LocalDate): Pair<Map<WeatherState, Int>, String> {
        val dailyNet = transactions.groupBy { it.transaction.date.toLocalDate() }
            .mapValues { entry ->
                entry.value.sumOf { if (it.transaction.type == "INCOME") it.transaction.amount else -it.transaction.amount }
            }

        val summary = mutableMapOf<WeatherState, Int>()
        var currentDate = start
        while (!currentDate.isAfter(end)) {
            val net = dailyNet[currentDate] ?: 0.0
            val state = when {
                net > 100 -> WeatherState.SUNNY
                net < -500 -> WeatherState.STORMY
                net < 0 -> WeatherState.RAINY
                else -> WeatherState.CLOUDY
            }
            summary[state] = summary.getOrDefault(state, 0) + 1
            currentDate = currentDate.plusDays(1)
        }

        val sunnyDays = summary.getOrDefault(WeatherState.SUNNY, 0)
        val rainyDays = summary.getOrDefault(WeatherState.RAINY, 0) + summary.getOrDefault(WeatherState.STORMY, 0)
        
        val insight = when {
            sunnyDays > rainyDays -> "The financial climate was predominantly clear, with multiple days of positive net flow."
            rainyDays > sunnyDays -> "Increased activity led to a period of frequent outflows, similar to a rainy season."
            else -> "A balanced month with stable conditions across most days."
        }

        return Pair(summary, insight)
    }

    private fun generateSequentialChartData(
        transactions: List<TransactionWithCategory>, 
        range: TimeRange,
        startDate: LocalDate
    ): Map<Int, Double> {
        val rawMap = transactions.groupBy { 
            when (range) {
                TimeRange.WEEKLY -> it.transaction.date.dayOfWeek.value
                TimeRange.MONTHLY -> it.transaction.date.dayOfMonth
                TimeRange.YEARLY -> it.transaction.date.monthValue
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
}
