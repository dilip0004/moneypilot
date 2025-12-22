package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onPopBackStack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Mode")
                Spacer(modifier = Modifier.weight(1f))
                // Simple toggle for now: System, Light, Dark
                val currentMode = when (preferences?.isDarkMode) {
                    true -> "DARK"
                    false -> "LIGHT"
                    null -> "SYSTEM"
                }
                TextButton(onClick = {
                    val nextMode = when (currentMode) {
                        "SYSTEM" -> "LIGHT"
                        "LIGHT" -> "DARK"
                        else -> "SYSTEM"
                    }
                    viewModel.updateDarkMode(nextMode)
                }) {
                    Text(currentMode)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic Color (Android 12+)")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = preferences?.useDynamicColor ?: true,
                    onCheckedChange = { viewModel.updateDynamicColor(it) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("True Black (OLED)")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = preferences?.useTrueBlack ?: false,
                    onCheckedChange = { viewModel.updateTrueBlack(it) }
                )
            }
        }
    }
}
