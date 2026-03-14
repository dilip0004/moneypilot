package com.yourname.moneypilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MoneyPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    trueBlack: Boolean = false,
    accentColor: Color = Color(0xFF7B5CFA),
    content: @Composable () -> Unit
) {
    val financeColors = FinanceColors() // Get our bright palette

    val colorScheme = when {
        darkTheme && trueBlack -> darkColorScheme(
            primary = accentColor,
            error = financeColors.expense, // Force Bright Red
            background = Color.Black,
            surface = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = Color.Black,
            onSurfaceVariant = Color(0xFF8E8E93)
        )
        darkTheme -> darkColorScheme(
            primary = accentColor,
            error = financeColors.expense, // Force Bright Red
            background = Color(0xFF1C1C1E),
            surface = Color(0xFF1C1C1E),
            onBackground = Color(0xFFE6E1E5),
            onSurface = Color(0xFFE6E1E5)
        )
        else -> lightColorScheme(
            primary = accentColor,
            error = financeColors.expense, // Force Bright Red
            onPrimary = Color.White,
            background = Color.White,
            surface = Color.White,
            onBackground = Color(0xFF1C1C1E),
            onSurface = Color(0xFF1C1C1E),
            surfaceVariant = Color(0xFFF2F2F7),
            onSurfaceVariant = Color(0xFF8E8E93)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
