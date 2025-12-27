package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onPopBackStack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = preferences?.dailySummaryTime?.split(":")?.get(0)?.toInt() ?: 22,
        initialMinute = preferences?.dailySummaryTime?.split(":")?.get(1)?.toInt() ?: 0
    )

    if (showTimePicker) {
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
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
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
                    onCheckedChange = { viewModel.updateDailySummaryEnabled(it) }
                )
            }

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
                            text = preferences?.dailySummaryTime ?: "22:00",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Alerts & Reminders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Budget Alerts", modifier = Modifier.weight(1f))
                Switch(checked = true, onCheckedChange = {})
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Goal Progress", modifier = Modifier.weight(1f))
                Switch(checked = true, onCheckedChange = {})
            }
        }
    }
}
