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
                .groupBy { 
                    when {
                        it.transaction.goalId != null -> "GOAL_${it.transaction.goalId}"
                        it.transaction.loanId != null -> "LOAN_${it.transaction.loanId}"
                        it.transaction.investmentId != null -> "INV_${it.transaction.investmentId}"
                        else -> "CAT_${it.transaction.categoryId}_SUB_${it.transaction.subcategoryId}"
                    }
                }
                .map { (_, items) ->
                    val first = items.first()
                    val name = when {
                        first.transaction.goalId != null -> first.goal?.name ?: "Goal Contribution"
                        first.transaction.loanId != null -> first.loan?.name ?: "Repayment"
                        first.transaction.investmentId != null -> first.investment?.name ?: "Investment"
                        else -> first.category?.name ?: "Uncategorized"
                    }
                    val icon = when {
                        first.transaction.goalId != null -> first.goal?.icon ?: "🎯"
                        first.transaction.loanId != null -> "🏦"
                        first.transaction.investmentId != null -> "📈"
                        else -> first.category?.icon ?: "❓"
                    }

                    val subName = when {
                        first.transaction.goalId != null -> "Goal"
                        first.transaction.loanId != null -> "Loan"
                        first.transaction.investmentId != null -> "Investment"
                        else -> first.subcategory?.name
                    }

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
                        categoryId = first.transaction.categoryId,
                        subcategoryId = first.transaction.subcategoryId,
                        name = name,
                        subcategoryName = subName,
                        icon = icon,
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
