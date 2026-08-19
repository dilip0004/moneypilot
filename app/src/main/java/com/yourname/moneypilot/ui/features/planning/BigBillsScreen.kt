package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedExpensesScreen(
    onAddPlannedExpense: () -> Unit,
    onEditPlannedExpense: (Long) -> Unit,
    viewModel: BigBillsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()

    var showReserveDialog by remember { mutableStateOf<BigBillEntity?>(null) }

    if (showReserveDialog != null) {
        RecordReserveDialog(
            bill = showReserveDialog!!,
            wallets = wallets,
            onDismiss = { showReserveDialog = null },
            onConfirm = { sourceId, amount ->
                viewModel.recordReserveTransfer(showReserveDialog!!, sourceId, amount)
                showReserveDialog = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPlannedExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("planned_expense_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Planned Expense")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is ScreenState.Success -> {
                    PlannedExpenseList(
                        state.data,
                        isPrivacyMode = isPrivacyMode,
                        onMarkPaid = { viewModel.markAsPaid(it) },
                        onDelete = { viewModel.deleteBill(it) },
                        onEdit = onEditPlannedExpense,
                        onRecordReserve = { showReserveDialog = it }
                    )
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No planned expenses yet.", modifier = Modifier.testTag("planned_expenses_empty_state"))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PlannedExpenseList(
    data: BigBillsState,
    isPrivacyMode: Boolean,
    onMarkPaid: (BigBillEntity) -> Unit,
    onDelete: (BigBillEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onRecordReserve: (BigBillEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("planned_expenses_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            PlannedExpensesSummaryCard(data, isPrivacyMode)
        }

        if (data.unpaidBills.isNotEmpty()) {
            item {
                Text("Upcoming Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
                    items(data.unpaidBills, key = { it.id }) { bill ->
                        PlannedExpenseItem(
                            bill = bill,
                            isPrivacyMode = isPrivacyMode,
                            onMarkPaid = onMarkPaid,
                            onDelete = onDelete,
                            onEdit = onEdit,
                            onRecordReserve = onRecordReserve
                        )
                    }
        }

        if (data.paidBills.isNotEmpty()) {
            item {
                Text("Paid / Completed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
                    items(data.paidBills, key = { it.id }) { bill ->
                        PlannedExpenseItem(
                            bill = bill,
                            isPrivacyMode = isPrivacyMode,
                            onMarkPaid = onMarkPaid,
                            onDelete = onDelete,
                            onEdit = onEdit,
                            onRecordReserve = onRecordReserve
                        )
                    }
        }
    }
}

@Composable
fun PlannedExpensesSummaryCard(data: BigBillsState, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth().testTag("planned_expenses_summary"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Planned Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Total Upcoming", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if(isPrivacyMode) "••••" else "₹ ${data.totalPendingAmount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val needed = (data.totalPendingAmount - data.unpaidBills.sumOf { it.reservedAmount }).coerceAtLeast(0.0)
                    Text("Still Needed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if(isPrivacyMode) "••••" else "₹ $needed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = financeColors.expense
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedExpenseItem(
    bill: BigBillEntity,
    isPrivacyMode: Boolean,
    onMarkPaid: (BigBillEntity) -> Unit,
    onDelete: (BigBillEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onRecordReserve: (BigBillEntity) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val progress = if (bill.amount > 0) (bill.reservedAmount / bill.amount).toFloat().coerceIn(0f, 1f) else 0f
    
    val monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), bill.dueDate).coerceAtLeast(1)
    val monthlyTarget = ceil((bill.amount - bill.reservedAmount) / monthsRemaining).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(bill.id) }
            .testTag("planned_expense_card_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due: ${bill.dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if(isPrivacyMode) "••••" else "₹ ${bill.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
            }

            if (!bill.isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Reserved: ${if(isPrivacyMode) "••••" else "₹ ${bill.reservedAmount}"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (monthlyTarget > 0) "Next: ₹${monthlyTarget.toInt()}/mo" else "Funded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        TextButton(onClick = { onRecordReserve(bill) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Record Reserve", fontSize = 11.sp)
                        }
                        IconButton(onClick = { onMarkPaid(bill) }) {
                            Icon(Icons.Default.CheckCircle, null, tint = financeColors.income, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDelete(bill) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordReserveDialog(
    bill: BigBillEntity,
    wallets: List<com.yourname.moneypilot.data.local.database.entities.WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<Long?>(wallets.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Reserve Transfer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Transfer money from your spending account to your reserve account: ${bill.name}")
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount to Reserve") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    prefix = { Text("₹ ") }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val walletName = wallets.find { it.id == selectedWalletId }?.name ?: "Select Source Wallet"
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet.name) },
                                onClick = {
                                    selectedWalletId = wallet.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = amount.toDoubleOrNull() != null && selectedWalletId != null,
                onClick = { onConfirm(selectedWalletId!!, amount.toDouble()) }
            ) { Text("Confirm Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
