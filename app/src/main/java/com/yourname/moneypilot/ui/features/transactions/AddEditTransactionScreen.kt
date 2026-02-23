package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showCalculator by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSubcategoryDropdown by remember { mutableStateOf(false) }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var showLoanDropdown by remember { mutableStateOf(false) }

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
                }) { Text(androidx.compose.ui.res.stringResource(com.yourname.moneypilot.R.string.next)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(androidx.compose.ui.res.stringResource(com.yourname.moneypilot.R.string.cancel)) } }
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
                }) { Text(androidx.compose.ui.res.stringResource(com.yourname.moneypilot.R.string.ok)) }
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
                        clipboardManager.getText()?.text?.let { viewModel.onEvent(AddEditTransactionEvent.PasteSms(it)) }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Magic Paste",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(AddEditTransactionEvent.SaveAndAddAnother) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Save & Add Another")
                    }
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
                if (!state.isTruthReviewed) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data parsed from SMS. Review details.", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { viewModel.onEvent(AddEditTransactionEvent.AcceptTruth) }) {
                                Text(androidx.compose.ui.res.stringResource(com.yourname.moneypilot.R.string.accept))
                            }
                        }
                    }
                }

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
                            Text(androidx.compose.ui.res.stringResource(com.yourname.moneypilot.R.string.transaction_date_time), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = state.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy - HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showAccountDropdown,
                    onExpandedChange = {
                        showAccountDropdown = !showAccountDropdown
                        if (showAccountDropdown) {
                            focusManager.clearFocus(); showCalculator = false
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = state.accounts.find { acc -> acc.id == state.accountId }?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet / Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("add_tx_account")
                    )
                    ExposedDropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        state.accounts.forEach { account ->
                            DropdownMenuItem(
                                modifier = Modifier.testTag("dropdown_item"),
                                text = { Text(account.name) },
                                onClick = {
                                    viewModel.onEvent(AddEditTransactionEvent.AccountChanged(account.id))
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }
                }

                if (state.type == "LOAN_REPAYMENT") {
                    ExposedDropdownMenuBox(
                        expanded = showLoanDropdown,
                        onExpandedChange = {
                            showLoanDropdown = !showLoanDropdown
                            if (showLoanDropdown) { focusManager.clearFocus(); showCalculator = false }
                        }
                    ) {
                        OutlinedTextField(
                            value = state.loans.find { l -> l.id == state.loanId }?.name ?: "Select Loan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loan to Repay") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLoanDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = showLoanDropdown, onDismissRequest = { showLoanDropdown = false }) {
                            state.loans.forEach { loan ->
                                DropdownMenuItem(
                                    modifier = Modifier.testTag("dropdown_item"),
                                    text = { Text(loan.name) },
                                    onClick = {
                                        viewModel.onEvent(AddEditTransactionEvent.LoanChanged(loan.id))
                                        showLoanDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else if (state.type == "GOAL_CONTRIBUTION") {
                    ExposedDropdownMenuBox(
                        expanded = showGoalDropdown,
                        onExpandedChange = {
                            showGoalDropdown = !showGoalDropdown
                            if (showGoalDropdown) { focusManager.clearFocus(); showCalculator = false }
                        }
                    ) {
                        OutlinedTextField(
                            value = state.goals.find { g -> g.id == state.goalId }?.name ?: "Select Goal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Savings Goal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGoalDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = showGoalDropdown, onDismissRequest = { showGoalDropdown = false }) {
                            state.goals.forEach { goal ->
                                DropdownMenuItem(
                                    modifier = Modifier.testTag("dropdown_item"),
                                    text = { Text(goal.name) },
                                    onClick = {
                                        viewModel.onEvent(AddEditTransactionEvent.GoalChanged(goal.id))
                                        showGoalDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else {
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
                                        modifier = Modifier.testTag("dropdown_item"),
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
                                            modifier = Modifier.testTag("dropdown_item"),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_description")
                        .onFocusChanged { if (it.isFocused) showCalculator = false },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    })
                )

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                    label = { Text("Amount") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_amount")
                        .onFocusChanged {
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
                        selected = state.type == "EXPENSE",
                        onClick = { showCalculator = false; viewModel.onEvent(AddEditTransactionEvent.TypeChanged("EXPENSE")) },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = state.type == "INCOME",
                        onClick = { showCalculator = false; viewModel.onEvent(AddEditTransactionEvent.TypeChanged("INCOME")) },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = state.type == "GOAL_CONTRIBUTION",
                        onClick = { showCalculator = false; viewModel.onEvent(AddEditTransactionEvent.TypeChanged("GOAL_CONTRIBUTION")) },
                        label = { Text("Goal") }
                    )
                    FilterChip(
                        selected = state.type == "LOAN_REPAYMENT",
                        onClick = { showCalculator = false; viewModel.onEvent(AddEditTransactionEvent.TypeChanged("LOAN_REPAYMENT")) },
                        label = { Text("Repay") }
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
