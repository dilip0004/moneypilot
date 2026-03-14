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
    operator fun invoke(
        start: LocalDateTime,
        end: LocalDateTime,
        reportType: ReportType
    ): Flow<AggregatedReportData> {
        return transactionRepository.getTransactionsWithDetailsByDateRange(start, end).map { transactions ->
            val nonTransfer = transactions.filter { it.transaction.type != TransactionType.Transfer }
            
            val incomeSum = nonTransfer.filter { it.transaction.type == TransactionType.Income }.sumOf { it.transaction.amount }
            val expenseSum = nonTransfer.filter { it.transaction.type == TransactionType.Expense }.sumOf { it.transaction.amount }
            
            // In v5, we simplify outflow to just expenses for now
            val totalOutflow = expenseSum

            val totalDisplay = when (reportType) {
                ReportType.INCOME -> incomeSum
                ReportType.EXPENSE -> expenseSum
                ReportType.CASH_FLOW -> incomeSum - totalOutflow
            }

            val listFiltered = when (reportType) {
                ReportType.INCOME -> nonTransfer.filter { it.transaction.type == TransactionType.Income }
                ReportType.EXPENSE -> nonTransfer.filter { it.transaction.type == TransactionType.Expense }
                ReportType.CASH_FLOW -> nonTransfer // Show all for cash flow context
            }

            val ranks = listFiltered
                .groupBy { it.transaction.categoryId }
                .map { (id, items) ->
                    val sum = items.sumOf { it.transaction.amount }
                    val denom = when (reportType) {
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

            AggregatedReportData(
                totalAmount = totalDisplay,
                totalOutflow = totalOutflow,
                categoryBreakdown = ranks,
                filteredTransactions = listFiltered
            )
        }
    }
}
