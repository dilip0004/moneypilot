package com.yourname.moneypilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.History)
    object Goals : Screen("goals", "Goals", Icons.Default.TrackChanges)
    object Budgets : Screen("budgets", "Budgets", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
