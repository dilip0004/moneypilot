package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Text("From Account", style = MaterialTheme.typography.titleMedium)
            // Simplified selection logic
            Text("Selected: ${state.accounts.find { it.id == state.fromAccountId }?.name ?: "None"}")
            
            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(32.dp))
            
            Text("To Account", style = MaterialTheme.typography.titleMedium)
            Text("Selected: ${state.accounts.find { it.id == state.toAccountId }?.name ?: "None"}")

            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(TransferEvent.EnteredAmount(it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(TransferEvent.EnteredDescription(it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.onEvent(TransferEvent.PerformTransfer) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transfer Funds")
            }
        }
    }
}
