package com.yourname.moneypilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.PieChart)
    object Records : Screen("records", "Records", Icons.Default.ReceiptLong)
}
