package com.yourname.moneypilot.ui.features.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import timber.log.Timber
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onPopBackStack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val preferences by viewModel.userPreferences.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("Notifications: Permission granted: $isGranted")
    }

    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    // Formatter for UI display (12h with AM/PM)
    val uiFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    if (showTimePicker && preferences != null) {
        // Extract latest values from preferences
        val savedTime = preferences?.dailySummaryTime?.split(":") ?: emptyList()
        val currentHour = savedTime.getOrNull(0)?.toIntOrNull() ?: 22
        val currentMinute = savedTime.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    viewModel.updateDailySummaryTime(time)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Daily Digest", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Summary", style = MaterialTheme.typography.bodyLarge)
                    Text("Receive a notification of your daily spend and earnings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences?.dailySummaryEnabled ?: true,
                    onCheckedChange = { 
                        viewModel.updateDailySummaryEnabled(it)
                        if (it) checkAndRequestNotificationPermission()
                    }
                )
            }

            // Correctly parse the saved time for display
            val displayTime = preferences?.dailySummaryTime?.let {
                val parts = it.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                LocalTime.of(h, m).format(uiFormatter)
            } ?: "10:00 PM"

            Surface(
                onClick = { showTimePicker = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                enabled = preferences?.dailySummaryEnabled ?: true
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Notification Time", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Insights", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Spending Insights", style = MaterialTheme.typography.bodyLarge)
                    Text("Analyze patterns and surface suggestions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences?.showSpendingInsights ?: false,
                    onCheckedChange = { viewModel.updateShowSpendingInsights(it) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Burn Rate Alerts", style = MaterialTheme.typography.bodyLarge)
                    Text("Warn when spending exceeds threshold of income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences?.showBurnRateAlerts ?: false,
                    onCheckedChange = { viewModel.updateShowBurnRateAlerts(it) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Category Alerts", style = MaterialTheme.typography.bodyLarge)
                    Text("Notify when a single category dominates spending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences?.showCategoryAlerts ?: false,
                    onCheckedChange = { viewModel.updateShowCategoryAlerts(it) }
                )
            }

            HorizontalDivider()

            Text("Alerts & Reminders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Budget Alerts", style = MaterialTheme.typography.bodyLarge)
                    Text("Notify when spending reaches 90% of budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences?.budgetAlertsEnabled ?: true,
                    onCheckedChange = { 
                        viewModel.updateBudgetAlertsEnabled(it)
                        if (it) checkAndRequestNotificationPermission()
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Goal Progress", style = MaterialTheme.typography.bodyLarge)
                    Text("Weekly updates on your savings goals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences?.goalProgressEnabled ?: true,
                    onCheckedChange = { 
                        viewModel.updateGoalProgressEnabled(it)
                        if (it) checkAndRequestNotificationPermission()
                    }
                )
            }
        }
    }
}
