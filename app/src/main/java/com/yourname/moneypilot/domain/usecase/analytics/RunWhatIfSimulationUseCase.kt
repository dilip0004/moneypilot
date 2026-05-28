package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import java.time.LocalDate
import javax.inject.Inject

data class SimulationImpact(
    val goalId: Long,
    val goalName: String,
    val originalEndDate: LocalDate,
    val simulatedEndDate: LocalDate,
    val monthsSaved: Int
)

class RunWhatIfSimulationUseCase @Inject constructor() {

    operator fun invoke(
        activeGoals: List<GoalEntity>,
        currentMonthlyContribution: Double,
        monthlySavingsBoost: Double
    ): List<SimulationImpact> {
        val totalMonthlyAvailable = (currentMonthlyContribution + monthlySavingsBoost).coerceAtLeast(0.0)
        if (totalMonthlyAvailable <= 0) return emptyList()

        return activeGoals.sortedBy { it.priority }.map { goal ->
            val remainingToSave = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            
            val originalMonths = if (currentMonthlyContribution > 0) {
                Math.ceil(remainingToSave / currentMonthlyContribution).toLong()
            } else 999L
            val originalEnd = LocalDate.now().plusMonths(originalMonths)

            val simulatedMonths = Math.ceil(remainingToSave / totalMonthlyAvailable).toLong()
            val simulatedEnd = LocalDate.now().plusMonths(simulatedMonths)

            SimulationImpact(
                goalId = goal.id,
                goalName = goal.name,
                originalEndDate = originalEnd,
                simulatedEndDate = simulatedEnd,
                monthsSaved = (originalMonths - simulatedMonths).toInt().coerceAtLeast(0)
            )
        }
    }
}
