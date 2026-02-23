package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onPopBackStack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()

    val primaryColors = listOf(
        0xFF7F3DFF, // Purple (Default)
        0xFF0067FF, // Blue
        0xFF00A36C, // Green
        0xFFFF5733, // Orange
        0xFFE91E63, // Pink
        0xFF607D8B, // Gray
        0xFF000000, // Black
        0xFF673AB7  // Indigo
    )

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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SectionHeader("Theme Mode")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark Mode")
                Spacer(modifier = Modifier.weight(1f))
                val currentMode = when (preferences?.theme?.name) {
                    "DARK" -> "DARK"
                    "LIGHT" -> "LIGHT"
                    "OLED" -> "OLED"
                    else -> "SYSTEM"
                }
                TextButton(onClick = {
                    val nextMode = when (currentMode) {
                        "SYSTEM" -> com.yourname.moneypilot.data.local.preferences.AppTheme.LIGHT
                        "LIGHT" -> com.yourname.moneypilot.data.local.preferences.AppTheme.DARK
                        "DARK" -> com.yourname.moneypilot.data.local.preferences.AppTheme.OLED
                        else -> com.yourname.moneypilot.data.local.preferences.AppTheme.SYSTEM
                    }
                    viewModel.updateTheme(nextMode)
                }) {
                    Text(currentMode)
                }
            }

            SectionHeader("Primary Color")
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(primaryColors) { colorInt ->
                    val color = Color(colorInt)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { viewModel.updatePrimaryColor(colorInt.toInt()) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (preferences?.primaryColor == colorInt.toInt()) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            SectionHeader("Advanced")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic Color (Android 12+)")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = preferences?.useDynamicColor ?: true,
                    onCheckedChange = { viewModel.updateDynamicColor(it) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("True Black (pure black surfaces)")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = preferences?.useTrueBlack ?: false,
                    onCheckedChange = { viewModel.updateTrueBlack(it) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}
