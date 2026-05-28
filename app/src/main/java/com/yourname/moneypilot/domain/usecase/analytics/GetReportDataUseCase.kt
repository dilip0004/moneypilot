package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.features.reports.CategoryRank
import com.yourname.moneypilot.ui.features.reports.ReportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

data class AggregatedReportData(
    val totalAmount: Double,
    val totalOutflow: Double,
    val categoryBreakdown: List<CategoryRank>,
    val filteredTransactions: List<TransactionWithDetails>
)

class GetReportDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Section 4.0: Accounting Doctrine Compliance.
     * Section 5.3: Credit Card Model (Refund -> Reverse original expense).
     * 
     * This UseCase treats "Refunds" as negative expenses to ensure they don't inflate
     * income metrics but correctly reduce consumption totals.
     */
    operator fun invoke(
        start: LocalDateTime,
        end: LocalDateTime,
        reportType: ReportType
    ): Flow<AggregatedReportData> {
        return transactionRepository.getTransactionsWithDetailsByDateRange(start, end).map { transactions ->
            
            val totalInflow = transactions.filter { 
                it.transaction.type == TransactionType.Income && !it.transaction.isRefund 
            }.sumOf { it.transaction.amount }

            // Consumption = sum of Expenses - sum of Refunds (negative impact)
            val consumptionSum = transactions.filter { it.transaction.type == TransactionType.Expense }.sumOf { 
                if (it.transaction.isRefund) -it.transaction.amount else it.transaction.amount
            }
            
            val totalOutflow = consumptionSum

            val totalDisplay = when (reportType) {
                ReportType.INCOME -> totalInflow
                ReportType.EXPENSE -> consumptionSum
                ReportType.CASH_FLOW -> totalInflow - consumptionSum
            }

            val listFiltered = when (reportType) {
                ReportType.INCOME -> transactions.filter { it.transaction.type == TransactionType.Income && !it.transaction.isRefund }
                ReportType.EXPENSE -> transactions.filter { it.transaction.type == TransactionType.Expense }
                ReportType.CASH_FLOW -> transactions.filter { it.transaction.type != TransactionType.Transfer }
            }

            val ranks = listFiltered
                .groupBy { it.transaction.categoryId }
                .map { (id, items) ->
                    val sum = items.sumOf { 
                        if (it.transaction.type == TransactionType.Expense && it.transaction.isRefund) -it.transaction.amount 
                        else it.transaction.amount 
                    }
                    val denom = when (reportType) {
                        ReportType.INCOME -> totalInflow
                        ReportType.EXPENSE -> consumptionSum
                        ReportType.CASH_FLOW -> consumptionSum
                    }
                    CategoryRank(
                        categoryId = id,
                        name = items.first().category?.name ?: "Uncategorized",
                        icon = items.first().category?.icon ?: "❓",
                        amount = sum,
                        percentage = if (denom > 0) (sum / denom).toFloat() else 0f
                    )
                }
                .filter { it.amount != 0.0 }
                .sortedByDescending { it.amount }

            AggregatedReportData(
                totalAmount = totalDisplay,
                totalOutflow = totalOutflow,
                categoryBreakdown = ranks,
                filteredTransactions = listFiltered
            )
        }
    }
}
