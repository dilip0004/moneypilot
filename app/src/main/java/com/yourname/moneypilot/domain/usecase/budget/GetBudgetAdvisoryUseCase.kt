package com.yourname.moneypilot.domain.usecase.budget

import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class BudgetAdvisory(
    val categoryId: Long,
    val categoryName: String,
    val currentLimit: Double,
    val suggestedLimit: Double,
    val reason: String,
    val type: AdvisoryType
)

enum class AdvisoryType { OPTIMIZE, INCREASE, CAUTION }

class GetBudgetAdvisoryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * Section 3.6 Compliance: Advisory Planning.
     * Analyzes last 3 months of spending to suggest limit adjustments.
     */
    suspend operator fun invoke(): List<BudgetAdvisory> {
        val today = LocalDate.now()
        val allBudgets: List<BudgetEntity> = budgetRepository.getAllBudgetsList()
        val currentBudgets = allBudgets.filter { budget -> budget.startDate <= today && budget.endDate >= today }
        
        val recommendations = mutableListOf<BudgetAdvisory>()

        for (budget in currentBudgets) {
            // Analyze previous 3 months
            val categoryId = budget.categoryId
            val startHistory = today.minusMonths(3).withDayOfMonth(1).atStartOfDay()
            val endHistory = today.withDayOfMonth(1).atStartOfDay().minusSeconds(1)

            val totalSpentHistory = transactionRepository.getCategoryExpenseSum(categoryId, startHistory, endHistory)
            val avgMonthlySpend = totalSpentHistory / 3.0

            if (avgMonthlySpend <= 0) continue

            val drift = (avgMonthlySpend / budget.amount)
            
            when {
                drift < 0.75 -> {
                    recommendations.add(BudgetAdvisory(
                        categoryId = categoryId,
                        categoryName = "Budget $categoryId", 
                        currentLimit = budget.amount,
                        suggestedLimit = Math.ceil(avgMonthlySpend * 1.1),
                        reason = "You consistently spend less than your limit here.",
                        type = AdvisoryType.OPTIMIZE
                    ))
                }
                drift > 0.95 -> {
                    recommendations.add(BudgetAdvisory(
                        categoryId = categoryId,
                        categoryName = "Budget $categoryId",
                        currentLimit = budget.amount,
                        suggestedLimit = Math.ceil(avgMonthlySpend * 1.15),
                        reason = "You are frequently hitting or exceeding this limit.",
                        type = AdvisoryType.INCREASE
                    ))
                }
            }
        }

        return recommendations.take(3) 
    }
}
