package com.yourname.moneypilot.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import timber.log.Timber

class DailySummaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Enqueue immediate one-time work so the notification is fired promptly
                val workRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>()
                    .setInitialDelay(0, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)

                // Re-read user preferences and re-schedule the next alarm via NotificationScheduler
                try {
                    val prefsRepo = UserPreferencesRepository(context)
                    val prefs = prefsRepo.userPreferencesFlow.first()
                    val scheduler = NotificationScheduler(context)
                    scheduler.scheduleDailySummary(prefs)
                    Timber.d("DailySummaryAlarmReceiver: re-armed next alarm via NotificationScheduler")
                } catch (e: Exception) {
                    Timber.w(e, "DailySummaryAlarmReceiver: failed to re-arm via NotificationScheduler")
                }
            } catch (t: Throwable) {
                Timber.w(t, "DailySummaryAlarmReceiver: error handling alarm receive")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
