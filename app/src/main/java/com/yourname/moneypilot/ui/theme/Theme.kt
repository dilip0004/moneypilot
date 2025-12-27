package com.yourname.moneypilot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7B5CFA),
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93)
)

private val OLEDColorScheme = darkColorScheme(
    primary = Color(0xFF7B5CFA), // Keep the accent color for actions
    onPrimary = Color.White,
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8E8E93)
)

@Composable
fun MoneyPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    trueBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme && trueBlack) {
        OLEDColorScheme
    } else if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF7B5CFA),
            background = Color(0xFF1C1C1E),
            surface = Color(0xFF1C1C1E)
        )
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
