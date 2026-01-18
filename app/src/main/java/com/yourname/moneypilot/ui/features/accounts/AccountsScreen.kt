package com.yourname.moneypilot.ui.features.accounts

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.ui.common.ScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accounts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ScreenState.Empty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No accounts yet")
                }
                is ScreenState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Something went wrong")
                }
                is ScreenState.Success -> {
                    val data = state.data
                    Column {
                        PeriodChips(
                            period = data.period,
                            onPeriod = viewModel::setPeriod
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(data.accounts.filter { !it.isArchived }) { acc ->
                                AccountCard(
                                    account = acc,
                                    period = data.period,
                                    lastMonthSnapshot = data.lastMonthSnapshots[acc.id],
                                    onArchive = { viewModel.archiveAccount(acc) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChips(period: AccountsPeriod, onPeriod: (AccountsPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        FilterChip(
            selected = period == AccountsPeriod.THIS_MONTH,
            onClick = { onPeriod(AccountsPeriod.THIS_MONTH) },
            label = { Text("This month") }
        )
        FilterChip(
            selected = period == AccountsPeriod.LAST_MONTH,
            onClick = { onPeriod(AccountsPeriod.LAST_MONTH) },
            label = { Text("Last month") }
        )
    }
}

@Composable
private fun AccountCard(
    account: AccountEntity,
    period: AccountsPeriod,
    lastMonthSnapshot: com.yourname.moneypilot.data.local.database.entities.MonthlyAccountSnapshotEntity?,
    onArchive: () -> Unit
) {
    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                    Text(account.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${account.currentBalance}", style = MaterialTheme.typography.titleMedium)
            }

            if (period == AccountsPeriod.LAST_MONTH && lastMonthSnapshot != null) {
                Divider()
                Text("Last month summary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Opening: ₹${lastMonthSnapshot.openingBalance}  •  Closing: ₹${lastMonthSnapshot.closingBalance}")
                Text("Income: ₹${lastMonthSnapshot.incomeTotal}  •  Expense: ₹${lastMonthSnapshot.expenseTotal}")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onArchive) { Text("Archive") }
            }
        }
    }
}
