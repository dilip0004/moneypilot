package com.yourname.moneypilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Transactions : Screen("transactions_hub", "Trans.", Icons.AutoMirrored.Filled.ReceiptLong)
    object Stats : Screen("stats_hub", "Stats", Icons.Default.BarChart)
    object Accounts : Screen("accounts_hub", "Accounts", Icons.Default.AccountBalanceWallet)
    object More : Screen("more_hub", "More", Icons.Default.MoreHoriz)
}
