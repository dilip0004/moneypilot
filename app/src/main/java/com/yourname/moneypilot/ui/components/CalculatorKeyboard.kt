package com.yourname.moneypilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorKeyboard(
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    initialValue: String = ""
) {
    var expression by remember { mutableStateOf(initialValue) }

    fun onKeyClick(key: String) {
        when (key) {
            "C" -> expression = ""
            "⌫" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
            "=" -> {
                try {
                    val result = evaluateExpression(expression)
                    expression = result.toString()
                } catch (e: Exception) {
                    // Handle error
                }
            }
            else -> expression += key
        }
        onValueChange(expression)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        val keys = listOf(
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "C", "+"),
            listOf("⌫", "=", "Done")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val weight = if (key == "Done") 2f else 1f
                    CalculatorKey(
                        text = key,
                        onClick = { if (key == "Done") onDone() else onKeyClick(key) },
                        modifier = Modifier.weight(weight),
                        containerColor = when {
                            key == "Done" -> MaterialTheme.colorScheme.primary
                            key in listOf("/", "*", "-", "+", "=") -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CalculatorKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "⌫") {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null)
        } else if (text == "Done") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Minimalistic expression evaluator for basic arithmetic
fun evaluateExpression(expression: String): Double {
    if (expression.isEmpty()) return 0.0
    // Simplified logic: In a production app, use a proper library or more robust parser
    return try {
        val sanitized = expression.replace(",", ".")
        // This is a very basic placeholder for demonstration
        sanitized.toDoubleOrNull() ?: 0.0
    } catch (e: Exception) {
        0.0
    }
}
