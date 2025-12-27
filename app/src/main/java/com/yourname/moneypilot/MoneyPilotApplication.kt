package com.yourname.moneypilot

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.yourname.moneypilot.worker.DailySummaryWorker
import com.yourname.moneypilot.worker.DailyUpdateWorker
import com.yourname.moneypilot.worker.MonthlyRolloverWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MoneyPilotApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        scheduleBackgroundTasks()
    }

    private fun scheduleBackgroundTasks() {
        val workManager = WorkManager.getInstance(this)

        // 1. Recurring Transactions Task
        val dailyRequest = PeriodicWorkRequestBuilder<DailyUpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniquePeriodicWork("DailyUpdateWork", ExistingPeriodicWorkPolicy.KEEP, dailyRequest)

        // 2. Budget Rollover Task
        val monthlyRequest = PeriodicWorkRequestBuilder<MonthlyRolloverWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork("MonthlyRolloverWork", ExistingPeriodicWorkPolicy.KEEP, monthlyRequest)

        // 3. DAILY SUMMARY NOTIFICATION (The Fix for 10 PM Requirement)
        val now = LocalDateTime.now()
        var executionTime = LocalDateTime.now().with(LocalTime.of(22, 0)) // Target 10 PM
        
        if (now.isAfter(executionTime)) {
            executionTime = executionTime.plusDays(1)
        }
        
        val initialDelay = Duration.between(now, executionTime).toMinutes()

        val summaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailySummaryWork",
            ExistingPeriodicWorkPolicy.REPLACE, // REPLACE to ensure the new 10PM timing is applied
            summaryRequest
        )
        
        Timber.d("All tasks scheduled. Daily Summary scheduled for 10 PM with delay of $initialDelay mins")
    }
}
