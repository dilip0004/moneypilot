package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.yourname.moneypilot.ui.features.accounts.getAccountIcon
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol
import kotlinx.coroutines.flow.collectLatest

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

    // Archive confirmation dialog
    if (showArchiveDialog != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = null },
            title = { Text("Archive Wallet") },
            text = { Text("Are you sure you want to archive '${showArchiveDialog!!.name}'? Archived wallets are hidden from the main list.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.archiveAccount(showArchiveDialog!!)
                    showArchiveDialog = null
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        val wallet = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Wallet?") },
            text = {
                Column {
                    Text("Permanently delete '${wallet.name}'?")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Wallets with history must be archived instead.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWallet(wallet)
                    showDeleteDialog = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScreenState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is ScreenState.Empty -> AccountsEmptyState()
            is ScreenState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error loading accounts")
            }
            is ScreenState.Success -> {
                val data = state.data
                val filteredAccounts = remember(data.accounts, searchQuery) {
                    data.accounts.filter { !it.isArchived && (it.name.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true)) }
                }

                if (filteredAccounts.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results matching '$searchQuery'", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (filteredAccounts.isEmpty()) {
                    AccountsEmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("accounts_list"),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp) // Tightened
                    ) {
                        items(
                            items = filteredAccounts,
                            key = { it.id }
                        ) { acc ->
                            CompactAccountCard(
                                account = acc,
                                isPrivacyMode = isPrivacyMode,
                                hasMismatch = data.mismatchedWalletIds.contains(acc.id),
                                onClick = { onAccountClick(acc.id) },
                                onArchive = { showArchiveDialog = acc },
                                onDelete = { showDeleteDialog = acc },
                                onEdit = { onEditAccount(acc.id) },
                                currencySymbol = currencySymbol,
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAccountCard(
    account: WalletEntity,
    isPrivacyMode: Boolean,
    hasMismatch: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val balanceColor = when {
        account.currentBalance < 0 -> MaterialTheme.colorScheme.error
        account.currentBalance == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> Color(0xFF00C853)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), // Slightly sharper corners for premium look
        colors = CardDefaults.cardColors(
            containerColor = if (hasMismatch) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
                else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Main Row: [Icon] [Name + TypeBadge] [Balance]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getAccountIcon(account.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(10.dp))
                
                // Name and Type Badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasMismatch) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.GppBad, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                        }
                    }
                    // Type Badge - Compact
                    Text(
                        text = account.type,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }

                // Balance (Largest as requested)
                val balance = if(isPrivacyMode) "••••" else account.currentBalance.formatCurrency(currencySymbol)
                Text(
                    text = balance,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = balanceColor,
                    fontSize = 18.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            
            // Tight Actions Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionIconSmall(icon = Icons.Default.Edit, onClick = onEdit)
                    Spacer(Modifier.width(16.dp))
                    ActionIconSmall(icon = Icons.Default.Archive, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), onClick = onArchive)
                }
                
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { 
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold) 
                }
            }
        }
    }
}

@Composable
private fun ActionIconSmall(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
    }
}

@Composable
fun AccountsEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "No accounts yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Add your bank or cash accounts to get started.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
