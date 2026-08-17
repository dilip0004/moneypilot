package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    budgetId: Long = -1L,
    month: String? = null, // yyyy-MM-dd
    onPopBackStack: () -> Unit,
    viewModel: AddEditBudgetViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedSubcategory by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    var showRecurrenceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(budgetId, month) {
        if (budgetId != -1L) {
            viewModel.onEvent(AddEditBudgetEvent.LoadBudget(budgetId))
        } else if (month != null) {
            try {
                val date = LocalDate.parse(month)
                viewModel.onEvent(AddEditBudgetEvent.InitialMonthSet(date))
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditBudgetViewModel.UiEvent.SaveBudget -> onPopBackStack()
                is AddEditBudgetViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showRecurrenceDialog) {
        AlertDialog(
            onDismissRequest = { showRecurrenceDialog = false },
            title = { Text("Update recurring budget") },
            text = { Text("Do you want to apply this change to this month only, or update the base recurring budget for all future months?") },
            confirmButton = {
                TextButton(onClick = {
                    val date = if (month != null) LocalDate.parse(month) else LocalDate.now()
                    viewModel.onEvent(AddEditBudgetEvent.SaveOverride(date))
                    showRecurrenceDialog = false
                }) { Text("This month only") }
            },
            dismissButton = {
                Button(onClick = {
                    viewModel.onEvent(AddEditBudgetEvent.SaveBudget)
                    showRecurrenceDialog = false
                }) { Text("Change recurring budget") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (budgetId == -1L) "Set Budget" else "Edit Budget", fontWeight = FontWeight.Bold) },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!showCalculator) {
                        IconButton(onClick = { 
                            if (state.isRecurring && budgetId != -1L && state.parentBudgetId == null) {
                                // Editing a base recurring budget
                                showRecurrenceDialog = true
                            } else {
                                viewModel.onEvent(AddEditBudgetEvent.SaveBudget) 
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = if (showCalculator) 320.dp else 0.dp)
            ) {
                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = {
                        expandedCategory = !expandedCategory
                        if (expandedCategory) {
                            showCalculator = false
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = state.categories.find { it.id == state.categoryId }?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        state.categories.forEach { category ->
                            if (category.type == "EXPENSE") {
                                DropdownMenuItem(
                                    text = { Text("${category.icon} ${category.name}") },
                                    onClick = {
                                        viewModel.onEvent(AddEditBudgetEvent.CategoryChanged(category.id))
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Subcategory Selector
                AnimatedVisibility(visible = state.subcategories.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        ExposedDropdownMenuBox(
                            expanded = expandedSubcategory,
                            onExpandedChange = {
                                expandedSubcategory = !expandedSubcategory
                                if (expandedSubcategory) {
                                    showCalculator = false
                                    focusManager.clearFocus()
                                }
                            }
                        ) {
                            OutlinedTextField(
                                value = state.subcategories.find { it.id == state.subcategoryId }?.name ?: "All Subcategories",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Specific Subcategory") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubcategory) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSubcategory,
                                onDismissRequest = { expandedSubcategory = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (All Subcategories)") },
                                    onClick = {
                                        viewModel.onEvent(AddEditBudgetEvent.SubcategoryChanged(null))
                                        expandedSubcategory = false
                                    }
                                )
                                state.subcategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            viewModel.onEvent(AddEditBudgetEvent.SubcategoryChanged(sub.id))
                                            expandedSubcategory = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditBudgetEvent.EnteredAmount(it)) },
                    label = { Text("Budget Limit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                focusManager.clearFocus()
                                showCalculator = true
                            }
                        },
                    readOnly = true,
                    prefix = { Text("₹ ") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Recurrence Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Monthly Recurring", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Apply to all future months", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.isRecurring,
                        onCheckedChange = { viewModel.onEvent(AddEditBudgetEvent.RecurrenceChanged(it)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Alert Threshold: ${state.alertThreshold}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = state.alertThreshold.toFloat(),
                    onValueChange = { viewModel.onEvent(AddEditBudgetEvent.AlertThresholdChanged(it.toInt())) },
                    valueRange = 50f..100f
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            if (showCalculator) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CalculatorKeyboard(
                        initialValue = state.amount,
                        onValueChange = { viewModel.onEvent(AddEditBudgetEvent.EnteredAmount(it)) },
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