package com.yourname.moneypilot.ui.features.goals

import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditGoalViewModel.UiEvent.SaveGoal -> onPopBackStack()
                is AddEditGoalViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Choose Goal Icon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // We'll wrap the EmojiPicker in a taller scrollable container or just use its LazyVerticalGrid
                // EmojiPicker is already a LazyVerticalGrid, but with userScrollEnabled = false.
                // We'll modify it to allow scroll here.
                
                Box(modifier = Modifier.height(300.dp)) {
                    EmojiPicker(
                        selectedEmoji = state.icon,
                        onEmojiSelected = {
                            viewModel.onEvent(AddEditGoalEvent.IconChanged(it))
                            showEmojiPicker = false
                        },
                        modifier = Modifier.fillMaxSize(),
                        isScrollEnabled = true
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = if (state.isEditMode) "Edit Goal" else "Add Goal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(AddEditGoalEvent.SaveGoal) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("goal_save_fab")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Picker - Compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showEmojiPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp).testTag("goal_emoji_picker")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(state.icon, fontSize = 32.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Goal Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(
                        onClick = { showEmojiPicker = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Choose Icon")
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredName(it)) },
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                )
            )

            OutlinedTextField(
                value = state.targetAmount,
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredTargetAmount(it)) },
                label = { Text("Target Amount") },
                modifier = Modifier.fillMaxWidth().testTag("goal_target_amount_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                prefix = { Text("₹ ") },
                shape = RoundedCornerShape(12.dp)
            )

            // Initial contribution logic (Only for new goals)
            if (!state.isEditMode) {
                OutlinedTextField(
                    value = state.currentAmount,
                    onValueChange = { viewModel.onEvent(AddEditGoalEvent.EnteredCurrentAmount(it)) },
                    label = { Text("Initial Contribution (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_current_amount_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    prefix = { Text("₹ ") },
                    shape = RoundedCornerShape(12.dp)
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
                            label = { Text("Deduct From") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWallet) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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
                // In Edit Mode, show progress read-only - compact
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text("₹ ${state.currentAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Priority - Compact
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(state.priority.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = state.priority.toFloat(),
                onValueChange = { viewModel.onEvent(AddEditGoalEvent.PriorityChanged(it.toInt())) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.testTag("goal_priority_slider").height(24.dp)
            )

            AppDatePickerField(
                label = "Target Date",
                value = state.targetDate,
                onChange = { viewModel.onEvent(AddEditGoalEvent.DateChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("goal_date_picker")
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Extra space for FAB and IME
        }
    }
}
