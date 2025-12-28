package com.yourname.moneypilot.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val preferences = preferencesRepository.userPreferencesFlow.first()
            if (!preferences.dailySummaryEnabled) return Result.success()

            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay()
            val endOfDay = today.atTime(LocalTime.MAX)

            // Use the one-shot query to avoid Flow collection issues in background workers
            val transactions = transactionDao.getTransactionsByDateRangeOnce(startOfDay, endOfDay)
            
            val totalSpent = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalEarned = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }

            sendNotification(totalSpent, totalEarned)
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
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
