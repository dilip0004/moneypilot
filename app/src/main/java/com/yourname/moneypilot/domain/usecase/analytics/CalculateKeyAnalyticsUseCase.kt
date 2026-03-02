package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import javax.inject.Inject

data class KeyAnalytics(
    val efficiency: Float? = null,
    val expenseVelocity: Double? = null,
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

        // 1. Efficiency
        val efficiency = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome).toFloat() else null

        // 2. Expense Velocity
        val velocity = if (daysInPeriod > 0) totalExpense / daysInPeriod else null

        // 3. Category Dominance
        val categorySpending = nonTransfer
            .filter { it.type == TransactionType.Expense && it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val dominance = if (totalExpense > 0) {
            categorySpending.map { (categoryId, amount) ->
                val categoryName = transactions.first { it.transaction.categoryId == categoryId }.category?.name ?: "Uncategorized"
                categoryName to (amount / totalExpense).toFloat()
            }.toMap()
        } else {
            emptyMap()
        }

        return KeyAnalytics(
            efficiency = efficiency,
            expenseVelocity = velocity,
            categoryDominance = dominance
        )
    }
}
