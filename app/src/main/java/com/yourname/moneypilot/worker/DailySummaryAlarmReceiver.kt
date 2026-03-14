package com.yourname.moneypilot.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import timber.log.Timber

class DailySummaryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("DailySummaryAlarmReceiver: Exact alarm triggered.")
        
        // Enqueue immediate one-time work so the notification is fired promptly
        val workRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay(0, TimeUnit.SECONDS)
            .addTag("DailySummaryImmediate")
            .build()
            
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
