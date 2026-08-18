package com.yourname.moneypilot.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.yourname.moneypilot.data.local.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import android.os.Build
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Schedules the next exact alarm for the daily summary.
     * Uses AlarmManager for precision (< 1 min delay) and WorkManager for task execution.
     */
    fun scheduleDailySummary(preferences: UserPreferences) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailySummaryAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!preferences.dailySummaryEnabled) {
            alarmManager.cancel(pendingIntent)
            Timber.d("Daily Summary disabled: Exact alarm cancelled.")
            return
        }

        val timeParts = preferences.dailySummaryTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 22
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = LocalDateTime.now()
        var executionTime = now.with(LocalTime.of(hour, minute, 0))
        
        // If the time has already passed today, schedule for tomorrow
        if (now.isAfter(executionTime)) {
            executionTime = executionTime.plusDays(1)
        }
        
        val triggerAtMillis = executionTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Set exact alarm that works even in Doze mode
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Timber.d("Daily Summary precision alarm scheduled for $executionTime")
                } else {
                    Timber.w("Cannot schedule exact alarms. Falling back to non-exact.")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Timber.d("Daily Summary precision alarm scheduled for $executionTime")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule alarm.")
        }
    }
}
