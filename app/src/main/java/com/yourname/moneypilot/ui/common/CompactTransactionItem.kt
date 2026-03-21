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
import androidx.compose.ui.unit.sp
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

    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // EMI Transparency (Sub-detail for automated loan entries)
                if (tx.transactionSourceType == "AUTO_EMI_PRINCIPAL") {
                    Text("Principal Repayment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (tx.transactionSourceType == "AUTO_EMI_INTEREST") {
                    Text("Interest Component", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Refund Behavior & Color Logic
            val isRefund = tx.note?.contains("Refund", ignoreCase = true) == true
            val (sign, color) = when {
                tx.type == TransactionType.Income || isRefund -> "+" to financeColors.income
                tx.type == TransactionType.Expense -> "-" to financeColors.expense
                tx.type == TransactionType.Transfer -> "" to MaterialTheme.colorScheme.onSurfaceVariant
                else -> "" to MaterialTheme.colorScheme.onSurface
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
