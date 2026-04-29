package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.EmojiPicker
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAccountScreen(
    onPopBackStack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var showEmojiPicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditAccountViewModel.UiEvent.SaveAccount -> onPopBackStack()
                is AddEditAccountViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Account") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(AddEditAccountEvent.SaveAccount) },
                modifier = Modifier.testTag("account_save_fab")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
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
            // #3: Emoji/Icon Selection
            Surface(
                onClick = { showEmojiPicker = !showEmojiPicker },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("account_emoji_picker")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account Icon", style = MaterialTheme.typography.labelMedium)
                    Text(state.icon, fontSize = 28.sp)
                }
            }

            if (showEmojiPicker) {
                EmojiPicker(
                    selectedEmoji = state.icon,
                    onEmojiSelected = {
                        viewModel.onEvent(AddEditAccountEvent.IconChanged(it))
                        showEmojiPicker = false
                    },
                    modifier = Modifier.testTag("account_emoji_grid")
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredName(it)) },
                label = { Text("Account Name (e.g. HDFC Bank)") },
                modifier = Modifier.fillMaxWidth().testTag("account_name_input")
            )

            OutlinedTextField(
                value = state.initialBalance,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredBalance(it)) },
                label = { Text("Initial Balance") },
                modifier = Modifier.fillMaxWidth().testTag("account_balance_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            OutlinedTextField(
                value = state.minBalance,
                onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredMinBalance(it)) },
                label = { Text("Minimum Balance (Buffer)") },
                modifier = Modifier.fillMaxWidth().testTag("account_min_balance_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Set as Primary Wallet")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isPrimary,
                    onCheckedChange = { viewModel.onEvent(AddEditAccountEvent.TogglePrimary) },
                    modifier = Modifier.testTag("account_primary_switch")
                )
            }

            Text("Account Type", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("BANK", "CASH", "CREDIT", "UPI", "SAVINGS", "BACKUP", "INVESTMENT", "OTHER").forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onEvent(AddEditAccountEvent.TypeChanged(type)) },
                        label = { Text(type) },
                        modifier = Modifier.testTag("account_type_$type")
                    )
                }
            }

            // CREDIT CARD SPECIFIC FIELDS (TASK-28)
            AnimatedVisibility(visible = state.type == "CREDIT") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = state.creditLimit,
                        onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredCreditLimit(it)) },
                        label = { Text("Credit Limit") },
                        modifier = Modifier.fillMaxWidth().testTag("account_credit_limit_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("₹ ") }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.billingStartDay,
                            onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredBillingStartDay(it)) },
                            label = { Text("Billing Day (1-31)") },
                            modifier = Modifier.weight(1f).testTag("account_billing_day_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = state.dueDate,
                            onValueChange = { viewModel.onEvent(AddEditAccountEvent.EnteredDueDate(it)) },
                            label = { Text("Due Day (1-31)") },
                            modifier = Modifier.weight(1f).testTag("account_due_day_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
    }
}
