package com.yourname.moneypilot.worker

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import timber.log.Timber
import java.time.temporal.ChronoUnit

/**
 * Enhanced Daily Worker (TASK-48)
 * Responsible for Daily Summaries AND Proactive Big Bill Reminders.
 */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val bigBillRepository: BigBillRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferences = preferencesRepository.userPreferencesFlow.first()
            
            if (preferences.dailySummaryEnabled) {
                val today = LocalDate.now()
                
                // 1. Calculate Daily Stats
                val startOfDay = today.atStartOfDay()
                val endOfDay = today.atTime(LocalTime.MAX)
                val txs = transactionRepository.getTransactionsWithDetailsByDateRange(startOfDay, endOfDay).first()
                val totalSpent = txs.filter { it.transaction.type == TransactionType.Expense }.sumOf { it.transaction.amount }
                val totalEarned = txs.filter { it.transaction.type == TransactionType.Income }.sumOf { it.transaction.amount }

                // 2. Identify Urgent Big Bills (TASK-48)
                val upcomingBills = bigBillRepository.getUnpaidBigBills().first().filter { bill ->
                    val daysUntilDue = ChronoUnit.DAYS.between(today, bill.dueDate)
                    daysUntilDue >= 0 && daysUntilDue <= bill.reminderDaysBefore
                }

                sendCombinedNotification(totalSpent, totalEarned, upcomingBills.size)
            }

            notificationScheduler.scheduleDailySummary(preferences)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "DailySummaryWorker: Failure during combined processing")
            Result.failure()
        }
    }

    private fun sendCombinedNotification(spent: Double, earned: Double, urgentBillCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "money_pilot_notifications"

        val summaryText = "Spent: ₹${String.format("%.0f", spent)} | Earned: ₹${String.format("%.0f", earned)}"
        val billAlertText = if (urgentBillCount > 0) "\n⚠️ $urgentBillCount Big Bills due soon!" else ""

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Financial Snapshot")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText + billAlertText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(1, notification)
    }
}
