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
    val transactionCount: Int = 0,
    val reflectionPrompts: List<String> = emptyList() // V2 Reflection triggers
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
            
            // For historical comparison, we fetch current and previous periods
            val (prevStart, prevEnd) = calculatePreviousRange(currentState.selectedDate, currentState.timeRange)

            val currentFlow = transactionRepository.getTransactionsWithCategoryByDateRange(start, end)
            val prevFlow = transactionRepository.getTransactionsWithCategoryByDateRange(prevStart, prevEnd)

            combine(currentFlow, prevFlow) { current, previous ->
                Pair(current, previous)
            }.collect { (transactions, prevTransactions) ->
                val nonTransferTransactions = transactions.filter { it.transaction.type != "TRANSFER" }
                val prevNonTransfer = prevTransactions.filter { it.transaction.type != "TRANSFER" }

                val incomeSum = nonTransferTransactions.filter { it.transaction.type == "INCOME" }.sumOf { it.transaction.amount }
                val expenseSum = nonTransferTransactions.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                val prevExpenseSum = prevNonTransfer.filter { it.transaction.type == "EXPENSE" }.sumOf { it.transaction.amount }
                
                val loanRepaymentSum = nonTransferTransactions.filter { it.transaction.type == "LOAN_REPAYMENT" }.sumOf { it.transaction.amount }
                val goalContributionSum = nonTransferTransactions.filter { it.transaction.type == "GOAL_CONTRIBUTION" }.sumOf { it.transaction.amount }
                val totalOutflow = expenseSum + loanRepaymentSum + goalContributionSum

                val totalDisplay = when (currentState.reportType) {
                    ReportType.INCOME -> incomeSum
                    ReportType.EXPENSE -> expenseSum
                    ReportType.CASH_FLOW -> incomeSum - totalOutflow
                }

                val listFiltered = when (currentState.reportType) {
                    ReportType.INCOME -> nonTransferTransactions.filter { it.transaction.type == "INCOME" }
                    ReportType.EXPENSE -> nonTransferTransactions.filter { it.transaction.type == "EXPENSE" }
                    ReportType.CASH_FLOW -> nonTransferTransactions.filter { it.transaction.type != "INCOME" }
                }

                val ranks = listFiltered
                    .groupBy { 
                        if (it.transaction.type == "LOAN_REPAYMENT") -100L
                        else if (it.transaction.type == "GOAL_CONTRIBUTION") -200L
                        else it.transaction.categoryId 
                    }
                    .map { (id, items) ->
                        val sum = items.sumOf { it.transaction.amount }
                        val denom = when (currentState.reportType) {
                            ReportType.INCOME -> incomeSum
                            ReportType.EXPENSE -> expenseSum
                            ReportType.CASH_FLOW -> totalOutflow
                        }
                        CategoryRank(
                            categoryId = if (id != null && id < 0L) null else id,
                            name = when (id) {
                                -100L -> "Loan Repayment"
                                -200L -> "Savings Contribution"
                                else -> items.first().category?.name ?: "Uncategorized"
                            },
                            icon = when (id) {
                                -100L -> "💸"
                                -200L -> "🎯"
                                else -> items.first().category?.icon ?: "❓"
                            },
                            amount = sum,
                            percentage = if (denom > 0) (sum / denom).toFloat() else 0f
                        )
                    }
                    .sortedByDescending { it.amount }

                val prompts = generateReflectionPrompts(expenseSum, prevExpenseSum, ranks, currentState.timeRange)
                val chartDataMap = generateSequentialChartData(listFiltered, currentState.timeRange, start.toLocalDate())
                val weatherData = calculateWeather(nonTransferTransactions, start.toLocalDate(), end.toLocalDate())

                val savingsPct = if (incomeSum > 0) ((incomeSum - totalOutflow) / incomeSum).toFloat() else 0f
                val days = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1
                val dailyAvg = if (days > 0) expenseSum / days else 0.0
                val count = nonTransferTransactions.size

                _reportState.update { it.copy(
                    totalAmount = totalDisplay,
                    secondaryAmount = if (currentState.reportType == ReportType.CASH_FLOW) totalOutflow else 0.0,
                    categoryBreakdown = ranks,
                    chartData = chartDataMap,
                    rangeStart = start.toLocalDate(),
                    rangeEnd = end.toLocalDate(),
                    weatherSummary = weatherData.first,
                    weatherInsight = weatherData.second,
                    savingsPercentage = savingsPct,
                    dailyAverage = dailyAvg,
                    transactionCount = count,
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

        // 1. Total Spending Trigger
        if (prevTotal > 0) {
            val diff = ((currentTotal - prevTotal) / prevTotal) * 100
            if (diff > 10) {
                prompts.add("Consumption is up ${diff.toInt()}% compared to $periodName. Reflect on what changed.")
            } else if (diff < -10) {
                prompts.add("Great job! You've reduced spending by ${Math.abs(diff.toInt())}% vs $periodName.")
            }
        }

        // 2. Category Dominance Trigger
        ranks.firstOrNull()?.let { top ->
            if (top.percentage > 0.4f) {
                prompts.add("${top.icon} ${top.name} is dominating your outflows at ${(top.percentage * 100).toInt()}%.")
            }
        }

        // 3. Low Savings Trigger
        val income = _reportState.value.totalAmount
        if (income > 0 && currentTotal / income > 0.9) {
            prompts.add("Warning: High burn rate. You've consumed over 90% of your income this period.")
        }

        return prompts
    }

    private fun calculateWeather(transactions: List<TransactionWithCategory>, start: LocalDate, end: LocalDate): Pair<Map<WeatherState, Int>, String> {
        val dailyNet = transactions.groupBy { it.transaction.date.toLocalDate() }
            .mapValues { entry ->
                entry.value.sumOf { 
                    when (it.transaction.type) {
                        "INCOME" -> it.transaction.amount
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

    private fun calculatePreviousRange(date: LocalDate, range: TimeRange): Pair<LocalDateTime, LocalDateTime> {
        return when (range) {
            TimeRange.WEEKLY -> calculateRange(date.minusWeeks(1), range)
            TimeRange.MONTHLY -> calculateRange(date.minusMonths(1), range)
            TimeRange.YEARLY -> calculateRange(date.minusYears(1), range)
        }
    }
}
