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
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import java.time.format.DateTimeFormatter

@Composable
fun CompactTransactionItem(
    txWithDetails: TransactionWithDetails,
    timePattern: String = "HH:mm"
) {
    val tx = txWithDetails.transaction
    Column(modifier = Modifier.padding(vertical = 0.dp, horizontal = 0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val title = txWithDetails.category?.name ?: tx.note ?: "Uncategorized"

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val sign = if (tx.type == TransactionType.Expense) "-" else "+"
            Text(
                "${sign}₹${tx.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (tx.type == TransactionType.Expense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        val timeStr = tx.dateTime.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern))
        val desc = tx.note?.trim() ?: ""
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
