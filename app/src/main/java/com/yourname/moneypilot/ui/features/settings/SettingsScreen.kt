package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToWebApp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    var showReserveWalletDialog by remember { mutableStateOf(false) }

    if (showReserveWalletDialog) {
        AlertDialog(
            onDismissRequest = { showReserveWalletDialog = false },
            title = { Text("Default Reserve Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select account for planned expense savings:", style = MaterialTheme.typography.bodyMedium)
                    wallets.forEach { wallet ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.updateGlobalReserveWalletId(wallet.id)
                                    showReserveWalletDialog = false 
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = preferences?.globalReserveWalletId == wallet.id,
                                onClick = null 
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(wallet.name)
                        }
                    }
                    TextButton(onClick = { 
                        viewModel.updateGlobalReserveWalletId(null)
                        showReserveWalletDialog = false
                    }) {
                        Text("Clear Selection", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReserveWalletDialog = false }) { Text("Close") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text("Settings", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SettingsHeader("App Preferences")
            SettingsGroupSurface {
                SettingsItemRow(
                    title = "Appearance & Personalization",
                    subtitle = "Themes, OLED black, and fonts",
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToAppearance
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "Security & Privacy",
                    subtitle = "PIN, Biometrics, and amount blurring",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToSecurity
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "Notifications",
                    subtitle = "Reminders and budget alerts",
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotifications
                )
            }

            SettingsHeader("Governance")
            SettingsGroupSurface {
                SettingsItemRow(
                    title = "Categories & Subcategories",
                    subtitle = "Manage the hierarchy of your transactions",
                    icon = Icons.Default.Category,
                    onClick = onNavigateToCategories
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "Planned Expense Reserve",
                    subtitle = preferences?.globalReserveWalletId?.let { id ->
                        wallets.find { it.id == id }?.name
                    } ?: "Default destination for future savings",
                    icon = Icons.Default.Savings,
                    onClick = { showReserveWalletDialog = true }
                )
            }

            SettingsHeader("System")
            SettingsGroupSurface {
                SettingsItemRow(
                    title = "Backup & Restore",
                    subtitle = "Export data to CSV, Excel, or JSON",
                    icon = Icons.Default.Backup,
                    onClick = onNavigateToBackup
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "App Diagnostics",
                    subtitle = "Run system integrity & functional tests",
                    icon = Icons.Default.HealthAndSafety,
                    onClick = onNavigateToDiagnostics
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "Web Desktop Access",
                    subtitle = "Access MoneyPilot from your computer browser",
                    icon = Icons.Default.Computer,
                    onClick = onNavigateToWebApp
                )
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsGroupSurface(content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        opacity = 0.15f,
        shape = RoundedCornerShape(16.dp),
        content = content
    )
}

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.05f)
    )
}
