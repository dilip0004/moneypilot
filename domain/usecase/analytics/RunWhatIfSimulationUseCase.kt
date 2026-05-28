package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SimulationImpact(
    val goalId: Long,
    val goalName: String,
    val originalEndDate: LocalDate,
    val simulatedEndDate: LocalDate,
    val monthsSaved: Int
)

class RunWhatIfSimulationUseCase @Inject constructor() {

    /**
     * Section 8.0 Compliance: Planning Engine / "What-if" Simulations.
     * Calculates how reducing spend in a category (re-allocating to goals) impacts timelines.
     */
    operator fun invoke(
        activeGoals: List<GoalEntity>,
        currentMonthlyContribution: Double, // The average amount user currently saves/contributes to goals
        monthlySavingsBoost: Double // Extra amount available by cutting expenses
    ): List<SimulationImpact> {
        val totalMonthlyAvailable = currentMonthlyContribution + monthlySavingsBoost
        if (totalMonthlyAvailable <= 0) return emptyList()

        val results = mutableListOf<SimulationImpact>()
        
        // Simple sequential allocation simulation (as per priority)
        var remainingBoost = totalMonthlyAvailable
        
        activeGoals.sortedBy { it.priority }.forEach { goal ->
            val remainingToSave = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            
            // 1. Original Projection
            val originalMonths = if (currentMonthlyContribution > 0) {
                Math.ceil(remainingToSave / currentMonthlyContribution).toLong()
            } else 999L
            val originalEnd = LocalDate.now().plusMonths(originalMonths)

            // 2. Simulated Projection
            val simulatedMonths = Math.ceil(remainingToSave / totalMonthlyAvailable).toLong()
            val simulatedEnd = LocalDate.now().plusMonths(simulatedMonths)

            results.add(
                SimulationImpact(
                    goalId = goal.id,
                    goalName = goal.name,
                    originalEndDate = originalEnd,
                    simulatedEndDate = simulatedEnd,
                    monthsSaved = (originalMonths - simulatedMonths).toInt().coerceAtLeast(0)
                )
            )
        }

        return results
    }
}
