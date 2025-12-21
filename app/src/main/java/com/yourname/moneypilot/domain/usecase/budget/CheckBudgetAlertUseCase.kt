package com.yourname.moneypilot.domain.usecase.budget

import com.yourname.moneypilot.domain.model.Budget
import javax.inject.Inject

class CheckBudgetAlertUseCase @Inject constructor() {
    operator fun invoke(budget: Budget): Boolean {
        if (budget.amount <= 0) return false
        val spentPercentage = (budget.spentAmount / budget.amount) * 100
        return spentPercentage >= budget.alertThreshold
    }
}
