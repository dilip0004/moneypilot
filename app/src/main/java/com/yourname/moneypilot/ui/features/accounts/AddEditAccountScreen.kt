package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditAccountViewModel.UiEvent.SaveAccount -> onPopBackStack()
                is AddEditAccountViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Account") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(AddEditAccountEvent.SaveAccount) }) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredName(it)) },
                label = { Text("Account Name (e.g. HDFC Bank)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.initialBalance,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredBalance(it)) },
                label = { Text("Initial Balance") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            OutlinedTextField(
                value = state.minBalance,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredMinBalance(it)) },
                label = { Text("Minimum Balance (Buffer)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Set as Primary Wallet")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isPrimary,
                    onCheckedChange = { viewModel.onEvent(AddEditAccountEvent.TogglePrimary) }
                )
            }

            Text("Account Type", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("BANK", "CASH", "CREDIT", "UPI").forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onEvent(AddEditAccountEvent.TypeChanged(type)) },
                        label = { Text(type) }
                    )
                }
            }

            // CREDIT CARD SPECIFIC FIELDS (TASK-28)
            AnimatedVisibility(visible = state.type == "CREDIT") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = state.creditLimit,
                        onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredCreditLimit(it)) },
                        label = { Text("Credit Limit") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("₹ ") }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.billingStartDay,
                            onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredBillingStartDay(it)) },
                            label = { Text("Billing Day (1-31)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = state.dueDate,
                            onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredDueDate(it)) },
                            label = { Text("Due Day (1-31)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
    }
}
