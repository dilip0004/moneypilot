package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.util.rememberCurrencySymbol
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onPopBackStack: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onSaveClickOverride: (() -> Unit)? = null,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val financeColors = LocalFinanceColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val currencySymbol = rememberCurrencySymbol()

    var showCalculator by remember { mutableStateOf(false) }
    var showWalletDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSubcategoryDropdown by remember { mutableStateOf(false) }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var showInvestmentDropdown by remember { mutableStateOf(false) }
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
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
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
                title = { Text(text = if(state.isEditing) "Edit Record" else "New Record") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isEditing) {
                        IconButton(onClick = {
                            showCalculator = false
                            onNavigateToTransfer()
                        }) {
                            Icon(imageVector = Icons.Default.CompareArrows, contentDescription = "Transfer")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showCalculator) {
                FloatingActionButton(
                    onClick = { 
                        if (onSaveClickOverride != null) {
                            onSaveClickOverride()
                        } else {
                            viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
                        }
                    },
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
                // SMS Truth Review Step
                if (state.reviewMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Parsed from SMS. Please review and accept.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.onEvent(AddEditTransactionEvent.AcceptSmsReview) }) {
                                Text("Accept")
                            }
                        }
                    }
                }

                // Explicit Date Acceptance
                val dateHighlightColor = if (!state.isDateConfirmed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    color = dateHighlightColor,
                    border = if (!state.isDateConfirmed) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth, 
                            contentDescription = null, 
                            tint = if (!state.isDateConfirmed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Date & Time ${if(!state.isDateConfirmed) "(Action Required)" else ""}", 
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!state.isDateConfirmed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy - HH:mm")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!state.isDateConfirmed) {
                            Button(
                                onClick = { viewModel.onEvent(AddEditTransactionEvent.ConfirmDate) },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Confirm", fontSize = 12.sp)
                            }
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
                    val walletName = state.wallets.find { it.id == state.walletFromId }?.name ?: "Select Wallet"
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account / Wallet") },
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
                            val categoryName = state.categories.find { cat -> cat.id == state.categoryId }?.let { "${it.icon} ${it.name}" } ?: "Select Category"
                            OutlinedTextField(
                                value = categoryName,
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
                                val subName = state.subcategories.find { sub -> sub.id == state.subcategoryId }?.name ?: "Select Subcategory"
                                OutlinedTextField(
                                    value = subName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Subcategory (Optional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubcategoryDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("add_tx_subcategory")
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

                        // ADVANCED POSITION LINKING (Loans, Interest, Refunds)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Advanced Linking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                
                                // Loan Link
                                ExposedDropdownMenuBox(
                                    expanded = showLoanDropdown,
                                    onExpandedChange = {
                                        showLoanDropdown = !showLoanDropdown
                                        if (showLoanDropdown) { focusManager.clearFocus(); showCalculator = false }
                                    }
                                ) {
                                    val loanName = state.loans.find { it.id == state.loanId }?.name ?: "Not Linked to Loan"
                                    OutlinedTextField(
                                        value = loanName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Liability / Loan") },
                                        leadingIcon = { Icon(Icons.Default.CreditScore, null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLoanDropdown) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                    )
                                    ExposedDropdownMenu(expanded = showLoanDropdown, onDismissRequest = { showLoanDropdown = false }) {
                                        DropdownMenuItem(text = { Text("None") }, onClick = {
                                            viewModel.onEvent(AddEditTransactionEvent.LoanChanged(null))
                                            showLoanDropdown = false
                                        })
                                        state.loans.forEach { loan ->
                                            DropdownMenuItem(text = { Text(loan.name) }, onClick = {
                                                viewModel.onEvent(AddEditTransactionEvent.LoanChanged(loan.id))
                                                showLoanDropdown = false
                                            })
                                        }
                                    }
                                }

                                // Toggles row
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Section 5.3: Interest Posting
                                    FilterChip(
                                        selected = state.isInterestPosting,
                                        onClick = { viewModel.onEvent(AddEditTransactionEvent.ToggleInterestPosting(!state.isInterestPosting)) },
                                        label = { Text("Interest Posting", fontSize = 11.sp) },
                                        leadingIcon = if (state.isInterestPosting) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Section 5.3: Refund support
                                    FilterChip(
                                        selected = state.isRefund,
                                        onClick = { viewModel.onEvent(AddEditTransactionEvent.ToggleRefund(!state.isRefund)) },
                                        label = { Text("Mark as Refund", fontSize = 11.sp) },
                                        leadingIcon = if (state.isRefund) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Planning Links
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Goal Link
                            Surface(
                                onClick = { showGoalDropdown = true },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                val goal = state.goals.find { it.id == state.goalId }
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Flag, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(goal?.name ?: "Link Goal", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                            
                            // Investment Link
                            Surface(
                                onClick = { showInvestmentDropdown = true },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                val inv = state.investments.find { it.id == state.investmentId }
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(inv?.name ?: "Link Asset", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }

                        // Section 3.2 Compliance: Transaction Tags UI
                        if (state.availableTags.isNotEmpty()) {
                            Text("Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.availableTags.forEach { tag ->
                                    val isSelected = state.selectedTagIds.contains(tag.id)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onEvent(AddEditTransactionEvent.ToggleTag(tag.id)) },
                                        label = { Text(tag.name, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(tag.color).copy(alpha = 0.3f),
                                            selectedLabelColor = Color(tag.color)
                                        ),
                                        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                                    )
                                }
                            }
                        }
                    }
                }

                // Select Goal Dialog
                if (showGoalDropdown) {
                    AlertDialog(
                        onDismissRequest = { showGoalDropdown = false },
                        title = { Text("Select Goal") },
                        text = {
                            Column {
                                state.goals.forEach { goal ->
                                    ListItem(
                                        headlineContent = { Text(goal.name) },
                                        leadingContent = { Text(goal.icon) },
                                        modifier = Modifier.clickable { 
                                            viewModel.onEvent(AddEditTransactionEvent.GoalChanged(goal.id))
                                            showGoalDropdown = false
                                        }
                                    )
                                }
                                ListItem(headlineContent = { Text("None") }, modifier = Modifier.clickable { 
                                    viewModel.onEvent(AddEditTransactionEvent.GoalChanged(null))
                                    showGoalDropdown = false
                                })
                            }
                        },
                        confirmButton = {}
                    )
                }

                // Select Asset Dialog
                if (showInvestmentDropdown) {
                    AlertDialog(
                        onDismissRequest = { showInvestmentDropdown = false },
                        title = { Text("Select Asset") },
                        text = {
                            Column {
                                state.investments.forEach { inv ->
                                    ListItem(
                                        headlineContent = { Text(inv.name) },
                                        modifier = Modifier.clickable { 
                                            viewModel.onEvent(AddEditTransactionEvent.InvestmentChanged(inv.id))
                                            showInvestmentDropdown = false
                                        }
                                    )
                                }
                                ListItem(headlineContent = { Text("None") }, modifier = Modifier.clickable { 
                                    viewModel.onEvent(AddEditTransactionEvent.InvestmentChanged(null))
                                    showInvestmentDropdown = false
                                })
                            }
                        },
                        confirmButton = {}
                    )
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredDescription(it)) },
                    label = { Text("Description / Note") },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showCalculator = false },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth().testTag("add_tx_amount"),
                        readOnly = true,
                        prefix = { Text("$currencySymbol ") }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                focusManager.clearFocus()
                                showCalculator = true
                                coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.type == TransactionType.Expense,
                        onClick = { viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Expense)) },
                        label = { Text("Expense") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = financeColors.expense.copy(alpha = 0.2f),
                            selectedLabelColor = financeColors.expense
                        )
                    )
                    FilterChip(
                        selected = state.type == TransactionType.Income,
                        onClick = { viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Income)) },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = financeColors.income.copy(alpha = 0.2f),
                            selectedLabelColor = financeColors.income
                        )
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
