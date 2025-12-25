package com.yourname.moneypilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.yourname.moneypilot.ui.theme.GradientBlue
import com.yourname.moneypilot.ui.theme.GradientLavender
import com.yourname.moneypilot.ui.theme.GradientWhite

@Composable
fun GradientBackground(
    isOledMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundBrush = if (isOledMode) {
        Brush.linearGradient(listOf(Color.Black, Color.Black))
    } else {
        Brush.verticalGradient(
            colors = listOf(
                GradientBlue,
                GradientLavender,
                GradientWhite
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        content()
    }
}
