package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.util.rememberCurrencySymbol
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit,
    onEditAccount: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencySymbol = rememberCurrencySymbol()

    var showArchiveDialog by remember { mutableStateOf<WalletEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<WalletEntity?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AccountsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Archive confirmation dialog
    if (showArchiveDialog != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = null },
            title = { Text("Archive Wallet") },
            text = { Text("Are you sure you want to archive '${showArchiveDialog!!.name}'? Archived wallets are hidden from the main list but can be restored later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveAccount(showArchiveDialog!!)
                        showArchiveDialog = null
                    },
                    modifier = Modifier.testTag("dialog_confirm_archive")
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        val wallet = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Wallet?") },
            text = {
                Column {
                    Text("Are you sure you want to permanently delete '${wallet.name}'?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ Wallets with existing transactions cannot be deleted. Please archive them instead.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("Deletion is only allowed if the wallet has no transaction history.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWallet(wallet)
                        showDeleteDialog = null
                    },
                    modifier = Modifier.testTag("dialog_confirm_delete"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAccount,
                modifier = Modifier.testTag("account_add_fab") // Removed navigationBarsPadding()
            ) {
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
                    Text("No accounts yet", modifier = Modifier.testTag("accounts_empty_state"))
                }
                is ScreenState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Something went wrong")
                }
                is ScreenState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.testTag("accounts_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(data.accounts.filter { !it.isArchived }) { acc ->
                            AccountCard(
                                account = acc,
                                onClick = { onAccountClick(acc.id) },
                                onArchive = { showArchiveDialog = acc },
                                onDelete = { showDeleteDialog = acc },
                                onEdit = { onEditAccount(acc.id) },
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: WalletEntity,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick).testTag("account_card_${account.id}"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(account.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("account_name_${account.id}"))
                    Text(account.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("account_edit_${account.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Wallet")
                    }
                    Text("$currencySymbol${account.currentBalance}", style = MaterialTheme.typography.titleMedium)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onArchive,
                    modifier = Modifier.testTag("account_archive_${account.id}")
                ) { Text("Archive") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("account_delete_${account.id}"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
