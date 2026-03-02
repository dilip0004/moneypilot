package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class DailySummary(
    val date: LocalDate,
    val totalIncome: Double,
    val totalExpense: Double
)

class GetDailyFinancialSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<Map<LocalDate, DailySummary>> {
        val startDate = LocalDateTime.of(year, month, 1, 0, 0)
        val endDate = startDate.plusMonths(1).minusNanos(1)

        return transactionRepository.getTransactionsWithDetailsByDateRange(startDate, endDate).map { transactionsWithDetails ->
            transactionsWithDetails.map { it.transaction }
                .groupBy { it.dateTime.toLocalDate() }
                .mapValues { (date, dailyTransactions) ->
                    DailySummary(
                        date = date,
                        totalIncome = dailyTransactions.filter { it.type == TransactionType.Income }.sumOf { it.amount },
                        totalExpense = dailyTransactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                    )
                }
        }
    }
}
