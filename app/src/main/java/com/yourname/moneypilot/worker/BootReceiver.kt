package com.yourname.moneypilot.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefsRepo = UserPreferencesRepository(context)
                    val prefs = prefsRepo.userPreferencesFlow.first()
                    val scheduler = NotificationScheduler(context)
                    scheduler.scheduleDailySummary(prefs)
                    Timber.d("BootReceiver: scheduled Daily Summary after boot/package replace")
                } catch (e: Exception) {
                    Timber.w(e, "BootReceiver: failed to schedule Daily Summary on boot")
                }
            }
        }
    }
}
