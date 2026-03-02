package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.clickable
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
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.ui.common.ScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit, // Added
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
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
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(data.accounts.filter { !it.isArchived }) { acc ->
                                AccountCard(
                                    account = acc,
                                    onClick = { onAccountClick(acc.id) }, // Added
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
private fun AccountCard(
    account: WalletEntity, // Updated
    onClick: () -> Unit, // Added
    onArchive: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick), // Added
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                    Text(account.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${account.currentBalance}", style = MaterialTheme.typography.titleMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onArchive) { Text("Archive") }
            }
        }
    }
}
