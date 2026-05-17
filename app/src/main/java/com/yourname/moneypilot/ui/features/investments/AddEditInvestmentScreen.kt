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

    val assetTypes = listOf("STOCKS", "MUTUAL_FUNDS", "CRYPTO", "GOLD", "FD", "RD", "PPF", "SIP", "REAL_ESTATE")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Log ${state.type.replace("_", " ")}", fontWeight = FontWeight.Bold) },
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
            // 1. Basic Info Section
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredName(it)) },
                label = { Text("Asset Name (e.g. HDFC Fixed Deposit)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            var expandedType by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Asset Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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

            // 2. Dynamic Fields Section
            when (state.type) {
                "STOCKS", "MUTUAL_FUNDS", "CRYPTO", "GOLD" -> {
                    MarketLinkedFields(state, viewModel)
                }
                "FD", "PPF" -> {
                    FixedDepositFields(state, viewModel)
                }
                "RD" -> {
                    RecurringDepositFields(state, viewModel)
                }
                "SIP" -> {
                    SipFields(state, viewModel)
                }
                "REAL_ESTATE" -> {
                    RealEstateFields(state, viewModel)
                }
            }

            // 3. Wallet Linking (Source of Funds)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Funding Source", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            var expandedWallet by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWallet,
                onExpandedChange = { expandedWallet = it }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.linkedWalletId }?.name ?: "No Wallet Linked",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deduct from Wallet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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
            
            Text(
                "Linking a wallet creates an automatic expense to track your cash flow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MarketLinkedFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.symbol,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredSymbol(it)) },
            label = { Text("Ticker / Symbol") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredQuantity(it)) },
                label = { Text("Units / Quantity") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = state.averagePrice,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredAvgPrice(it)) },
                label = { Text("Avg Buy Price") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") }
            )
        }
        OutlinedTextField(
            value = state.currentPrice,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredCurrentPrice(it)) },
            label = { Text("Current Market Price") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") }
        )
    }
}

@Composable
fun FixedDepositFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.depositAmount,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredDepositAmount(it)) },
            label = { Text("Principal Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.interestRate,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredInterestRate(it)) },
                label = { Text("Interest Rate (%)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = state.tenure,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredTenure(it)) },
                label = { Text("Tenure (Months)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        OutlinedTextField(
            value = state.maturityDate,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredMaturityDate(it)) },
            label = { Text("Maturity Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Optional") }
        )
    }
}

@Composable
fun RecurringDepositFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.monthlyDeposit,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredMonthlyDeposit(it)) },
            label = { Text("Monthly Installment") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.interestRate,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredInterestRate(it)) },
                label = { Text("Rate of Interest (%)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = state.tenure,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredTenure(it)) },
                label = { Text("Tenure (Months)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
fun SipFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.monthlyAmount,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredMonthlyAmount(it)) },
            label = { Text("SIP Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") }
        )
        OutlinedTextField(
            value = state.fundName,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredFundName(it)) },
            label = { Text("Mutual Fund Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RealEstateFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.purchasePrice,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredPurchasePrice(it)) },
            label = { Text("Purchase Price") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") }
        )
        OutlinedTextField(
            value = state.currentValue,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredCurrentValue(it)) },
            label = { Text("Current Market Valuation") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") }
        )
    }
}
