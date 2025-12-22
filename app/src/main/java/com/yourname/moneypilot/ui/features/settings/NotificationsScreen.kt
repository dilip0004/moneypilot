package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Alerts & Reminders", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Budget Alerts")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = true, onCheckedChange = {})
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bill Reminders")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = false, onCheckedChange = {})
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Goal Progress")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = true, onCheckedChange = {})
            }
        }
    }
}
