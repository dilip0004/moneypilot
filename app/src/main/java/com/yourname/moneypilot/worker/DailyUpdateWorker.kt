package com.yourname.moneypilot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime

@HiltWorker
class DailyUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("DailyUpdateWorker started")
        return try {
            processRecurringTransactions()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in DailyUpdateWorker")
            Result.retry()
        }
    }

    private suspend fun processRecurringTransactions() {
        val now = LocalDateTime.now()
        val recurringTransactions = transactionDao.getRecurringTransactions().first()

        recurringTransactions.forEach { transaction ->
            if (shouldCreateNextTransaction(transaction, now)) {
                val nextTransaction = transaction.copy(
                    id = 0,
                    date = calculateNextDate(transaction.date, transaction.recurringPattern),
                    createdAt = now,
                    updatedAt = now
                )
                transactionDao.insert(nextTransaction)
                Timber.d("Created next recurring transaction for: ${transaction.description}")
            }
        }
    }

    private fun shouldCreateNextTransaction(transaction: TransactionEntity, now: LocalDateTime): Boolean {
        val nextDate = calculateNextDate(transaction.date, transaction.recurringPattern)
        return nextDate.isBefore(now) || nextDate.isEqual(now)
    }

    private fun calculateNextDate(currentDate: LocalDateTime, pattern: String?): LocalDateTime {
        return when (pattern) {
            "DAILY" -> currentDate.plusDays(1)
            "WEEKLY" -> currentDate.plusWeeks(1)
            "MONTHLY" -> currentDate.plusMonths(1)
            "YEARLY" -> currentDate.plusYears(1)
            else -> currentDate
        }
    }
}
