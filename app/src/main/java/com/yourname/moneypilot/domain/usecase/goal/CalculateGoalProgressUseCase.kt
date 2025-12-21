package com.yourname.moneypilot.domain.usecase.goal

import com.yourname.moneypilot.domain.model.Goal
import javax.inject.Inject

class CalculateGoalProgressUseCase @Inject constructor() {
    operator fun invoke(goal: Goal): Float {
        if (goal.targetAmount <= 0) return 0f
        return (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    }
}
