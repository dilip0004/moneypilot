package com.yourname.moneypilot.ui.features.calendar

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.domain.usecase.transaction.GetDailyFinancialSummaryUseCase
import com.yourname.moneypilot.domain.usecase.transaction.DailySummary
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val dailySummaries: Map<LocalDate, DailySummary> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateTransactions: List<TransactionEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val getDailyFinancialSummaryUseCase: GetDailyFinancialSummaryUseCase
) : BaseViewModel<CalendarState>() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                _currentMonth,
                _selectedDate,
                _currentMonth.flatMapLatest { month ->
                    getDailyFinancialSummaryUseCase(month.monthValue, month.year)
                },
                _currentMonth.flatMapLatest { month ->
                    val start = month.atDay(1).atStartOfDay()
                    val end = month.atEndOfMonth().atTime(23, 59, 59)
                    transactionRepository.getTransactionsByDateRange(start, end)
                }
            ) { month, selectedDate, summaries, allTransactions ->
                val dayTransactions = allTransactions.filter { it.date.toLocalDate() == selectedDate }
                CalendarState(
                    currentMonth = month,
                    dailySummaries = summaries,
                    selectedDate = selectedDate,
                    selectedDateTransactions = dayTransactions
                )
            }.collect { newState ->
                _state.value = newState
                _uiState.value = ScreenState.Success(newState)
            }
        }
    }

    fun onMonthChange(month: YearMonth) {
        _currentMonth.value = month
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }
}
