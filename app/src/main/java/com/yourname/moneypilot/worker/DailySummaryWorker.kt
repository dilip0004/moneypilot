package com.yourname.moneypilot.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import timber.log.Timber

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationScheduler: NotificationScheduler // Injected to re-arm the next alarm
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferences = preferencesRepository.userPreferencesFlow.first()
            
            // 1. Execute the actual work
            if (preferences.dailySummaryEnabled) {
                val today = LocalDate.now()
                val startOfDay = today.atStartOfDay()
                val endOfDay = today.atTime(LocalTime.MAX)

                val transactionsWithDetails = transactionRepository.getTransactionsWithDetailsByDateRange(startOfDay, endOfDay).first()
                val transactions = transactionsWithDetails.map { it.transaction }
                
                val totalSpent = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                val totalEarned = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }

                sendNotification(totalSpent, totalEarned)
            }

            // 2. Re-arm the next daily alarm (Linear Chain Pattern)
            notificationScheduler.scheduleDailySummary(preferences)
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "DailySummaryWorker: Failed to process or re-arm")
            Result.failure()
        }
    }

    private fun sendNotification(spent: Double, earned: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_summary_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Summary",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily financial spending and earnings summary"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Financial Summary")
            .setContentText("Spent: ₹${String.format("%.2f", spent)} | Earned: ₹${String.format("%.2f", earned)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        notificationManager.notify(1, notification)
    }
}
