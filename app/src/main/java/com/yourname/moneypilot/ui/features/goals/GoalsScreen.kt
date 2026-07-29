package com.yourname.moneypilot.ui.features.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onGoalClick: (Long) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    
    var showContributeDialog by remember { mutableStateOf<GoalEntity?>(null) }
    var showWithdrawDialog by remember { mutableStateOf<GoalEntity?>(null) }
    var showCompletedGoals by remember { mutableStateOf(false) }

    // Contribution dialog
    if (showContributeDialog != null) {
        val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()
        GoalTransactionDialog(
            title = "Contribute to ${showContributeDialog!!.name}",
            confirmLabel = "Add",
            wallets = wallets,
            onDismiss = { showContributeDialog = null },
            onConfirm = { amount, walletId ->
                viewModel.contributeToGoal(showContributeDialog!!.id, amount, walletId)
                showContributeDialog = null
            }
        )
    }

    // Withdrawal dialog
    if (showWithdrawDialog != null) {
        val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()
        GoalTransactionDialog(
            title = "Withdraw from ${showWithdrawDialog!!.name}",
            confirmLabel = "Withdraw",
            wallets = wallets,
            onDismiss = { showWithdrawDialog = null },
            onConfirm = { amount, walletId ->
                viewModel.withdrawFromGoal(showWithdrawDialog!!.id, amount, walletId)
                showWithdrawDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Savings Goals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showCompletedGoals = !showCompletedGoals }) {
                    Icon(
                        imageVector = if (showCompletedGoals) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Completed Goals",
                        tint = if (showCompletedGoals) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGoal,
                modifier = Modifier.navigationBarsPadding().testTag("goal_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    val filteredGoals = if (showCompletedGoals) {
                        state.data.goals
                    } else {
                        state.data.goals.filter { it.status == "ACTIVE" }
                    }

                    if (filteredGoals.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = if(showCompletedGoals) "No goals found" else "No active goals", modifier = Modifier.testTag("goals_empty_state"))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("goals_list"),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredGoals, key = { it.id }) { goal ->
                                GoalItem(
                                    goal = goal,
                                    isPrivacyMode = isPrivacyMode,
                                    onEditGoal = { onEditGoal(goal.id) },
                                    onGoalClick = { onGoalClick(goal.id) },
                                    onContribute = { showContributeDialog = goal },
                                    onWithdraw = { showWithdrawDialog = goal }
                                )
                                GoalRecentEntries(goal.id, viewModel, isPrivacyMode)
                            }
                        }
                    }
                }
                is ScreenState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No goals set yet", modifier = Modifier.testTag("goals_empty_state"))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalTransactionDialog(
    title: String,
    confirmLabel: String,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf(wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_dialog_amount_input"),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val walletName = wallets.find { it.id == selectedWalletId }?.name ?: "Select Wallet"
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet.name) },
                                onClick = {
                                    selectedWalletId = wallet.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0 && selectedWalletId != null) {
                        onConfirm(amount, selectedWalletId!!)
                    }
                },
                modifier = Modifier.testTag("goal_dialog_confirm_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("goal_dialog_cancel_button")) { Text("Cancel") }
        }
    )
}

@Composable
fun GoalItem(
    goal: GoalEntity,
    isPrivacyMode: Boolean,
    onEditGoal: () -> Unit,
    onGoalClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit
) {
    val rawProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val targetProgress = rawProgress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        label = "goal_progress_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGoalClick() }
            .testTag("goal_card_${goal.id}"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = if (goal.status == "COMPLETED") 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = goal.icon, modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("goal_name_${goal.id}")
                        )
                        Text(
                            text = "Target: ${goal.targetDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(rawProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (goal.status == "COMPLETED" || rawProgress >= 1f) Color(0xFF00A36C) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("goal_progress_${goal.id}")
                    )
                    IconButton(onClick = onEditGoal) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp).testTag("goal_progress_bar_${goal.id}"),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = if (goal.status == "COMPLETED" || rawProgress >= 1f) Color(0xFF00A36C) else MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if(isPrivacyMode) "••••" else "₹${goal.currentAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("goal_current_amount_${goal.id}")
                )
                Text(
                    text = "Goal: " + if(isPrivacyMode) "••••" else "₹${goal.targetAmount}", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (goal.status == "ACTIVE") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onContribute,
                        modifier = Modifier.weight(1f).testTag("goal_contribute_button_${goal.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = MaterialTheme.colorScheme.primary)
                    }
                    
                    OutlinedButton(
                        onClick = onWithdraw,
                        modifier = Modifier.weight(1f).testTag("goal_withdraw_button_${goal.id}"),
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Withdraw")
                    }
                }
            } else if (goal.status == "COMPLETED") {
                Spacer(modifier = Modifier.height(8.dp))
                Badge(containerColor = Color(0xFF00A36C).copy(alpha = 0.2f), contentColor = Color(0xFF00A36C)) {
                    Text("GOAL MET", modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
fun GoalRecentEntries(goalId: Long, viewModel: GoalsViewModel, isPrivacyMode: Boolean) {
    val entries by viewModel.getTransactionsForGoal(goalId).collectAsState(initial = emptyList())

    if (entries.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Recent history", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            entries.take(3).forEach { txWithDetails ->
                val tx = txWithDetails.transaction
                val isIncome = tx.type == TransactionType.Income
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(if (isIncome) "Withdrawal" else "Contribution", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(tx.dateTime.toLocalDate().toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val displayAmount = if (isPrivacyMode) "••••" else if (isIncome) "- ₹${tx.amount}" else "+ ₹${tx.amount}"
                        Text(
                            text = displayAmount, 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = if (isIncome) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
