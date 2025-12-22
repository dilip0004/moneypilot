package com.yourname.moneypilot.ui.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.DashboardCard
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen

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
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    DashboardContent(state.data)
                }
                is ScreenState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Start by adding a transaction")
                    }
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
                amount = "$${state.totalBalance}"
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
                        amount = "+$${summary.totalIncome}",
                        modifier = Modifier.weight(1f),
                        containerColor = IncomeGreen.copy(alpha = 0.1f),
                        contentColor = IncomeGreen
                    )
                    DashboardCard(
                        title = "Expenses",
                        amount = "-$${summary.totalExpense}",
                        modifier = Modifier.weight(1f),
                        containerColor = ExpenseRed.copy(alpha = 0.1f),
                        contentColor = ExpenseRed
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

        items(state.recentTransactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}

@Composable
fun TransactionItem(transaction: com.yourname.moneypilot.data.local.database.entities.TransactionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = transaction.description, style = MaterialTheme.typography.bodyLarge)
                Text(text = transaction.type, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "${if (transaction.type == "EXPENSE") "-" else "+"}$${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.type == "EXPENSE") ExpenseRed else IncomeGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
