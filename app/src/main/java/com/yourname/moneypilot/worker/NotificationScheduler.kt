package com.yourname.moneypilot.worker

import android.content.Context
import androidx.work.*
import com.yourname.moneypilot.data.local.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleDailySummary(preferences: UserPreferences) {
        val workManager = WorkManager.getInstance(context)

        if (!preferences.dailySummaryEnabled) {
            workManager.cancelUniqueWork("DailySummaryWork")
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, DailySummaryAlarmReceiver::class.java)
                val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                alarmManager.cancel(pending)
                Timber.d("Daily Summary alarm cancelled")
            } catch (e: Exception) {
                Timber.w(e, "Failed to cancel Daily Summary alarm")
            }
            Timber.d("Daily Summary disabled and work cancelled")
            return
        }

        // Parse 24h time safely
        val timeParts = preferences.dailySummaryTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 22
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = LocalDateTime.now()
        var executionTime = now.with(LocalTime.of(hour, minute, 0))
        
        // If the time has already passed today, schedule for tomorrow
        if (now.isAfter(executionTime)) {
            executionTime = executionTime.plusDays(1)
        }
        
        val initialDelayMillis = Duration.between(now, executionTime).toMillis()

        val summaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .build()

        // Use REPLACE to ensure the new schedule/delay is applied immediately
        workManager.enqueueUniquePeriodicWork(
            "DailySummaryWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            summaryRequest
        )
        
        // Also schedule an exact AlarmManager alarm as a fallback to improve delivery reliability
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailySummaryAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, executionTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pending)
            Timber.d("Daily Summary alarm scheduled for $executionTime (delay: ${initialDelayMillis / 1000}s)")
        } catch (e: Exception) {
            Timber.w(e, "Failed to schedule alarm fallback for Daily Summary")
        }

        Timber.d("Daily Summary scheduled for $executionTime (delay: ${initialDelayMillis / 1000}s)")
    }
}
