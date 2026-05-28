package com.yourname.moneypilot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.dao.BudgetDao
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@HiltWorker
class MonthlyRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetDao: BudgetDao,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("MonthlyRolloverWorker: Starting execution audit")
        return try {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val today = LocalDate.now()
            val currentMonthStr = YearMonth.from(today).toString() // YYYY-MM

            // Section 16.0 Compliance: Handle Budget Carry Forward reliably
            if (prefs.lastRolloverMonth != currentMonthStr) {
                performRollover(today)
                preferencesRepository.updateLastRolloverMonth(currentMonthStr)
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "MonthlyRolloverWorker: Execution failed")
            Result.retry()
        }
    }

    private suspend fun performRollover(today: LocalDate) {
        // Find budgets from the logical "previous month" relative to today
        val lastMonthDate = today.minusMonths(1).withDayOfMonth(1)
        val budgetsToRollover = budgetDao.getActiveBudgets(lastMonthDate).first()

        Timber.i("MonthlyRolloverWorker: Auditing ${budgetsToRollover.size} budgets for rollover from $lastMonthDate")

        budgetsToRollover.forEach { budget ->
            if (budget.rolloverEnabled) {
                // Check if a budget already exists for the CURRENT month for this category
                val currentMonthBudgets = budgetDao.getActiveBudgets(today).first()
                val alreadyExists = currentMonthBudgets.any { it.categoryId == budget.categoryId && it.subcategoryId == budget.subcategoryId }

                if (!alreadyExists) {
                    val remaining = (budget.amount - budget.spentAmount).coerceAtLeast(0.0)
                    if (remaining > 0) {
                        val newBudget = budget.copy(
                            id = 0,
                            amount = budget.amount + remaining,
                            spentAmount = 0.0,
                            startDate = today.withDayOfMonth(1),
                            endDate = today.withDayOfMonth(today.lengthOfMonth()),
                            createdAt = java.time.LocalDateTime.now(),
                            updatedAt = java.time.LocalDateTime.now()
                        )
                        budgetDao.insert(newBudget)
                        Timber.d("MonthlyRolloverWorker: Rolled over ₹$remaining for category ${budget.categoryId}")
                    }
                }
            }
        }
    }
}
