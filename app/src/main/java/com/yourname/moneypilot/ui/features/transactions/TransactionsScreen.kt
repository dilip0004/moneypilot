package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.features.dashboard.TransactionItem
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    showSearchBar: Boolean = true,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (showSearchBar) { // Only show FAB in the main Records hub
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (showSearchBar) {
                val state = (uiState as? ScreenState.Success)?.data
                OutlinedTextField(
                    value = state?.searchQuery ?: "",
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val currentUiState = uiState) {
                    is ScreenState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ScreenState.Success -> {
                        TransactionHistoryContent(
                            state = currentUiState.data,
                            onEdit = onEditTransaction,
                            onDuplicate = { viewModel.duplicateTransaction(it) },
                            onDelete = { viewModel.deleteTransaction(it) }
                        )
                    }
                    is ScreenState.Error -> {
                        Text(text = "Error: ${currentUiState.message}", color = MaterialTheme.colorScheme.error)
                    }
                    is ScreenState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No transactions found")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryContent(
    state: TransactionsState,
    onEdit: (Long) -> Unit,
    onDuplicate: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        state.groupedTransactions.forEach { grouped ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = grouped.date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ₹${grouped.dailyTotal}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (grouped.dailyTotal >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
            items(grouped.transactions) { transaction ->
                TransactionListItemWithMenu(
                    transaction = transaction,
                    onEdit = { onEdit(transaction.transaction.id) },
                    onDuplicate = { onDuplicate(transaction.transaction) },
                    onDelete = { onDelete(transaction.transaction) }
                )
            }
        }
    }
}

@Composable
fun TransactionListItemWithMenu(
    transaction: TransactionWithCategory,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true },
                        onTap = { onEdit() }
                    )
                }
        ) {
            TransactionItem(transaction.transaction)
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { 
                    showMenu = false
                    onEdit() 
                }
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                onClick = { 
                    showMenu = false
                    onDuplicate() 
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { 
                    showMenu = false
                    onDelete() 
                }
            )
        }
    }
}
