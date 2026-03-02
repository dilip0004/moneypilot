package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onPopBackStack: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showCalculator by remember { mutableStateOf(false) }
    var showWalletDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSubcategoryDropdown by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = state.date.hour,
        initialMinute = state.date.minute
    )

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditTransactionViewModel.UiEvent.SaveTransaction -> onPopBackStack()
                is AddEditTransactionViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate =
                        Instant.ofEpochMilli(datePickerState.selectedDateMillis ?: Instant.now().toEpochMilli())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    viewModel.onEvent(AddEditTransactionEvent.DateChanged(LocalDateTime.of(selectedDate, selectedTime)))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                FloatingActionButton(
                    onClick = { viewModel.onEvent(AddEditTransactionEvent.SaveTransaction) },
                    modifier = Modifier.testTag("add_tx_save")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .padding(bottom = if (showCalculator) 300.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date & Time field
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        showCalculator = false
                        showDatePicker = true
                    },
                    modifier = Modifier
                        .testTag("add_tx_date_time")
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Date & Time", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = state.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy - HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showWalletDropdown,
                    onExpandedChange = {
                        showWalletDropdown = !showWalletDropdown
                        if (showWalletDropdown) {
                            focusManager.clearFocus(); showCalculator = false
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = state.wallets.find { it.id == state.walletFromId }?.name ?: "Select Wallet",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showWalletDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("add_tx_account")
                    )
                    ExposedDropdownMenu(
                        expanded = showWalletDropdown,
                        onDismissRequest = { showWalletDropdown = false }
                    ) {
                        state.wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet.name) },
                                onClick = {
                                    viewModel.onEvent(AddEditTransactionEvent.WalletChanged(wallet.id))
                                    showWalletDropdown = false
                                }
                            )
                        }
                    }
                }

                if (state.type != TransactionType.Transfer) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = showCategoryDropdown,
                            onExpandedChange = {
                                showCategoryDropdown = !showCategoryDropdown
                                if (showCategoryDropdown) { focusManager.clearFocus(); showCalculator = false }
                            }
                        ) {
                            OutlinedTextField(
                                value = state.categories.find { cat -> cat.id == state.categoryId }?.name ?: "Select Category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("add_tx_category"),
                                isError = state.categoryId == null
                            )
                            ExposedDropdownMenu(expanded = showCategoryDropdown, onDismissRequest = { showCategoryDropdown = false }) {
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

                        if (state.subcategories.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = showSubcategoryDropdown,
                                onExpandedChange = {
                                    showSubcategoryDropdown = !showSubcategoryDropdown
                                    if (showSubcategoryDropdown) { focusManager.clearFocus(); showCalculator = false }
                                }
                            ) {
                                OutlinedTextField(
                                    value = state.subcategories.find { sub -> sub.id == state.subcategoryId }?.name ?: "Select Subcategory",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subcategory") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubcategoryDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("add_tx_subcategory"),
                                    isError = state.subcategories.isNotEmpty() && state.subcategoryId == null
                                )
                                ExposedDropdownMenu(expanded = showSubcategoryDropdown, onDismissRequest = { showSubcategoryDropdown = false }) {
                                    state.subcategories.forEach { subItem ->
                                        DropdownMenuItem(
                                            text = { Text(subItem.name) },
                                            onClick = {
                                                viewModel.onEvent(AddEditTransactionEvent.SubcategoryChanged(subItem.id))
                                                showSubcategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredDescription(it)) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showCalculator = false },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        if (it.isFocused) {
                            focusManager.clearFocus()
                            showCalculator = true
                            coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                        }
                    },
                    readOnly = true,
                    prefix = { Text("₹ ") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.type == TransactionType.Expense,
                        onClick = { viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Expense)) },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = state.type == TransactionType.Income,
                        onClick = { viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Income)) },
                        label = { Text("Income") }
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            if (showCalculator) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    CalculatorKeyboard(
                        initialValue = state.amount,
                        onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                        onDone = { showCalculator = false; focusManager.clearFocus() }
                    )
                }
            }
        }
    }
}
