package com.yourname.moneypilot.ui.features.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.AppDatePickerField
import com.yourname.moneypilot.ui.components.EmojiPicker
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditGoalViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
                title = { Text(text = if (state.isEditMode) "Edit Goal" else "Add Goal") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showEmojiPicker) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(AddEditGoalEvent.SaveGoal) },
                    modifier = Modifier.testTag("goal_save_fab")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Picker
            Surface(
                onClick = { showEmojiPicker = !showEmojiPicker },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("goal_emoji_picker")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Goal Icon", style = MaterialTheme.typography.labelMedium)
                    Text(state.icon, fontSize = 28.sp)
                }
            }

            if (showEmojiPicker) {
                EmojiPicker(
                    selectedEmoji = state.icon,
                    onEmojiSelected = {
                        viewModel.onEvent(AddEditGoalEvent.IconChanged(it))
                        showEmojiPicker = false
                    },
                    modifier = Modifier.testTag("goal_emoji_grid")
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredName(it)) },
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
            )

            OutlinedTextField(
                value = state.targetAmount,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredTargetAmount(it)) },
                label = { Text("Target Amount") },
                modifier = Modifier.fillMaxWidth().testTag("goal_target_amount_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            // Initial contribution logic (Only for new goals)
            if (!state.isEditMode) {
                OutlinedTextField(
                    value = state.currentAmount,
                    onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredCurrentAmount(it)) },
                    label = { Text("Initial Saved Amount (Contribution)") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_current_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹ ") }
                )

                if ((state.currentAmount.toDoubleOrNull() ?: 0.0) > 0) {
                    var expandedWallet by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedWallet,
                        onExpandedChange = { expandedWallet = it }
                    ) {
                        OutlinedTextField(
                            value = state.wallets.find { it.id == state.linkedWalletId }?.name ?: "Select Source Wallet",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Deduct Initial Contribution From") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWallet,
                            onDismissRequest = { expandedWallet = false }
                        ) {
                            state.wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text(wallet.name) },
                                    onClick = {
                                        viewModel.onEvent(AddEditGoalEvent.WalletChanged(wallet.id))
                                        expandedWallet = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // In Edit Mode, show progress read-only
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Current Progress", style = MaterialTheme.typography.labelSmall)
                        Text("₹ ${state.currentAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("To add more, create a transaction linked to this goal.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text("Priority (1-5): ${state.priority}", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = state.priority.toFloat(),
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.PriorityChanged(it.toInt())) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.testTag("goal_priority_slider")
            )

            AppDatePickerField(
                label = "Target Date",
                value = state.targetDate,
                onChange = { viewModel.onEvent(AddEditGoalEvent.DateChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("goal_date_picker")
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
