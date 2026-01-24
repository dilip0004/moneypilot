package com.yourname.moneypilot.ui.features.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.AppDatePickerField
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditGoalViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditGoalViewModel.UiEvent.SaveGoal -> onPopBackStack()
                is AddEditGoalViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Add Goal") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(AddEditGoalEvent.SaveGoal) }) {
                Icon(Icons.Default.Save, contentDescription = "Save")
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
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredName(it)) },
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.targetAmount,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredTargetAmount(it)) },
                label = { Text("Target Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            OutlinedTextField(
                value = state.currentAmount,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredCurrentAmount(it)) },
                label = { Text("Initial Saved Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            Text("Priority (1-5): ${state.priority}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = state.priority.toFloat(),
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.PriorityChanged(it.toInt())) },
                valueRange = 1f..5f,
                steps = 3
            )

            AppDatePickerField(
                label = "Target Date",
                value = state.targetDate,
                onChange = { viewModel.onEvent(AddEditGoalEvent.DateChanged(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
