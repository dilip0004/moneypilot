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
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.format.DateTimeFormatter

@Composable
fun CompactTransactionItem(
    txWithDetails: TransactionWithDetails,
    timePattern: String = "HH:mm"
) {
    val tx = txWithDetails.transaction
    val financeColors = LocalFinanceColors.current

    Column(modifier = Modifier.padding(vertical = 0.dp, horizontal = 0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Construct hierarchical title (Category > Subcategory)
        val title = when {
            txWithDetails.category != null && txWithDetails.subcategory != null -> 
                "${txWithDetails.category.name} > ${txWithDetails.subcategory.name}"
            txWithDetails.category != null -> 
                txWithDetails.category.name
            !tx.note.isNullOrBlank() -> 
                tx.note
            else -> "Uncategorized"
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val (sign, color) = when (tx.type) {
                TransactionType.Income -> "+" to financeColors.income
                TransactionType.Expense -> "-" to financeColors.expense
                TransactionType.Transfer -> "" to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                "${sign}₹${tx.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        val timeStr = tx.dateTime.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern))
        val desc = tx.note?.trim() ?: ""
        // If the title is already using the note, don't repeat it in the second line
        val secondLine = if (desc.isNotEmpty() && title != desc) "$timeStr • $desc" else timeStr

        Text(
            text = secondLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
