package com.yourname.moneypilot.ui.features.planning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.yourname.moneypilot.ui.components.*
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is ScreenState.Success -> {
                    PlannedExpenseList(
                        state.data,
                        isPrivacyMode = isPrivacyMode,
                        onMarkPaid = { viewModel.markAsPaid(it) },
                        onEdit = onEditPlannedExpense,
                        onRecordReserve = { showReserveDialog = it }
                    )
                }
                is ScreenState.Empty -> {
                    EmptyState(
                        icon = Icons.Default.Savings,
                        title = "No planned expenses",
                        subtitle = "Plan for future committed costs like insurance or fees.",
                        action = {
                            Button(onClick = onAddPlannedExpense) { Text("Add Expense") }
                        }
                    )
                }
                else -> {}
            }
        }

        MoneyPilotFAB(
            onClick = onAddPlannedExpense,
            icon = Icons.Default.Add,
            label = "Plan Expense",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun PlannedExpenseList(
    data: BigBillsState,
    isPrivacyMode: Boolean,
    onMarkPaid: (BigBillEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onRecordReserve: (BigBillEntity) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val totalReserved = data.unpaidBills.sumOf { it.reservedAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        item {
            FinancialSummarySurface(
                title = "Planned Commitment",
                primaryValue = if(isPrivacyMode) "••••" else "₹ ${data.totalPendingAmount.toInt()}",
                progress = if(data.totalPendingAmount > 0) (totalReserved / data.totalPendingAmount).toFloat() else 0f,
                secondaryInfo = {
                    SummaryItem(label = "Reserved", value = if(isPrivacyMode) "••••" else "₹ ${totalReserved.toInt()}", color = financeColors.income)
                    val needed = (data.totalPendingAmount - totalReserved).coerceAtLeast(0.0)
                    SummaryItem(label = "Still Needed", value = if(isPrivacyMode) "••••" else "₹ ${needed.toInt()}", color = financeColors.expense)
                }
            )
        }

        if (data.unpaidBills.isNotEmpty()) {
            item { SectionHeader("Upcoming") }
            items(data.unpaidBills, key = { it.id }) { bill ->
                PlannedExpenseListItem(bill, isPrivacyMode, onMarkPaid, onRecordReserve, onEdit)
            }
        }

        if (data.paidBills.isNotEmpty()) {
            item { SectionHeader("Settled") }
            items(data.paidBills, key = { it.id }) { bill ->
                PlannedExpenseListItem(bill, isPrivacyMode, onMarkPaid, onRecordReserve, onEdit)
            }
        }
    }
}

@Composable
fun PlannedExpenseListItem(
    bill: BigBillEntity,
    isPrivacyMode: Boolean,
    onMarkPaid: (BigBillEntity) -> Unit,
    onRecordReserve: (BigBillEntity) -> Unit,
    onEdit: (Long) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val progress = if (bill.amount > 0) (bill.reservedAmount / bill.amount).toFloat().coerceIn(0f, 1f) else 0f
    
    val monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), bill.dueDate).coerceAtLeast(1)
    val monthlyTarget = ceil((bill.amount - bill.reservedAmount) / monthsRemaining).coerceAtLeast(0.0)

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(bill.id) },
        shape = RoundedCornerShape(12.dp),
        opacity = GlassLevel.High
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = bill.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Due ${bill.dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))} • ${bill.recurrenceType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
                Text(
                    text = if(isPrivacyMode) "••••" else "₹ ${bill.amount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            if (!bill.isPaid) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = if(progress >= 0.9f) financeColors.income else MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (monthlyTarget > 0) "Save ₹${monthlyTarget.toInt()}/mo" else "Fully Funded",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onRecordReserve(bill) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Record Reserve", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = { onMarkPaid(bill) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = financeColors.income, modifier = Modifier.size(20.dp))
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
        title = { Text("Record Reserve Transfer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Deduct from spending account and move to reserve: ${bill.name}", style = MaterialTheme.typography.bodySmall)
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    prefix = { Text("₹ ") },
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val walletName = wallets.find { it.id == selectedWalletId }?.name ?: "Select Wallet"
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source Wallet") },
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
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
