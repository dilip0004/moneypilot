package com.yourname.moneypilot.ui.features.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditLoanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditLoanViewModel.UiEvent.SaveLoan -> onPopBackStack()
                is AddEditLoanViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Log Loan/Debt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AddEditLoanEvent.SaveLoan) }) {
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
                onValueChange = { viewModel.onEvent(AddEditLoanEvent.EnteredName(it)) },
                label = { Text("Loan Name (e.g. Home Loan)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.lender,
                onValueChange = { viewModel.onEvent(AddEditLoanEvent.EnteredLender(it)) },
                label = { Text("Lender / Person Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditLoanEvent.EnteredAmount(it)) },
                    label = { Text("Principal Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹ ") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = state.durationMonths,
                    onValueChange = { viewModel.onEvent(AddEditLoanEvent.EnteredDuration(it)) },
                    label = { Text("Tenure (Months)") },
                    modifier = Modifier.weight(0.6f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.interestRate,
                    onValueChange = { viewModel.onEvent(AddEditLoanEvent.EnteredInterest(it)) },
                    label = { Text("Annual Interest %") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("%") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.monthlyPayment,
                    onValueChange = { viewModel.onEvent(AddEditLoanEvent.EnteredMonthlyPayment(it)) },
                    label = { Text("Monthly EMI") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹ ") },
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
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
                        label = { Text("Loan Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        listOf("BORROWED", "LENT").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    viewModel.onEvent(AddEditLoanEvent.TypeChanged(type))
                                    expandedType = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Auto-Deduction Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            var expandedWallet by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWallet,
                onExpandedChange = { expandedWallet = it }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.linkedWalletId }?.name ?: "No Wallet Linked",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deduct EMI from") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    placeholder = { Text("Select Wallet for Auto-Deduction") }
                )
                ExposedDropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                    DropdownMenuItem(
                        text = { Text("None (Manual Repayment)") },
                        onClick = {
                            viewModel.onEvent(AddEditLoanEvent.WalletLinked(null))
                            expandedWallet = false
                        }
                    )
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                viewModel.onEvent(AddEditLoanEvent.WalletLinked(wallet.id))
                                expandedWallet = false
                            }
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "Repayment Day of Month: ${state.repaymentDayOfMonth}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.repaymentDayOfMonth.toFloat(),
                    onValueChange = { viewModel.onEvent(AddEditLoanEvent.RepaymentDayChanged(it.toInt())) },
                    valueRange = 1f..31f,
                    steps = 30
                )
            }

            Text(
                "Info: MoneyPilot will automatically post a split transaction (Principal + Interest) on this day every month if a wallet is linked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
