package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.features.reports.CategoryRank
import com.yourname.moneypilot.ui.features.reports.TimeRange
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

class GenerateReflectionPromptsUseCase @Inject constructor() {

    operator fun invoke(
        currentTotal: Double,
        prevTotal: Double,
        ranks: List<CategoryRank>,
        range: TimeRange,
        incomeTotal: Double,
        transactions: List<TransactionWithDetails>
    ): List<String> {
        val prompts = mutableListOf<String>()
        val periodName = when (range) {
            TimeRange.WEEKLY -> "last week"
            TimeRange.MONTHLY -> "last month"
            TimeRange.YEARLY -> "last year"
        }

        // 1. Comparison Trigger (Consumption Delta)
        if (prevTotal > 0) {
            val diff = ((currentTotal - prevTotal) / prevTotal) * 100
            if (diff > 15) {
                prompts.add("Consumption is up ${diff.toInt()}% compared to $periodName. Reflect on what changed.")
            } else if (diff < -15) {
                prompts.add("Great job! You've reduced spending by ${abs(diff.toInt())}% vs $periodName.")
            }
        }

        // 2. Dominance Trigger (Category weight)
        ranks.firstOrNull()?.let { top ->
            if (top.percentage > 0.4f) {
                prompts.add("${top.icon} ${top.name} is dominating your outflows at ${(top.percentage * 100).toInt()}% of total spend.")
            }
        }

        // 3. Burn Rate Trigger (Income vs Outflow)
        if (incomeTotal > 0 && currentTotal / incomeTotal > 0.9) {
            prompts.add("Warning: High burn rate. You've consumed over 90% of your income this period.")
        }

        // 4. Behavioral Trigger: Weekend Spike (v5 Architecture Requirement)
        val expenses = transactions.filter { it.transaction.type == TransactionType.Expense }
        if (expenses.size >= 10 && range != TimeRange.WEEKLY) {
            val weekendExpenses = expenses.filter { 
                val day = it.transaction.dateTime.dayOfWeek
                day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY 
            }
            val weekdayExpenses = expenses.filter { 
                val day = it.transaction.dateTime.dayOfWeek
                day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY 
            }

            if (weekendExpenses.isNotEmpty() && weekdayExpenses.isNotEmpty()) {
                val avgWeekend = weekendExpenses.sumOf { it.transaction.amount } / 2.0 
                val avgWeekday = weekdayExpenses.sumOf { it.transaction.amount } / 5.0 
                
                if (avgWeekend > avgWeekday * 1.8) {
                    prompts.add("Insight: Your weekend spending is significantly higher than your weekday average. Is this intentional?")
                }
            }
        }

        // 5. Behavioral Trigger: Salary-Day Spike Analysis (missing_items.mk)
        if (range == TimeRange.MONTHLY && transactions.isNotEmpty()) {
            val largestIncome = transactions
                .filter { it.transaction.type == TransactionType.Income }
                .maxByOrNull { it.transaction.amount }

            if (largestIncome != null && largestIncome.transaction.amount > (incomeTotal * 0.5)) {
                val salaryDate = largestIncome.transaction.dateTime.toLocalDate()
                val postSalaryExpenses = expenses.filter { 
                    val diff = ChronoUnit.DAYS.between(salaryDate, it.transaction.dateTime.toLocalDate())
                    diff in 0..3 
                }
                
                val totalMonthExpense = expenses.sumOf { it.transaction.amount }
                val daysInMonth = 30.0 // Approximation
                val avgDailyExpense = totalMonthExpense / daysInMonth
                
                val avgPostSalaryExpense = if (postSalaryExpenses.isNotEmpty()) postSalaryExpenses.sumOf { it.transaction.amount } / 4.0 else 0.0
                
                if (avgPostSalaryExpense > avgDailyExpense * 2.5) {
                    prompts.add("Pattern: We detected a significant spending spike immediately after your salary arrived on ${salaryDate.dayOfMonth}. Consider budgeting for these days.")
                }
            }
        }

        return prompts
    }
}
