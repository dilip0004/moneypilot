package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun BigBillsScreen(
    onAddBigBill: () -> Unit,
    onEditBigBill: (Long) -> Unit,
    viewModel: BigBillsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBigBill,
                modifier = Modifier.testTag("big_bill_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Big Bill")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ScreenState.Success -> {
                    BigBillList(
                        state.data,
                        isPrivacyMode = isPrivacyMode,
                        onMarkPaid = { viewModel.markAsPaid(it) },
                        onDelete = { viewModel.deleteBill(it) },
                        onEdit = onEditBigBill,
                        onCreateMonthlyTransfer = { viewModel.createMonthlyTransferForBill(it) }
                    )
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No big bills tracked yet.", modifier = Modifier.testTag("big_bills_empty_state"))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun BigBillList(
    data: BigBillsState,
    isPrivacyMode: Boolean,
    onMarkPaid: (BigBillEntity) -> Unit,
    onDelete: (BigBillEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onCreateMonthlyTransfer: (BigBillEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("big_bills_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            PendingBillsHeader(data.totalPendingAmount, isPrivacyMode)
        }

        if (data.unpaidBills.isNotEmpty()) {
            item {
                Text("Upcoming Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(data.unpaidBills) { bill ->
                BigBillItem(
                    bill = bill,
                    isPrivacyMode = isPrivacyMode,
                    onMarkPaid = onMarkPaid,
                    onDelete = onDelete,
                    onEdit = onEdit,
                    onCreateMonthlyTransfer = onCreateMonthlyTransfer
                )
            }
        }

        if (data.paidBills.isNotEmpty()) {
            item {
                Text("Paid Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(data.paidBills) { bill ->
                BigBillItem(
                    bill = bill,
                    isPrivacyMode = isPrivacyMode,
                    onMarkPaid = onMarkPaid,
                    onDelete = onDelete,
                    onEdit = onEdit,
                    onCreateMonthlyTransfer = onCreateMonthlyTransfer
                )
            }
        }
    }
}

@Composable
fun PendingBillsHeader(amount: Double, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    val hasPending = amount > 0
    val statusColor = if (hasPending) financeColors.expense else financeColors.income

    Card(
        modifier = Modifier.fillMaxWidth().testTag("pending_bills_header"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasPending) "Total Pending Bills" else "All Bills Paid",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
            val displayAmount = if(isPrivacyMode) "••••" else "₹ $amount"
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
                modifier = Modifier.testTag("pending_bills_amount")
            )
        }
    }
}

@Composable
fun BigBillItem(
    bill: BigBillEntity,
    isPrivacyMode: Boolean,
    onMarkPaid: (BigBillEntity) -> Unit,
    onDelete: (BigBillEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onCreateMonthlyTransfer: (BigBillEntity) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(bill.id) }
            .testTag("big_bill_card_${bill.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = { if (!bill.isPaid) onMarkPaid(bill) },
                        modifier = Modifier.testTag("big_bill_mark_paid_${bill.id}")
                    ) {
                        Icon(
                            imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (bill.isPaid) financeColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = bill.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("big_bill_name_${bill.id}")
                        )
                        Text(
                            text = "Due: ${bill.dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayAmount = if(isPrivacyMode) "••••" else "₹ ${bill.amount}"
                    Text(
                        text = displayAmount,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else financeColors.expense,
                        modifier = Modifier.testTag("big_bill_amount_${bill.id}")
                    )
                    IconButton(
                        onClick = { onDelete(bill) },
                        modifier = Modifier.testTag("big_bill_delete_${bill.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            }

            if (!bill.isPaid && bill.autoReserveFlag) {
                val monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), bill.dueDate).coerceAtLeast(1)
                val monthlyTarget = ceil(bill.amount / monthsRemaining)

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val reserveLabel = if(isPrivacyMode) "••••" else "₹ $monthlyTarget"
                        Text(
                            text = "Reserve: $reserveLabel / month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("big_bill_reserve_target_${bill.id}")
                        )
                    }
                    Button(
                        onClick = { onCreateMonthlyTransfer(bill) },
                        modifier = Modifier.height(32.dp).testTag("big_bill_create_transfer_${bill.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Reserve Now", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
