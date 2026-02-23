package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onPopBackStack: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }

    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is TransferViewModel.UiEvent.TransferSuccess -> onPopBackStack()
                is TransferViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Fund Transfer") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // From Account Dropdown
            ExposedDropdownMenuBox(
                expanded = fromAccountExpanded,
                onExpandedChange = { fromAccountExpanded = !fromAccountExpanded }
            ) {
                OutlinedTextField(
                    value = state.accounts.find { it.id == state.fromAccountId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("From Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromAccountExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = fromAccountExpanded,
                    onDismissRequest = { fromAccountExpanded = false }
                ) {
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.onEvent(TransferEvent.FromAccountChanged(account.id))
                                fromAccountExpanded = false
                            }
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Transfer Icon",
                modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp)
            )

            // To Account Dropdown
            ExposedDropdownMenuBox(
                expanded = toAccountExpanded,
                onExpandedChange = { toAccountExpanded = !toAccountExpanded }
            ) {
                OutlinedTextField(
                    value = state.accounts.find { it.id == state.toAccountId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = toAccountExpanded,
                    onDismissRequest = { toAccountExpanded = false }
                ) {
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.onEvent(TransferEvent.ToAccountChanged(account.id))
                                toAccountExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(TransferEvent.EnteredAmount(it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(TransferEvent.EnteredDescription(it)) },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.onEvent(TransferEvent.PerformTransfer) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Transfer Funds")
            }
        }
    }
}
