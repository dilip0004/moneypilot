package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditBudgetViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditBudgetViewModel.UiEvent.SaveBudget -> {
                    onPopBackStack()
                }
                is AddEditBudgetViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Budget") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.onEvent(AddEditBudgetEvent.SaveBudget)
            }) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(AddEditBudgetEvent.EnteredAmount(it)) },
                label = { Text("Budget Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rollover Enabled")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = state.rolloverEnabled,
                    onCheckedChange = { viewModel.onEvent(AddEditBudgetEvent.ToggleRollover) }
                )
            }

            Text("Alert Threshold: ${state.alertThreshold}%", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = state.alertThreshold.toFloat(),
                onValueChange = { viewModel.onEvent(AddEditBudgetEvent.AlertThresholdChanged(it.toInt())) },
                valueRange = 50f..100f,
                steps = 9
            )

            Text("Category Selection (First available used)", style = MaterialTheme.typography.bodySmall)
            Text("Period: ${state.period}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
