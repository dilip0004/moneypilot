package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToWebApp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader("App Preferences")
            SettingsItem(
                title = "Appearance & Personalization",
                subtitle = "Themes, OLED black, and fonts",
                icon = Icons.Default.Palette,
                onClick = onNavigateToAppearance
            )
            SettingsItem(
                title = "Security & Privacy",
                subtitle = "PIN, Biometrics, and amount blurring",
                icon = Icons.Default.Security,
                onClick = onNavigateToSecurity
            )
            SettingsItem(
                title = "Notifications",
                subtitle = "Reminders and budget alerts",
                icon = Icons.Default.Notifications,
                onClick = onNavigateToNotifications
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsHeader("Governance")
            SettingsItem(
                title = "Categories & Subcategories",
                subtitle = "Manage the hierarchy of your transactions",
                icon = Icons.Default.Category,
                onClick = onNavigateToCategories
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsHeader("System")
            SettingsItem(
                title = "Backup & Restore",
                subtitle = "Export data to CSV, Excel, or JSON",
                icon = Icons.Default.Backup,
                onClick = onNavigateToBackup
            )
            SettingsItem(
                title = "App Diagnostics",
                subtitle = "Run system integrity & functional tests",
                icon = Icons.Default.HealthAndSafety,
                onClick = onNavigateToDiagnostics
            )
            SettingsItem(
                title = "Web Desktop Access",
                subtitle = "Access MoneyPilot from your computer browser",
                icon = Icons.Default.Computer,
                onClick = onNavigateToWebApp
            )
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
