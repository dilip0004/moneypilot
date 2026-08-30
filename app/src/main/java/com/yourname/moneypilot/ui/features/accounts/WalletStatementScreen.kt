package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.components.*
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.util.formatCompact
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletStatementScreen(
    onPopBackStack: () -> Unit,
    viewModel: WalletStatementViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val currencySymbol = preferences?.currency ?: "₹"
    
    var showDateRangePicker by remember { mutableStateOf(false) }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        val start = Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val end = Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.onDateRangeChanged(start, end)
                    }
                    showDateRangePicker = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { 
                    Column {
                        Text(state.wallet?.name ?: "Statement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${state.endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDateRangePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Range")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header Stats Area
            StatementSummary(state, isPrivacyMode, currencySymbol)

            // Table Header
            StatementTableHeader()

            // Ledger Body
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.transactions, key = { it.transaction.id }) { txDetails ->
                        LedgerRow(txDetails, state.wallet?.id ?: -1L, isPrivacyMode, currencySymbol)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun StatementSummary(state: WalletStatementState, isPrivacyMode: Boolean, currencySymbol: String) {
    val financeColors = LocalFinanceColors.current
    val walletId = state.wallet?.id ?: -1L
    
    val totalInflow = state.transactions.filter { 
        it.transaction.type == TransactionType.Income || (it.transaction.type == TransactionType.Transfer && it.transaction.walletToId == walletId)
    }.sumOf { it.transaction.amount }

    val totalOutflow = state.transactions.filter { 
        it.transaction.type == TransactionType.Expense || (it.transaction.type == TransactionType.Transfer && it.transaction.walletFromId == walletId)
    }.sumOf { it.transaction.amount }

    val closing = state.openingBalance + totalInflow - totalOutflow

    FinancialSummarySurface(
        title = "Ledger Summary",
        primaryValue = if(isPrivacyMode) "••••" else currencySymbol + closing.toInt(),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        secondaryInfo = {
            SummaryItem(label = "Opening", value = if(isPrivacyMode) "••••" else state.openingBalance.formatCompact(currencySymbol), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SummaryItem(label = "Inflow", value = if(isPrivacyMode) "••••" else "+" + totalInflow.formatCompact(currencySymbol), color = financeColors.income)
            SummaryItem(label = "Outflow", value = if(isPrivacyMode) "••••" else "−" + totalOutflow.formatCompact(currencySymbol), color = financeColors.expense)
        }
    )
}

@Composable
fun StatementTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("DATE", modifier = Modifier.weight(0.18f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("DESCRIPTION", modifier = Modifier.weight(0.42f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("TYPE", modifier = Modifier.weight(0.15f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("AMOUNT", modifier = Modifier.weight(0.25f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LedgerRow(
    txDetails: com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails, 
    currentWalletId: Long,
    isPrivacyMode: Boolean,
    currencySymbol: String
) {
    val tx = txDetails.transaction
    val financeColors = LocalFinanceColors.current
    
    val (typeLabel, amountColor, sign) = when (tx.type) {
        TransactionType.Income -> Triple("CR", financeColors.income, "+")
        TransactionType.Expense -> Triple("DR", financeColors.expense, "−")
        TransactionType.Transfer -> {
            if (tx.walletToId == currentWalletId) Triple("TR", financeColors.income, "+")
            else Triple("TR", financeColors.expense, "−")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            text = tx.dateTime.format(DateTimeFormatter.ofPattern("dd MMM")),
            modifier = Modifier.weight(0.18f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        // Description / Category
        Column(modifier = Modifier.weight(0.42f)) {
            val title = when {
                txDetails.goal != null -> "🎯 ${txDetails.goal.name}"
                txDetails.category != null -> txDetails.category.name
                else -> "Transfer"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!tx.note.isNullOrBlank()) {
                Text(
                    text = tx.note!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
            }
        }

        // Type (CR/DR/TR)
        Surface(
            modifier = Modifier.weight(0.15f),
            color = amountColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = amountColor,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Amount
        val displayAmount = if(isPrivacyMode) "••••" 
        else "$sign$currencySymbol${if(tx.amount % 1.0 == 0.0) tx.amount.toInt() else String.format(Locale.getDefault(), "%.2f", tx.amount)}"
        
        Text(
            text = displayAmount,
            modifier = Modifier.weight(0.25f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            color = amountColor
        )
    }
}
