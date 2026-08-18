package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.AppDatePickerField
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

    val assetTypes = listOf("STOCKS", "CRYPTO", "GOLD", "FD", "RD", "PPF", "SIP", "REAL_ESTATE")

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
                label = { Text("Asset Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var expandedType by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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

                AppDatePickerField(
                    label = "Start Date",
                    value = state.startDate,
                    onChange = { viewModel.onEvent(AddEditInvestmentEvent.StartDateChanged(it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Dynamic Fields Section
            when (state.type) {
                "STOCKS", "CRYPTO" -> {
                    StockCryptoFields(state, viewModel)
                }
                "GOLD" -> {
                    GoldFields(state, viewModel)
                }
                "FD" -> {
                    FixedDepositFields(state, viewModel)
                }
                "RD" -> {
                    RecurringDepositFields(state, viewModel)
                }
                "PPF" -> {
                    PpfFields(state, viewModel)
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
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCryptoFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.symbol,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredSymbol(it)) },
            label = { Text(if(state.type == "STOCKS") "Ticker / Symbol" else "Crypto Name / Code") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredQuantity(it)) },
                label = { Text("Quantity") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.averagePrice,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredAvgPrice(it)) },
                label = { Text("Avg Buy Price") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredQuantity(it)) },
                label = { Text("Weight (grams)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.averagePrice,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredAvgPrice(it)) },
                label = { Text("Purchase Price /g") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedDepositFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.principal,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredPrincipal(it)) },
            label = { Text("Principal Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") },
            shape = RoundedCornerShape(12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.interestRate,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredInterestRate(it)) },
                label = { Text("Interest Rate (%)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.tenureMonths,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredTenureMonths(it)) },
                label = { Text("Tenure (Months)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDepositFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.monthlyInstallment,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredMonthlyInstallment(it)) },
            label = { Text("Monthly Installment") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") },
            shape = RoundedCornerShape(12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.interestRate,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredInterestRate(it)) },
                label = { Text("Rate of Interest (%)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.tenureMonths,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredTenureMonths(it)) },
                label = { Text("Tenure (Months)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpfFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.currentBalance,
            onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredCurrentBalance(it)) },
            label = { Text("Current Balance") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("₹") },
            shape = RoundedCornerShape(12.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.annualContribution,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredAnnualContribution(it)) },
                label = { Text("Annual Contribution") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") },
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.interestRate,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredInterestRate(it)) },
                label = { Text("Interest Rate (%)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.sipAmount,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredSipAmount(it)) },
                label = { Text("SIP Amount") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") },
                shape = RoundedCornerShape(12.dp)
            )
            var expandedFreq by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedFreq,
                onExpandedChange = { expandedFreq = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = state.frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFreq) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                    listOf("MONTHLY", "QUARTERLY", "YEARLY").forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                viewModel.onEvent(AddEditInvestmentEvent.FrequencyChanged(freq))
                                expandedFreq = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealEstateFields(state: AddEditInvestmentState, viewModel: AddEditInvestmentViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.purchaseValue,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredPurchaseValue(it)) },
                label = { Text("Purchase Price") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") },
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state.currentValuation,
                onValueChange = { viewModel.onEvent(AddEditInvestmentEvent.EnteredCurrentValuation(it)) },
                label = { Text("Current Valuation") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹") },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
