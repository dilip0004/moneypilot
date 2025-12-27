package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import com.yourname.moneypilot.ui.components.ContentCard
import com.yourname.moneypilot.ui.components.GradientBackground
import com.yourname.moneypilot.ui.components.PrimaryButton
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
    var showCalculator by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditBudgetViewModel.UiEvent.SaveBudget -> onPopBackStack()
                is AddEditBudgetViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    title = { Text("Set Category Budget", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onPopBackStack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ContentCard {
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
                                    if (category.type == "EXPENSE") { // Budgets are typically for expenses
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Amount Input with Calculator
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

                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (!showCalculator) {
                            PrimaryButton(
                                text = "Save Budget",
                                onClick = { viewModel.onEvent(AddEditBudgetEvent.SaveBudget) }
                            )
                        }
                    }
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
}
