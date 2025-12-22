package com.yourname.moneypilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Stats : Screen("reports", "Stats", Icons.Default.PieChart)
    object Transactions : Screen("transactions", "Trans.", Icons.Default.ReceiptLong)
    object Planning : Screen("planning", "Planning", Icons.Default.EventNote)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
