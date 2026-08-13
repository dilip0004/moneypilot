package com.yourname.moneypilot.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> MoneyPilotSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelExtractor: (T) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    showIcon: Boolean = true,
    iconExtractor: ((T) -> ImageVector?)? = null
) {
    val selectedIndex = options.indexOf(selectedOption)
    val primary = MaterialTheme.colorScheme.primary
    
    // Spec 4 & 12: Accent-derived luminous gradient (Polished intensity)
    val selectedGradient = remember(primary) {
        Brush.linearGradient(
            colors = listOf(
                primary,
                primary.copy(alpha = 0.85f),
                primary.copy(alpha = 0.7f)
            )
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        val maxWidth = this.maxWidth
        val itemWidth = maxWidth / options.size
        
        // Spec 13: Smooth selection animation
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "indicator_offset"
        )

        // Spec 2: Luminous gradient background for selected option
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .padding(3.dp)
                .background(selectedGradient, RoundedCornerShape(10.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
        )

        // Labels
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val icon = iconExtractor?.invoke(option)
                        if (isSelected && (showIcon || icon != null)) {
                            Icon(
                                imageVector = icon ?: Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).padding(end = 4.dp),
                                tint = Color.White
                            )
                        }
                        Text(
                            text = labelExtractor(option),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
