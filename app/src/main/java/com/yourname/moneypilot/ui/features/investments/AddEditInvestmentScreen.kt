package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvestmentScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditInvestmentViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditInvestmentViewModel.UiEvent.SaveInvestment -> onPopBackStack()
                is AddEditInvestmentViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    val assetTypes = listOf("STOCKS", "MUTUAL_FUNDS", "CRYPTO", "GOLD", "REAL_ESTATE", "FD")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Log Investment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AddEditInvestmentEvent.SaveInvestment) }) {
                        Icon(Icons.Default.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredName(it)) },
                label = { Text("Asset Name (e.g. HDFC Index Fund)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.symbol,
                    onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredSymbol(it)) },
                    label = { Text("Symbol / Ticker") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                var expandedType by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = state.type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        assetTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    viewModel.onEvent(AddEditInvestmentEvent.TypeChanged(type))
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
            }

            // Wallet Selector (TASK-INVESTMENT-LINK)
            var expandedWallet by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWallet,
                onExpandedChange = { expandedWallet = it }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.linkedWalletId }?.name ?: "No Wallet Linked",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Buy using Wallet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    placeholder = { Text("Deduct funds from...") }
                )
                ExposedDropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                    DropdownMenuItem(text = { Text("None (Already Paid)") }, onClick = {
                        viewModel.onEvent(AddEditInvestmentEvent.WalletLinked(null))
                        expandedWallet = false
                    })
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(text = { Text(wallet.name) }, onClick = {
                            viewModel.onEvent(AddEditInvestmentEvent.WalletLinked(wallet.id))
                            expandedWallet = false
                        })
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredQuantity(it)) },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.currency,
                    onValueChange = {},
                    label = { Text("Currency") },
                    modifier = Modifier.weight(0.6f),
                    readOnly = true,
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state.averagePrice,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredAvgPrice(it)) },
                label = { Text("Purchase Price (Avg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") },
                singleLine = true
            )

            OutlinedTextField(
                value = state.currentPrice,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredCurrentPrice(it)) },
                label = { Text("Current Market Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") },
                singleLine = true
            )
            
            Text(
                "Info: Linking a wallet will automatically create an Expense transaction in your ledger to maintain balance accuracy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
