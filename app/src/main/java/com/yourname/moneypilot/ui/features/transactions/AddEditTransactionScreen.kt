package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onPopBackStack: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var showCalculator by remember { mutableStateOf(false) }

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
                },
                actions = {
                    IconButton(onClick = onNavigateToTransfer) {
                        Icon(imageVector = Icons.Default.CompareArrows, contentDescription = "Transfer")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showCalculator) {
                FloatingActionButton(onClick = {
                    viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
                }) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { 
                            if (it.isFocused) {
                                showCalculator = true
                                focusManager.clearFocus() // Hide system keyboard
                            }
                        },
                    readOnly = true, // Force use of calculator
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

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

            if (showCalculator) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    CalculatorKeyboard(
                        initialValue = state.amount,
                        onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                        onDone = { showCalculator = false }
                    )
                }
            }
        }
    }
}
