package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getAccountIcon(type: String): ImageVector {
    return when (type.uppercase()) {
        "BANK" -> Icons.Default.AccountBalance
        "CREDIT", "CREDIT_CARD" -> Icons.Default.CreditCard
        "CASH" -> Icons.Default.AccountBalanceWallet
        "INVESTMENT" -> Icons.AutoMirrored.Filled.TrendingUp
        "LOAN" -> Icons.Default.Home
        else -> Icons.Default.AccountBalanceWallet
    }
}
