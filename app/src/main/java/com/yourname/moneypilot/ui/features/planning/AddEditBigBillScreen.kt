package com.yourname.moneypilot.ui.features.planning

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.BillRecurrence
import com.yourname.moneypilot.ui.components.AppDatePickerField
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlannedExpenseScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditBigBillViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditBigBillViewModel.UiEvent.SaveBill -> onPopBackStack()
                is AddEditBigBillViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == 0L) "Add Planned Expense" else "Edit Planned Expense", fontWeight = FontWeight.Bold) },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(AddEditBigBillEvent.SaveBill) }) {
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
                label = { Text("Expense Name (e.g. Life Insurance)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Recurrence Row
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
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedRecurrence, onDismissRequest = { expandedRecurrence = false }) {
                    BillRecurrence.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ")) },
                            onClick = {
                                viewModel.onEvent(AddEditBigBillEvent.RecurrenceChanged(type))
                                expandedRecurrence = false
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditBigBillEvent.EnteredAmount(it)) },
                    label = { Text("Target Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                AppDatePickerField(
                    label = "Due Date",
                    value = state.dueDate,
                    onChange = { viewModel.onEvent(AddEditBigBillEvent.DueDateChanged(it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Reserve Account
            var expandedWallet by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedWallet,
                onExpandedChange = { expandedWallet = it }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.reserveWalletId }?.name ?: "Default Reserve Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reserve In Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedWallet, onDismissRequest = { expandedWallet = false }) {
                    DropdownMenuItem(text = { Text("Use Global Default") }, onClick = {
                        viewModel.onEvent(AddEditBigBillEvent.ReserveWalletChanged(null))
                        expandedWallet = false
                    })
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(text = { Text(wallet.name) }, onClick = {
                            viewModel.onEvent(AddEditBigBillEvent.ReserveWalletChanged(wallet.id))
                            expandedWallet = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onEvent(AddEditBigBillEvent.EnteredNotes(it)) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
