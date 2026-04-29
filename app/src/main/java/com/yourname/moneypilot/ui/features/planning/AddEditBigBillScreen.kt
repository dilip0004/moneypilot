package com.yourname.moneypilot.ui.features.planning

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.BillRecurrence
import com.yourname.moneypilot.ui.components.AppDatePickerField
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBigBillScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditBigBillViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditBigBillViewModel.UiEvent.SaveBigBill -> onPopBackStack()
                is AddEditBigBillViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Log Large Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(AddEditBigBillEvent.SaveBigBill) },
                        modifier = Modifier.testTag("big_bill_save_button")
                    ) {
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
                onValueChange = { viewModel.onEvent(AddEditBigBillEvent.EnteredName(it)) },
                label = { Text("Expense Name (e.g. Annual Insurance)") },
                modifier = Modifier.fillMaxWidth().testTag("big_bill_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(AddEditBigBillEvent.EnteredAmount(it)) },
                label = { Text("Estimated Amount") },
                modifier = Modifier.fillMaxWidth().testTag("big_bill_amount_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") },
                singleLine = true
            )

            AppDatePickerField(
                label = "Due Date",
                value = state.dueDate,
                onChange = { viewModel.onEvent(AddEditBigBillEvent.DateChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("big_bill_date_picker")
            )

            // Recurrence Picker
            var expandedRecurrence by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedRecurrence,
                onExpandedChange = { expandedRecurrence = it }
            ) {
                OutlinedTextField(
                    value = state.recurrenceType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Recurrence") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecurrence) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("big_bill_recurrence_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = expandedRecurrence,
                    onDismissRequest = { expandedRecurrence = false }
                ) {
                    BillRecurrence.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                viewModel.onEvent(AddEditBigBillEvent.RecurrenceChanged(type))
                                expandedRecurrence = false
                            },
                            modifier = Modifier.testTag("big_bill_recurrence_${type.name}")
                        )
                    }
                }
            }

            // Wallet Selector
            var expandedWallet by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWallet,
                onExpandedChange = { expandedWallet = it }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.linkedWalletId }?.name ?: "No Wallet Linked",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Linked Wallet (Source of Funds)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("big_bill_wallet_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = expandedWallet,
                    onDismissRequest = { expandedWallet = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            viewModel.onEvent(AddEditBigBillEvent.WalletChanged(null))
                            expandedWallet = false
                        }
                    )
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                viewModel.onEvent(AddEditBigBillEvent.WalletChanged(wallet.id))
                                expandedWallet = false
                            },
                            modifier = Modifier.testTag("big_bill_wallet_${wallet.id}")
                        )
                    }
                }
            }

            // Category Selector
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = state.categories.find { it.id == state.categoryId }?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("big_bill_category_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.icon} ${category.name}") },
                            onClick = {
                                viewModel.onEvent(AddEditBigBillEvent.CategoryChanged(category.id))
                                expandedCategory = false
                            },
                            modifier = Modifier.testTag("big_bill_category_${category.id}")
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Proactive Planning", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = state.autoReserveFlag,
                    onCheckedChange = { viewModel.onEvent(AddEditBigBillEvent.AutoReserveChanged(it)) },
                    modifier = Modifier.testTag("big_bill_auto_reserve_checkbox")
                )
                Column {
                    Text("Auto-Reserve Goal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Calculate monthly target to meet this bill.", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = state.reminderDaysBefore,
                onValueChange = { viewModel.onEvent(AddEditBigBillEvent.ReminderDaysChanged(it)) },
                label = { Text("Reminder (Days Before)") },
                modifier = Modifier.fillMaxWidth().testTag("big_bill_reminder_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("e.g. 3") }
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onEvent(AddEditBigBillEvent.EnteredNotes(it)) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth().testTag("big_bill_notes_input"),
                minLines = 3
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = state.isPaid,
                    onCheckedChange = { viewModel.onEvent(AddEditBigBillEvent.StatusChanged(it)) },
                    modifier = Modifier.testTag("big_bill_paid_checkbox")
                )
                Text("Mark as Paid", style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                "Big Bills help you plan for irregular but large expenses. Setting a reminder ensures you have funds ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
