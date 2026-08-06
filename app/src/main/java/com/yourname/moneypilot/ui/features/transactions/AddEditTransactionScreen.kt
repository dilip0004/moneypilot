package com.yourname.moneypilot.ui.features.transactions

import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXForward
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXBackward
import com.yourname.moneypilot.ui.theme.motion.motionTween
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.components.CalculatorKeyboard
import com.yourname.moneypilot.ui.features.transactions.components.*
import com.yourname.moneypilot.util.rememberCurrencySymbol
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class QuickRecordIntent(val title: String, val icon: ImageVector, val color: Color) {
    EXPENSE("Expense", Icons.Default.ShoppingCart, Color(0xFFF44336)),
    INCOME("Income", Icons.Default.Payments, Color(0xFF4CAF50)),
    TRANSFER("Transfer", Icons.AutoMirrored.Filled.CompareArrows, Color(0xFF2196F3)),
    GOAL("Goal", Icons.Default.TrackChanges, Color(0xFFFF9800)),
    LOAN("Loan / EMI", Icons.Default.AccountBalance, Color(0xFF9C27B0)),
    INVESTMENT("Investment", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF00BCD4)),
    REFUND("Refund", Icons.AutoMirrored.Filled.Undo, Color(0xFF795548))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onPopBackStack: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onSaveClickOverride: (() -> Unit)? = null,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val currencySymbol = rememberCurrencySymbol()

    var showCalculator by remember { mutableStateOf(false) }
    var selectedIntent by remember { mutableStateOf<QuickRecordIntent?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = state.date.hour,
        initialMinute = state.date.minute
    )

    // Skip intent selection if editing or if intent was already inferred (e.g. from SMS)
    LaunchedEffect(state.isEditing, state.reviewMode) {
        if (state.isEditing || state.reviewMode) {
            selectedIntent = when {
                state.isRefund -> QuickRecordIntent.REFUND
                state.loanId != null -> QuickRecordIntent.LOAN
                state.goalId != null -> QuickRecordIntent.GOAL
                state.investmentId != null -> QuickRecordIntent.INVESTMENT
                state.type == TransactionType.Income -> QuickRecordIntent.INCOME
                else -> QuickRecordIntent.EXPENSE
            }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditTransactionViewModel.UiEvent.SaveTransaction -> onPopBackStack()
                is AddEditTransactionViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = Instant.ofEpochMilli(datePickerState.selectedDateMillis ?: Instant.now().toEpochMilli())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    viewModel.onEvent(AddEditTransactionEvent.DateChanged(LocalDateTime.of(selectedDate, selectedTime)))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (state.isEditing) "Edit Record" else "Quick Record",
                        fontWeight = FontWeight.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedIntent != null && !state.isEditing && !state.reviewMode) {
                            selectedIntent = null
                        } else {
                            onPopBackStack()
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedIntent != null && !showCalculator) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = tween(MotionConstants.DurationButton),
                    label = "fab_scale"
                )

                FloatingActionButton(
                    onClick = { 
                        if (onSaveClickOverride != null) {
                            onSaveClickOverride()
                        } else {
                            viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .testTag("add_tx_save")
                        .graphicsLayer(scaleX = fabScale, scaleY = fabScale)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedIntent,
            transitionSpec = {
                if (targetState != null) {
                    SharedAxisXForward
                } else {
                    SharedAxisXBackward
                }.using(SizeTransform(clip = false))
            },
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "form_transition"
        ) { intent ->
            if (intent == null) {
                IntentSelector(onIntentSelected = {
                    if (it == QuickRecordIntent.TRANSFER) {
                        onNavigateToTransfer()
                    } else {
                        selectedIntent = it
                        // Sync ViewModel Type
                        when(it) {
                            QuickRecordIntent.INCOME -> viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Income))
                            QuickRecordIntent.REFUND -> {
                                viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Income))
                                viewModel.onEvent(AddEditTransactionEvent.ToggleRefund(true))
                            }
                            else -> viewModel.onEvent(AddEditTransactionEvent.TypeChanged(TransactionType.Expense))
                        }
                    }
                })
            } else {
                DynamicFormContainer(
                    intent = intent,
                    state = state,
                    onEvent = viewModel::onEvent,
                    onShowCalculator = { 
                        focusManager.clearFocus()
                        showCalculator = true 
                        coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                    },
                    onShowDatePicker = {
                        focusManager.clearFocus()
                        showCalculator = false
                        showDatePicker = true
                    },
                    currencySymbol = currencySymbol,
                    scrollState = scrollState,
                    isCalculatorVisible = showCalculator
                )
            }
        }

        if (showCalculator) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                CalculatorKeyboard(
                    initialValue = state.amount,
                    onValueChange = { viewModel.onEvent(AddEditTransactionEvent.EnteredAmount(it)) },
                    onDone = { showCalculator = false; focusManager.clearFocus() }
                )
            }
        }
    }
}

@Composable
fun IntentSelector(onIntentSelected: (QuickRecordIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "What do you want to record?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Choose a type to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(QuickRecordIntent.entries) { intent ->
                var visible by remember { mutableStateOf(false) }
                val index = QuickRecordIntent.entries.indexOf(intent)
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * 40L)
                    visible = true
                }
                
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = motionTween()) + fadeIn(animationSpec = motionTween())
                ) {
                    TransactionIntentCard(
                        title = intent.title,
                        icon = intent.icon,
                        color = intent.color,
                        onClick = { onIntentSelected(intent) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicFormContainer(
    intent: QuickRecordIntent,
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    onShowDatePicker: () -> Unit,
    currencySymbol: String,
    scrollState: androidx.compose.foundation.ScrollState,
    isCalculatorVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = if (isCalculatorVisible) 300.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.reviewMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Message, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Parsed from SMS. Please review and accept.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEvent(AddEditTransactionEvent.AcceptSmsReview) }) { Text("Accept") }
                }
            }
        }

        when (intent) {
            QuickRecordIntent.EXPENSE -> ExpenseForm(state, onEvent, onShowCalculator, currencySymbol)
            QuickRecordIntent.INCOME -> IncomeForm(state, onEvent, onShowCalculator, currencySymbol)
            QuickRecordIntent.GOAL -> GoalForm(state, onEvent, onShowCalculator, currencySymbol)
            QuickRecordIntent.LOAN -> LoanForm(state, onEvent, onShowCalculator, currencySymbol)
            QuickRecordIntent.INVESTMENT -> InvestmentForm(state, onEvent, onShowCalculator, currencySymbol)
            QuickRecordIntent.REFUND -> RefundForm(state, onEvent, onShowCalculator, currencySymbol)
            QuickRecordIntent.TRANSFER -> { /* Handled by navigation */ }
        }

        // Section 3.2 Compliance: Transaction Tags UI (Show for all forms)
        if (state.availableTags.isNotEmpty()) {
            SectionHeader("Tags")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.availableTags.forEach { tag ->
                    val isSelected = state.selectedTagIds.contains(tag.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEvent(AddEditTransactionEvent.ToggleTag(tag.id)) },
                        label = { Text(tag.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(tag.color).copy(alpha = 0.3f),
                            selectedLabelColor = Color(tag.color)
                        ),
                        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
