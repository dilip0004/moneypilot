package com.yourname.moneypilot.worker

import android.content.Context
import androidx.work.*
import com.yourname.moneypilot.data.local.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
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
            return
        }

        val timeParts = preferences.dailySummaryTime.split(":")
        val hour = timeParts.getOrNull(0)?.toInt() ?: 22
        val minute = timeParts.getOrNull(1)?.toInt() ?: 0

        val now = LocalDateTime.now()
        var executionTime = LocalDateTime.now().with(LocalTime.of(hour, minute))
        
        if (now.isAfter(executionTime)) {
            executionTime = executionTime.plusDays(1)
        }
        
        val initialDelay = Duration.between(now, executionTime).toMinutes()

        val summaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailySummaryWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            summaryRequest
        )
    }
}
