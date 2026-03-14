package com.yourname.moneypilot.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val income: Color = Color(0xFF00C853), // Bright Green
    val expense: Color = Color(0xFFFF0000), // Bright Red
    val budget: Color = Color(0xFF0067FF),
    val goal: Color = Color(0xFFFF8C00),
    val warning: Color = Color(0xFFFFA500) // Added for "Near Limit" or "Alert" states
)

val LocalFinanceColors = staticCompositionLocalOf { FinanceColors() }
