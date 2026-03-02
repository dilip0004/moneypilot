package com.yourname.moneypilot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
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
        
        createNotificationChannel()
        scheduleBackgroundTasks()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "daily_summary_channel"
            val name = "Daily Financial Summary"
            val descriptionText = "Evening summary of your daily transactions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundTasks() {
        val workManager = WorkManager.getInstance(this)

        // Budget Rollover Task - run roughly once every 30 days instead of daily
        val monthlyRequest = PeriodicWorkRequestBuilder<MonthlyRolloverWorker>(30, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniquePeriodicWork("MonthlyRolloverWork", ExistingPeriodicWorkPolicy.KEEP, monthlyRequest)

        // Daily Summary Notification (preference-aware)
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = preferencesRepository.userPreferencesFlow.first()
            notificationScheduler.scheduleDailySummary(preferences)
            Timber.d("Daily Summary scheduled via NotificationScheduler")
        }
    }
}
