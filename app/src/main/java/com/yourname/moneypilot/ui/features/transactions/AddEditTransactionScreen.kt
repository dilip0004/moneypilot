package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
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
fun AddEditTransactionScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditTransactionViewModel.UiEvent.SaveTransaction -> {
                    onPopBackStack()
                }
                is AddEditTransactionViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
            }) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
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
                value = state.description,
                onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredDescription(it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Simplification: In a real app, use a Dropdown for Account/Category
            Text("Type: ${state.type}", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == "EXPENSE",
                    onClick = { viewModel.onEvent(AddEditTransactionEvent.TypeChanged("EXPENSE")) },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = state.type == "INCOME",
                    onClick = { viewModel.onEvent(AddEditTransactionEvent.TypeChanged("INCOME")) },
                    label = { Text("Income") }
                )
            }
            
            Text("Account Selection (First available used)", style = MaterialTheme.typography.bodySmall)
            Text("Category Selection (Optional)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
