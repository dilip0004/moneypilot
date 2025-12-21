package com.yourname.moneypilot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.dao.BudgetDao
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate

@HiltWorker
class MonthlyRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetDao: BudgetDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("MonthlyRolloverWorker started")
        return try {
            performRollover()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in MonthlyRolloverWorker")
            Result.retry()
        }
    }

    private suspend fun performRollover() {
        val today = LocalDate.now()
        // We only perform rollover on the first day of the month
        if (today.dayOfMonth != 1) return

        val lastMonthDate = today.minusDays(1)
        val activeBudgets = budgetDao.getActiveBudgets(lastMonthDate).first()

        activeBudgets.forEach { budget ->
            if (budget.rolloverEnabled) {
                val remaining = budget.amount - budget.spentAmount
                if (remaining > 0) {
                    val newBudget = budget.copy(
                        id = 0,
                        amount = budget.amount + remaining,
                        spentAmount = 0.0,
                        startDate = today,
                        endDate = today.withDayOfMonth(today.lengthOfMonth()),
                        createdAt = java.time.LocalDateTime.now(),
                        updatedAt = java.time.LocalDateTime.now()
                    )
                    budgetDao.insert(newBudget)
                    Timber.d("Rolled over budget for category ${budget.categoryId}. New amount: ${newBudget.amount}")
                }
            }
        }
    }
}
