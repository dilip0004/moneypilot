package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.MoneyPilotListItem
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    onAccountClick: (Long) -> Unit,
    onEditAccount: (Long) -> Unit,
    searchQuery: String = "",
    viewModel: AccountsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    
    val currencySymbol = rememberCurrencySymbol()

    var showArchiveDialog by remember { mutableStateOf<WalletEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<WalletEntity?>(null) }

    // Dialogs remain unchanged as they are utility
    if (showArchiveDialog != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = null },
            title = { Text("Archive Wallet") },
            text = { Text("Are you sure you want to archive '${showArchiveDialog!!.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveAccount(showArchiveDialog!!)
                    showArchiveDialog = null
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { showArchiveDialog = null }) { Text("Cancel") } }
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Wallet?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWallet(showDeleteDialog!!)
                    showDeleteDialog = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScreenState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is ScreenState.Success -> {
                val data = state.data
                val filteredAccounts = remember(data.accounts, searchQuery) {
                    data.accounts.filter { !it.isArchived && (it.name.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true)) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("accounts_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = filteredAccounts, key = { it.id }) { acc ->
                        val balanceColor = when {
                            acc.currentBalance < 0 -> MaterialTheme.colorScheme.error
                            acc.currentBalance == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else -> Color(0xFF00C853)
                        }
                        
                        MoneyPilotListItem(
                            icon = acc.icon,
                            title = acc.name,
                            subtitle = acc.type,
                            statusColor = balanceColor,
                            onClick = { onAccountClick(acc.id) },
                            trailingContent = {
                                Text(
                                    text = if(isPrivacyMode) "••••" else acc.currentBalance.formatCurrency(currencySymbol),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Black,
                                    color = balanceColor
                                )
                                Row {
                                    IconButton(onClick = { onEditAccount(acc.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.5f))
                                    }
                                    IconButton(onClick = { showArchiveDialog = acc }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Archive, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        )
                    }
                }
            }
            is ScreenState.Empty -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No accounts found") }
            else -> {}
        }
    }
}
