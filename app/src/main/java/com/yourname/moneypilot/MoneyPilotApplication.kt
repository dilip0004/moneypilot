package com.yourname.moneypilot

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.worker.DailyUpdateWorker
import com.yourname.moneypilot.worker.MonthlyRolloverWorker
import com.yourname.moneypilot.worker.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MoneyPilotApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferencesRepository: UserPreferencesRepository
    @Inject lateinit var notificationScheduler: NotificationScheduler

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

        // 3. Daily Summary Notification (preference-aware)
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = preferencesRepository.userPreferencesFlow.first()
            notificationScheduler.scheduleDailySummary(preferences)
            Timber.d("Daily Summary scheduled via NotificationScheduler")
        }
    }
}
