package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigBillsScreen(
    viewModel: BigBillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Navigate to add bill */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Big Bill")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScreenState.Success -> {
                    BigBillList(
                        state.data,
                        onMarkPaid = { viewModel.markAsPaid(it) },
                        onDelete = { viewModel.deleteBill(it) }
                    )
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No big bills tracked yet.")
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
    onMarkPaid: (BigBillEntity) -> Unit,
    onDelete: (BigBillEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            PendingBillsHeader(data.totalPendingAmount)
        }

        if (data.unpaidBills.isNotEmpty()) {
            item {
                Text("Upcoming Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(data.unpaidBills) { bill ->
                BigBillItem(bill, onMarkPaid, onDelete)
            }
        }

        if (data.paidBills.isNotEmpty()) {
            item {
                Text("Paid Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(data.paidBills) { bill ->
                BigBillItem(bill, onMarkPaid, onDelete)
            }
        }
    }
}

@Composable
fun PendingBillsHeader(amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Pending Bills", style = MaterialTheme.typography.labelMedium, color = ExpenseRed)
            Text(
                text = "₹ $amount",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = ExpenseRed
            )
        }
    }
}

@Composable
fun BigBillItem(
    bill: BigBillEntity,
    onMarkPaid: (BigBillEntity) -> Unit,
    onDelete: (BigBillEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = { if (!bill.isPaid) onMarkPaid(bill) }) {
                    Icon(
                        imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (bill.isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = bill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due: ${bill.dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "₹ ${bill.amount}",
                fontWeight = FontWeight.ExtraBold,
                color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else ExpenseRed
            )
        }
    }
}
