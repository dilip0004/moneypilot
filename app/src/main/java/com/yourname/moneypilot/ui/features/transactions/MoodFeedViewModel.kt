package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class FinancialMood {
    BULLISH, NEUTRAL, BEARISH
}

data class DayMoodData(
    val date: LocalDate,
    val dayName: String,
    val formattedDate: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val transactionCount: Int,
    val mood: FinancialMood,
    val transactions: List<TransactionWithDetails>,
    val isToday: Boolean
)

data class MoodFeedState(
    val days: List<DayMoodData> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now()
)

@HiltViewModel
class MoodFeedViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : BaseViewModel<MoodFeedState>() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    init {
        loadMoodData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadMoodData() {
        viewModelScope.launch {
            _currentMonth.flatMapLatest { month ->
                transactionRepository.getAllTransactionsWithDetails().map { transactions ->
                    val monthFiltered = transactions.filter {
                        val txDate = it.transaction.dateTime.toLocalDate()
                        txDate.year == month.year && txDate.month == month.month
                    }

                    val grouped = monthFiltered.groupBy { it.transaction.dateTime.toLocalDate() }
                        .map { (date, items) ->
                            val income = items.filter { it.transaction.type == TransactionType.Income }.sumOf { it.transaction.amount }
                            val expense = items.filter { it.transaction.type == TransactionType.Expense }.sumOf { it.transaction.amount }
                            val net = income - expense
                            
                            val mood = when {
                                net > 100 -> FinancialMood.BULLISH
                                net < -100 -> FinancialMood.BEARISH
                                else -> FinancialMood.NEUTRAL
                            }

                            DayMoodData(
                                date = date,
                                dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                                formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM")),
                                totalIncome = income,
                                totalExpense = expense,
                                netBalance = net,
                                transactionCount = items.size,
                                mood = mood,
                                transactions = items.sortedByDescending { it.transaction.dateTime },
                                isToday = date == LocalDate.now()
                            )
                        }
                        .sortedByDescending { it.date }

                    MoodFeedState(
                        days = grouped,
                        currentMonth = month
                    )
                }
            }.collect { state ->
                _uiState.value = if (state.days.isEmpty()) ScreenState.Empty else ScreenState.Success(state)
            }
        }
    }

    fun updateMonth(month: YearMonth) {
        _currentMonth.value = month
    }
}
