package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import javax.inject.Inject

data class KeyAnalytics(
    val savingsRate: Float? = null,
    val dailyAverage: Double? = null,
    val transactionCount: Int = 0,
    val categoryDominance: Map<String, Float> = emptyMap()
)

class CalculateKeyAnalyticsUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionWithDetails>,
        daysInPeriod: Int
    ): KeyAnalytics {
        val nonTransfer = transactions.filter { it.transaction.type != TransactionType.Transfer }.map { it.transaction }

        val totalIncome = nonTransfer.filter { it.type == TransactionType.Income }.sumOf { it.amount }
        val totalExpense = nonTransfer.filter { it.type == TransactionType.Expense }.sumOf { it.amount }

        // 1. Savings Rate (Formerly Efficiency)
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome).toFloat() else null

        // 2. Daily Average (Formerly Velocity)
        val dailyAverage = if (daysInPeriod > 0) totalExpense / daysInPeriod else null
        
        // 3. Transactions (Frequency)
        val transactionCount = transactions.size

        // 4. Category Dominance
        val categorySpending = transactions
            .filter { it.transaction.type == TransactionType.Expense }
            .groupBy { 
                when {
                    it.transaction.goalId != null -> "GOAL_${it.transaction.goalId}"
                    it.transaction.loanId != null -> "LOAN_${it.transaction.loanId}"
                    it.transaction.investmentId != null -> "INV_${it.transaction.investmentId}"
                    else -> "CAT_${it.transaction.categoryId}"
                }
            }
            .mapValues { entry -> entry.value.sumOf { it.transaction.amount } }

        val dominance = if (totalExpense > 0) {
            categorySpending.map { (key, amount) ->
                val first = transactions.first { 
                    val itKey = when {
                        it.transaction.goalId != null -> "GOAL_${it.transaction.goalId}"
                        it.transaction.loanId != null -> "LOAN_${it.transaction.loanId}"
                        it.transaction.investmentId != null -> "INV_${it.transaction.investmentId}"
                        else -> "CAT_${it.transaction.categoryId}"
                    }
                    itKey == key
                }
                val name = when {
                    first.transaction.goalId != null -> "🎯 ${first.goal?.name ?: "Goal"}"
                    first.transaction.loanId != null -> "🏦 ${first.loan?.name ?: "Loan"}"
                    first.transaction.investmentId != null -> "📈 ${first.investment?.name ?: "Inv"}"
                    else -> first.category?.name ?: "Uncategorized"
                }
                name to (amount / totalExpense).toFloat()
            }.toMap()
        } else {
            emptyMap()
        }

        return KeyAnalytics(
            savingsRate = savingsRate,
            dailyAverage = dailyAverage,
            transactionCount = transactionCount,
            categoryDominance = dominance
        )
    }
}
