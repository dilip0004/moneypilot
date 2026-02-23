package com.yourname.moneypilot.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import java.time.format.DateTimeFormatter

@Composable
fun CompactTransactionItem(
    tx: TransactionWithCategory,
    timePattern: String = "HH:mm"
) {
    Column(modifier = Modifier.padding(vertical = 0.dp, horizontal = 0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val title = listOfNotNull(tx.category?.name, tx.subcategory?.name)
            .joinToString(" • ")
            .ifBlank { tx.goal?.name ?: "Uncategorized" }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val sign = if (tx.transaction.type == "EXPENSE") "-" else "+"
            Text(
                "${sign}₹${tx.transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (tx.transaction.type == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        val timeStr = tx.transaction.date.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern))
        val desc = tx.transaction.description.trim()
        val secondLine = if (desc.isNotEmpty()) "$timeStr • $desc" else timeStr

        Text(
            text = secondLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
