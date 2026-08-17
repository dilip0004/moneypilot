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
import java.util.Locale
import java.time.format.DateTimeFormatter

@Composable
fun CompactTransactionItem(
    txWithDetails: TransactionWithDetails,
    timePattern: String = "HH:mm",
    isPrivacyMode: Boolean = false
) {
    val tx = txWithDetails.transaction
    val financeColors = LocalFinanceColors.current

    // Semantic label based on transaction source type (TASK-LEDGER-LABELS)
    val semanticLabel = when (tx.transactionSourceType) {
        "LOAN_REPAYMENT" -> "Repaid"
        "GOAL_CONTRIBUTION" -> "Saved"
        "GOAL_WITHDRAWAL" -> "Withdrawn"
        "INVESTMENT_BUY" -> "Invested"
        "INVESTMENT_SELL" -> "Sold"
        "BIG_BILL_SETTLEMENT" -> "Reserved"
        else -> when (tx.type) {
            TransactionType.Expense -> "Spent"
            TransactionType.Income -> "Received"
            else -> ""
        }
    }

    val categoryName = when {
        txWithDetails.category != null && txWithDetails.subcategory != null ->
            "${txWithDetails.category.name} > ${txWithDetails.subcategory.name}"
        txWithDetails.category != null ->
            txWithDetails.category.name
        else -> null
    }

    val title = when {
        !tx.note.isNullOrBlank() && categoryName == null -> tx.note!!
        categoryName != null -> categoryName
        else -> "Uncategorized"
    }

    val displayTitle = if (semanticLabel.isNotBlank()) "$title ($semanticLabel)" else title

    Column(modifier = Modifier.padding(vertical = 2.dp, horizontal = 0.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val isRefund = tx.note?.contains("Refund", ignoreCase = true) == true
            val (sign, color) = when {
                tx.type == TransactionType.Income || isRefund -> "+" to financeColors.income
                tx.type == TransactionType.Expense -> "-" to financeColors.expense
                tx.type == TransactionType.Transfer -> "" to MaterialTheme.colorScheme.onSurfaceVariant
                else -> "" to MaterialTheme.colorScheme.onSurface
            }

            val displayAmount = if (isPrivacyMode) "••••" 
            else "${sign}₹${if(tx.amount % 1.0 == 0.0) tx.amount.toInt() else String.format(Locale.getDefault(), "%.2f", tx.amount)}"

            Text(
                displayAmount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }

        val timeStr = tx.dateTime.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern))
        val desc = tx.note?.trim() ?: ""
        val secondLine = if (desc.isNotEmpty() && title != desc) "$timeStr • $desc" else timeStr

        Text(
            text = secondLine,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
