package com.yourname.moneypilot

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.yourname.moneypilot.worker.DailyUpdateWorker
import com.yourname.moneypilot.worker.MonthlyRolloverWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
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

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        scheduleBackgroundTasks()
        Timber.d("MoneyPilot Application Started and Tasks Scheduled")
    }

    private fun scheduleBackgroundTasks() {
        val workManager = WorkManager.getInstance(this)

        // Daily task for recurring transactions
        val dailyRequest = PeriodicWorkRequestBuilder<DailyUpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

        // Monthly task for budget rollover (checked daily to ensure it runs on the 1st)
        val monthlyRequest = PeriodicWorkRequestBuilder<MonthlyRolloverWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "MonthlyRolloverWork",
            ExistingPeriodicWorkPolicy.KEEP,
            monthlyRequest
        )
    }
}
