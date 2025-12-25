package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

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
    
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSubcategoryDropdown by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
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
                    IconButton(onClick = {
                        showCalculator = false
                        onNavigateToTransfer()
                    }) {
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
                // Date Selection Row
                Surface(
                    onClick = { 
                        focusManager.clearFocus()
                        showCalculator = false
                        showDatePicker = true 
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Transaction Date", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = state.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy - HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Account Selection
                ExposedDropdownMenuBox(
                    expanded = showAccountDropdown,
                    onExpandedChange = { 
                        showAccountDropdown = !showAccountDropdown 
                        if (showAccountDropdown) {
                            focusManager.clearFocus()
                            showCalculator = false
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = state.accounts.find { it.id == state.accountId }?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet / Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        state.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.onEvent(AddEditTransactionEvent.AccountChanged(account.id))
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }
                }

                // Category Selection
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { 
                        showCategoryDropdown = !showCategoryDropdown 
                        if (showCategoryDropdown) {
                            focusManager.clearFocus()
                            showCalculator = false
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = state.categories.find { it.id == state.categoryId }?.name ?: "Uncategorized",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.icon} ${category.name}") },
                                onClick = {
                                    viewModel.onEvent(AddEditTransactionEvent.CategoryChanged(category.id))
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Subcategory Selection (New)
                if (state.categoryId != null && state.subcategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = showSubcategoryDropdown,
                        onExpandedChange = { 
                            showSubcategoryDropdown = !showSubcategoryDropdown 
                            if (showSubcategoryDropdown) {
                                focusManager.clearFocus()
                                showCalculator = false
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = state.subcategories.find { it.id == state.subcategoryId }?.name ?: "Select Subcategory",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subcategory") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubcategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showSubcategoryDropdown,
                            onDismissRequest = { showSubcategoryDropdown = false }
                        ) {
                            state.subcategories.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = {
                                        viewModel.onEvent(AddEditTransactionEvent.SubcategoryChanged(sub.id))
                                        showSubcategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredDescription(it)) },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { 
                            if (it.isFocused) {
                                showCalculator = false
                            }
                        }
                )

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { 
                            if (it.isFocused) {
                                focusManager.clearFocus()
                                showCalculator = true
                            }
                        },
                    readOnly = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text("Type: ${state.type}", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.type == "EXPENSE",
                        onClick = { 
                            showCalculator = false
                            viewModel.onEvent(AddEditTransactionEvent.TypeChanged("EXPENSE")) 
                        },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = state.type == "INCOME",
                        onClick = { 
                            showCalculator = false
                            viewModel.onEvent(AddEditTransactionEvent.TypeChanged("INCOME")) 
                        },
                        label = { Text("Income") }
                    )
                }
            }

            if (showCalculator) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CalculatorKeyboard(
                        initialValue = state.amount,
                        onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                        onDone = { 
                            showCalculator = false 
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}
