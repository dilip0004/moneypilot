package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val dailySpending: Map<Int, Double> // Day of month to amount
)

class CalculateMonthlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(month: Int, year: Int): Flow<MonthlySummary> {
        val startDate = LocalDateTime.of(year, month, 1, 0, 0)
        val endDate = startDate.plusMonths(1).minusNanos(1)
        
        return transactionRepository.getTransactionsByDateRange(startDate, endDate).map { transactions ->
            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            
            val dailyMap = transactions
                .filter { it.type == "EXPENSE" }
                .groupBy { it.date.dayOfMonth }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                
            MonthlySummary(income, expense, dailyMap)
        }
    }
}
