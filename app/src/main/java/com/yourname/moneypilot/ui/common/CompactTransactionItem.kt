package com.yourname.moneypilot.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
        txWithDetails.goal != null -> "🎯 ${txWithDetails.goal.name}"
        txWithDetails.loan != null -> "🏦 ${txWithDetails.loan.name}"
        txWithDetails.investment != null -> "📈 ${txWithDetails.investment.name}"
        txWithDetails.category != null && txWithDetails.subcategory != null ->
            "${txWithDetails.category.name} > ${txWithDetails.subcategory.name}"
        txWithDetails.category != null ->
            txWithDetails.category.name
        else -> null
    }

    val icon = when {
        txWithDetails.goal != null -> txWithDetails.goal.icon
        txWithDetails.category != null -> txWithDetails.category.icon
        else -> "❓"
    }

    val title = when {
        !tx.note.isNullOrBlank() && (txWithDetails.category == null && txWithDetails.goal == null) -> tx.note!!
        categoryName != null -> categoryName
        else -> "Uncategorized"
    }

    val displayTitle = title

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon / Category Surface
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            
            val walletName = txWithDetails.walletFrom?.name ?: txWithDetails.walletTo?.name ?: "Unknown"
            val timeStr = tx.dateTime.toLocalTime().format(DateTimeFormatter.ofPattern(timePattern))
            val subName = txWithDetails.subcategory?.name
            val metadata = buildString {
                append(walletName)
                if (subName != null) append(" • $subName")
                append(" • $timeStr")
            }
            
            Text(
                text = metadata,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val isRefund = tx.isRefund
        val (sign, color) = when {
            tx.type == TransactionType.Income || isRefund -> "+" to financeColors.income
            tx.type == TransactionType.Expense -> "−" to financeColors.expense
            tx.type == TransactionType.Transfer -> "" to Color.White.copy(alpha = 0.7f)
            else -> "" to Color.White
        }

        val displayAmount = if (isPrivacyMode) "••••" 
        else "${sign}₹${if(tx.amount % 1.0 == 0.0) tx.amount.toInt() else String.format(Locale.getDefault(), "%.2f", tx.amount)}"

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.End
            )
            if (semanticLabel.isNotBlank() && semanticLabel != "Spent" && semanticLabel != "Received") {
                Text(
                    text = semanticLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
