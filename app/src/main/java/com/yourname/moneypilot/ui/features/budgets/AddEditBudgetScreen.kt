package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
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

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditBudgetViewModel.UiEvent.SaveBudget -> onPopBackStack()
                is AddEditBudgetViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Set Budget", fontWeight = FontWeight.Bold) },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!showCalculator) {
                        IconButton(onClick = { viewModel.onEvent(AddEditBudgetEvent.SaveBudget) }) {
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
                    .padding(16.dp)
                    .padding(bottom = if (showCalculator) 300.dp else 0.dp)
            ) {
                Text("Budget Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                        Spacer(modifier = Modifier.height(16.dp))
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditBudgetEvent.EnteredAmount(it)) },
                    label = { Text("Monthly Limit") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                focusManager.clearFocus()
                                showCalculator = true
                            }
                        },
                    readOnly = true,
                    prefix = { Text("₹ ") }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text("Alert Threshold: ${state.alertThreshold}%", style = MaterialTheme.typography.labelMedium)
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