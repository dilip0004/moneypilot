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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.ui.common.CompactTransactionItem
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.util.rememberCurrencySymbol
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    showSearchBar: Boolean = true,
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var walletExpanded by remember { mutableStateOf(false) }
    val currencySymbol = rememberCurrencySymbol()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is TransactionsViewModel.UiEvent.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showSearchBar) {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    modifier = Modifier
                        .testTag("transactions_fab_add")
                        .navigationBarsPadding()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .testTag("transactions_daily_root")
        ) {
            if (showSearchBar) {
                val state = (uiState as? ScreenState.Success)?.data

                ExposedDropdownMenuBox(
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = !walletExpanded }
                ) {
                    val selectedWalletName = state?.wallets?.find { it.id == state.selectedWalletId }?.name ?: "All Wallets"
                    OutlinedTextField(
                        value = selectedWalletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Wallets") },
                            onClick = {
                                viewModel.onWalletSelected(-1L)
                                walletExpanded = false
                            }
                        )
                        state?.wallets?.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet.name) },
                                onClick = {
                                    viewModel.onWalletSelected(wallet.id)
                                    walletExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state?.searchQuery ?: "",
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                            onDelete = { viewModel.deleteTransaction(it) },
                            currencySymbol = currencySymbol
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
    onEdit: (String) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    currencySymbol: String
) {
    val financeColors = LocalFinanceColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("tx_daily_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        state.groupedTransactions.forEach { grouped ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = grouped.dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currencySymbol${grouped.dailyTotal}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (grouped.dailyTotal >= 0) financeColors.income else financeColors.expense
                    )
                }
            }
            items(grouped.transactions) { transactionWithDetails ->
                TransactionListItemWithMenu(
                    transactionWithDetails = transactionWithDetails,
                    onEdit = { onEdit(transactionWithDetails.transaction.id) },
                    onDelete = { onDelete(transactionWithDetails.transaction) }
                )
            }
        }
    }
}

@Composable
fun TransactionListItemWithMenu(
    transactionWithDetails: TransactionWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tx_item_${transactionWithDetails.transaction.id}")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true },
                        onTap = { onEdit() }
                    )
                }
        ) {
            CompactTransactionItem(transactionWithDetails)
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
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}