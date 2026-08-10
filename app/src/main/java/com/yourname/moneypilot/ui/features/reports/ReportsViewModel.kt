package com.yourname.moneypilot.ui.features.reports

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.domain.usecase.analytics.*
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
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
    val keyAnalytics: KeyAnalytics = KeyAnalytics(),
    val reflectionPrompts: List<String> = emptyList(),
    val anomalies: List<Anomaly> = emptyList(),
    val showSpendingInsights: Boolean = false,
    val showBurnRateAlerts: Boolean = false,
    val showCategoryAlerts: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getReportDataUseCase: GetReportDataUseCase,
    private val generateReflectionPromptsUseCase: GenerateReflectionPromptsUseCase,
    private val calculateFinancialWeatherUseCase: CalculateFinancialWeatherUseCase,
    private val calculateKeyAnalyticsUseCase: CalculateKeyAnalyticsUseCase,
    private val detectAnomaliesUseCase: DetectAnomaliesUseCase
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

            val currentFlow = getReportDataUseCase(start, end, currentState.reportType)
            val prevFlow = getReportDataUseCase(prevStart, prevEnd, currentState.reportType)
            val prefFlow = userPreferencesRepository.userPreferencesFlow

            combine(currentFlow, prevFlow, prefFlow) { current, previous, prefs ->
                Triple(current, previous, prefs)
            }.collect { (currentData, prevData, prefs) ->
                val chartDataMap = generateSequentialChartData(currentData.filteredTransactions, currentState.timeRange, start.toLocalDate())
                
                val (weatherSummary, weatherInsight) = calculateFinancialWeatherUseCase(
                    currentData.filteredTransactions, start.toLocalDate(), end.toLocalDate()
                )
                
                val prompts = if (prefs.showSpendingInsights) {
                    generateReflectionPromptsUseCase(
                        currentTotal = currentData.totalOutflow,
                        prevTotal = prevData.totalOutflow,
                        ranks = currentData.categoryBreakdown,
                        range = currentState.timeRange,
                        incomeTotal = currentData.totalAmount,
                        transactions = currentData.filteredTransactions
                    )
                } else emptyList()

                val daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()).toInt() + 1
                val keyAnalytics = calculateKeyAnalyticsUseCase(currentData.filteredTransactions, daysInPeriod)
                
                val anomalies = if (prefs.showSpendingInsights) detectAnomaliesUseCase(currentData.filteredTransactions) else emptyList()

                _reportState.update { it.copy(
                    totalAmount = currentData.totalAmount,
                    secondaryAmount = if (currentState.reportType == ReportType.CASH_FLOW) currentData.totalOutflow else 0.0,
                    categoryBreakdown = currentData.categoryBreakdown,
                    chartData = chartDataMap,
                    rangeStart = start.toLocalDate(),
                    rangeEnd = end.toLocalDate(),
                    weatherSummary = weatherSummary,
                    weatherInsight = weatherInsight,
                    keyAnalytics = keyAnalytics,
                    reflectionPrompts = prompts,
                    anomalies = anomalies,
                    showSpendingInsights = prefs.showSpendingInsights,
                    showBurnRateAlerts = prefs.showBurnRateAlerts,
                    showCategoryAlerts = prefs.showCategoryAlerts
                ) }
                _uiState.value = ScreenState.Success(_reportState.value)
            }
        }
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

data class CategoryRank(
    val categoryId: Long?,
    val name: String,
    val icon: String,
    val amount: Double,
    val percentage: Float
)
