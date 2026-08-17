package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.motionTween
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
import androidx.compose.ui.graphics.Brush
import java.time.YearMonth
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    currentMonth: YearMonth = YearMonth.now(),
    showSearchBar: Boolean = true,
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    isPrivacyMode: Boolean = false,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var walletExpanded by remember { mutableStateOf(false) }
    val currencySymbol = rememberCurrencySymbol()

    // Sync current month from parent to ViewModel
    LaunchedEffect(currentMonth) {
        viewModel.updateMonth(currentMonth)
    }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showSearchBar) {
                val primary = MaterialTheme.colorScheme.primary
                val gradient = Brush.linearGradient(
                    colors = listOf(primary, primary.copy(alpha = 0.8f))
                )
                
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("transactions_fab_add")
                        .padding(bottom = 12.dp)
                        .background(gradient, CircleShape)
                        .size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
                .testTag("transactions_daily_root")
        ) {
            if (showSearchBar) {
                val state = (uiState as? ScreenState.Success)?.data

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wallet Dropdown - Compact
                    Box(modifier = Modifier.weight(1.2f)) {
                        ExposedDropdownMenuBox(
                            expanded = walletExpanded,
                            onExpandedChange = { walletExpanded = !walletExpanded }
                        ) {
                            val selectedWalletName = state?.wallets?.find { it.id == state.selectedWalletId }?.name ?: "All Wallets"
                            OutlinedTextField(
                                value = selectedWalletName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = walletExpanded,
                                onDismissRequest = { walletExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Wallets") },
                                    onClick = { viewModel.onWalletSelected(-1L); walletExpanded = false }
                                )
                                state?.wallets?.forEach { wallet ->
                                    DropdownMenuItem(
                                        text = { Text(wallet.name) },
                                        onClick = { viewModel.onWalletSelected(wallet.id); walletExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar - Compact
                    OutlinedTextField(
                        value = state?.searchQuery ?: "",
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.weight(2f).height(48.dp),
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
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
                            currencySymbol = currencySymbol,
                            isPrivacyMode = isPrivacyMode
                        )
                    }
                    is ScreenState.Error -> {
                        Text(text = "Error: ${currentUiState.message}", color = MaterialTheme.colorScheme.error)
                    }
                    is ScreenState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No transactions for this period")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionHistoryContent(
    state: TransactionsState,
    onEdit: (String) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    currencySymbol: String,
    isPrivacyMode: Boolean
) {
    val financeColors = LocalFinanceColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tx_daily_list"),
        verticalArrangement = Arrangement.spacedBy(4.dp), // Tighter vertical spacing
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp) // Proper bottom padding
    ) {
        state.groupedTransactions.forEach { grouped ->
            // Spec 8: Root cause fix for glitch - stable keys and remove AnimatedVisibility inside list
            item(key = "header_${grouped.date}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = grouped.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    
                    val amount = grouped.dailyTotal
                    val isNegative = amount < 0
                    val absAmount = Math.abs(amount)
                    val displayTotal = if (isPrivacyMode) "••••" 
                    else "${if(isNegative) "−" else ""}$currencySymbol${if(absAmount % 1.0 == 0.0) absAmount.toInt() else String.format("%.2f", absAmount)}"
                    
                    Text(
                        text = displayTotal,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (amount >= 0) financeColors.income else financeColors.expense
                    )
                }
            }
            
            items(
                items = grouped.transactions,
                key = { it.transaction.id }
            ) { transactionWithDetails ->
                // Spec 8: Removed LaunchedEffect delay and AnimatedVisibility which caused visual jumps during scroll
                TransactionListItemWithMenu(
                    transactionWithDetails = transactionWithDetails,
                    onEdit = { onEdit(transactionWithDetails.transaction.id) },
                    onDelete = { onDelete(transactionWithDetails.transaction) },
                    isPrivacyMode = isPrivacyMode,
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@Composable
fun TransactionListItemWithMenu(
    transactionWithDetails: TransactionWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isPrivacyMode: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)) // Subtle rounding for group items
                .testTag("tx_item_${transactionWithDetails.transaction.id}")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true },
                        onTap = { onEdit() }
                    )
                },
            color = Color.Transparent // Keep it lightweight
        ) {
            CompactTransactionItem(transactionWithDetails, isPrivacyMode = isPrivacyMode)
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