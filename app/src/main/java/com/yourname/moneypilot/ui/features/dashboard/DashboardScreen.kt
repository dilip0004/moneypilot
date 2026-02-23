package com.yourname.moneypilot.ui.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.DashboardCard
// use MaterialTheme.colorScheme.income / expense
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "MoneyPilot",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Success -> DashboardContent(state.data)
                is ScreenState.Error -> Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                is ScreenState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Start by adding a transaction")
                }
            }
        }
    }
}

@Composable
fun DashboardContent(state: DashboardState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardCard(
                title = "Total Balance",
                amount = "INR ${state.totalBalance}"
            )
        }

        state.monthlySummary?.let { summary ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardCard(
                        title = "Income",
                        amount = "+₹${summary.totalIncome}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.income.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.income
                    )
                    DashboardCard(
                        title = "Expenses",
                        amount = "-₹${summary.totalExpense}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.expense.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.expense
                    )
                }
            }
        }

        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(state.recentTransactions) { tx ->
            TransactionItem(tx)
        }
    }
}

@Composable
private fun TransactionItem(tx: TransactionEntity) {
    val title = tx.description.ifBlank { "Transaction" }

    val (icon, iconBg) = when (tx.type) {
        "INCOME" -> Icons.Filled.ArrowUpward to MaterialTheme.colorScheme.primaryContainer
        "EXPENSE" -> Icons.Filled.ArrowDownward to MaterialTheme.colorScheme.errorContainer
        else -> Icons.Filled.SwapHoriz to MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = tx.type)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Text(
                        text = tx.date.format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val sign = if (tx.type == "EXPENSE") "-" else "+"
            Text(
                text = "${sign}₹${tx.amount}",
                style = MaterialTheme.typography.titleLarge,
                color = if (tx.type == "EXPENSE") MaterialTheme.colorScheme.expense else MaterialTheme.colorScheme.income,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
