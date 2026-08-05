package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onPopBackStack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val scrollState = rememberScrollState()

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

    val fontOptions = listOf(
        "DEFAULT" to "System Default",
        "SANS_SERIF" to "Modern Sans",
        "SERIF" to "Classic Serif",
        "MONOSPACE" to "Tech Monospace"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                modifier = Modifier.statusBarsPadding(),
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
                .padding(16.dp)
                .verticalScroll(scrollState),
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

            SectionHeader("Typography & Fonts")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fontOptions.forEach { (fontKey, displayName) ->
                    val isSelected = (preferences?.fontFamily ?: "DEFAULT") == fontKey
                    Surface(
                        onClick = { viewModel.updateFontFamily(fontKey) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) borderStroke() else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = when(fontKey) {
                                        "SERIF" -> FontFamily.Serif
                                        "MONOSPACE" -> FontFamily.Monospace
                                        "SANS_SERIF" -> FontFamily.SansSerif
                                        else -> FontFamily.Default
                                    }
                                )
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
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
                val useDynamic = preferences?.useDynamicColor ?: true
                Switch(
                    checked = useDynamic,
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
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}
