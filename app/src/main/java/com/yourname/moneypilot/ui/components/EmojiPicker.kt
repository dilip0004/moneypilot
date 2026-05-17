package com.yourname.moneypilot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPicker(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf(
        "💰", "🏦", "💳", "💵", "💸", "💹", "💎", "🏠", 
        "🚗", "🛒", "🍔", "🎬", "✈️", "🎓", "🎁", "📱", 
        "💻", "⌚", "🎮", "🏋️", "💊", "🐶", "🎨", "🎸"
    )

    // Set userScrollEnabled = false to prevent nested scroll conflicts
    // when placed inside a scrollable Column.
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier.height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp),
        userScrollEnabled = false 
    ) {
        items(emojis) { emoji ->
            Surface(
                onClick = { onEmojiSelected(emoji) },
                shape = CircleShape,
                color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                modifier = Modifier.size(44.dp),
                border = if (selectedEmoji == emoji) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
