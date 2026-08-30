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
    fontFamilyName: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val financeColors = FinanceColors()

    val colorScheme = when {
        darkTheme && trueBlack -> darkColorScheme(
            primary = accentColor,
            error = financeColors.expense,
            background = Color.Black,
            surface = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = Color.Black,
            onSurfaceVariant = Color(0xFF8E8E93)
        )
        darkTheme -> darkColorScheme(
            primary = accentColor,
            secondary = accentColor.copy(alpha = 0.8f),
            tertiary = Color(0xFF00C853),
            error = financeColors.expense,
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            onBackground = Color(0xFFE6E1E5),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF21262D),
            onSurfaceVariant = Color(0xFF8E8E93)
        )
        else -> lightColorScheme(
            primary = accentColor,
            secondary = accentColor.copy(alpha = 0.8f),
            tertiary = Color(0xFF00C853),
            error = financeColors.expense,
            onPrimary = Color.White,
            background = Color(0xFFF0F2F5),
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

    // Get dynamic typography based on preference
    val typography = getTypography(fontFamilyName)

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
