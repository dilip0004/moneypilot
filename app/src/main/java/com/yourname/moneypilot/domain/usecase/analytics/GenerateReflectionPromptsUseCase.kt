package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.ui.features.reports.CategoryRank
import com.yourname.moneypilot.ui.features.reports.TimeRange
import javax.inject.Inject
import kotlin.math.abs

class GenerateReflectionPromptsUseCase @Inject constructor() {

    operator fun invoke(
        currentTotal: Double,
        prevTotal: Double,
        ranks: List<CategoryRank>,
        range: TimeRange,
        incomeTotal: Double
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
            if (diff > 10) {
                prompts.add("Consumption is up ${diff.toInt()}% compared to $periodName. Reflect on what changed.")
            } else if (diff < -10) {
                prompts.add("Great job! You've reduced spending by ${abs(diff.toInt())}% vs $periodName.")
            }
        }

        // 2. Dominance Trigger (Category weight)
        ranks.firstOrNull()?.let { top ->
            if (top.percentage > 0.4f) {
                prompts.add("${top.icon} ${top.name} is dominating your outflows at ${(top.percentage * 100).toInt()}%.")
            }
        }

        // 3. Burn Rate Trigger (Income vs Outflow)
        if (incomeTotal > 0 && currentTotal / incomeTotal > 0.9) {
            prompts.add("Warning: High burn rate. You've consumed over 90% of your income this period.")
        }

        return prompts
    }
}
