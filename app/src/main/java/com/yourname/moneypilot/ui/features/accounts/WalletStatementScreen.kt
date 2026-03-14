package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletStatementScreen(
    onPopBackStack: () -> Unit,
    viewModel: WalletStatementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
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
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(state.wallet?.name ?: "Statement", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${state.endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            StatementSummary(state)

            // Table Header
            StatementTableHeader()

            // Ledger Body
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.transactions) { txDetails ->
                        LedgerRow(txDetails, state.wallet?.id ?: -1L)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatementSummary(state: WalletStatementState) {
    val financeColors = LocalFinanceColors.current
    val walletId = state.wallet?.id ?: -1L
    
    val totalInflow = state.transactions.filter { 
        it.transaction.type == TransactionType.Income || (it.transaction.type == TransactionType.Transfer && it.transaction.walletToId == walletId)
    }.sumOf { it.transaction.amount }

    val totalOutflow = state.transactions.filter { 
        it.transaction.type == TransactionType.Expense || (it.transaction.type == TransactionType.Transfer && it.transaction.walletFromId == walletId)
    }.sumOf { it.transaction.amount }

    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryColumn("Opening", "₹${state.openingBalance}")
            SummaryColumn("Inflow", "+₹$totalInflow", color = financeColors.income)
            SummaryColumn("Outflow", "-₹$totalOutflow", color = financeColors.expense)
            SummaryColumn("Closing", "₹${state.openingBalance + totalInflow - totalOutflow}")
        }
    }
}

@Composable
fun SummaryColumn(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun StatementTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Date", modifier = Modifier.weight(0.15f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("Description", modifier = Modifier.weight(0.45f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("Type", modifier = Modifier.weight(0.15f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Amount", modifier = Modifier.weight(0.25f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
fun LedgerRow(txDetails: com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails, currentWalletId: Long) {
    val tx = txDetails.transaction
    val financeColors = LocalFinanceColors.current
    
    // Determine if it's a Credit (CR), Debit (DR), or Transfer (TR) relative to THIS wallet
    val (typeLabel, amountColor, sign) = when (tx.type) {
        TransactionType.Income -> Triple("CR", financeColors.income, "+")
        TransactionType.Expense -> Triple("DR", financeColors.expense, "-")
        TransactionType.Transfer -> {
            if (tx.walletToId == currentWalletId) Triple("TR", financeColors.income, "+")
            else Triple("TR", financeColors.expense, "-")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            text = tx.dateTime.format(DateTimeFormatter.ofPattern("dd MMM")),
            modifier = Modifier.weight(0.15f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )

        // Description / Category
        Column(modifier = Modifier.weight(0.45f)) {
            Text(
                text = txDetails.category?.name ?: "Transfer",
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
        Text(
            text = typeLabel,
            modifier = Modifier.weight(0.15f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = amountColor.copy(alpha = 0.8f)
        )

        // Amount
        Text(
            text = "$sign₹${tx.amount}",
            modifier = Modifier.weight(0.25f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = amountColor
        )
    }
}
