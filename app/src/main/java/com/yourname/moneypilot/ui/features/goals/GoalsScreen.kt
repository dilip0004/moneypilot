package com.yourname.moneypilot.ui.features.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.common.ScreenState
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showContributeDialog by remember { mutableStateOf<GoalEntity?>(null) }
    var showWithdrawDialog by remember { mutableStateOf<GoalEntity?>(null) }

    // Contribution dialog
    if (showContributeDialog != null) {
        GoalTransactionDialog(
            title = "Contribute to ${showContributeDialog!!.name}",
            confirmLabel = "Add",
            onDismiss = { showContributeDialog = null },
            onConfirm = { amount ->
                viewModel.contributeToGoal(showContributeDialog!!.id, amount)
                showContributeDialog = null
            }
        )
    }

    // Withdrawal dialog
    if (showWithdrawDialog != null) {
        GoalTransactionDialog(
            title = "Withdraw from ${showWithdrawDialog!!.name}",
            confirmLabel = "Withdraw",
            onDismiss = { showWithdrawDialog = null },
            onConfirm = { amount ->
                viewModel.withdrawFromGoal(showWithdrawDialog!!.id, amount)
                showWithdrawDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            Text(
                text = "Savings Goals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("goals_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        state.data.goals.forEach { goal ->
                            item(key = "goal_${goal.id}") {
                                GoalItem(
                                    goal = goal,
                                    onClick = { onEditGoal(goal.id) },
                                    onContribute = { showContributeDialog = goal },
                                    onWithdraw = { showWithdrawDialog = goal }
                                )
                            }
                            item(key = "history_${goal.id}") {
                                GoalRecentEntries(goal.id, viewModel)
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

@Composable
fun GoalItem(
    goal: GoalEntity,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit
) {
    val targetProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceAtMost(1f),
        label = "goal_progress_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("goal_card_${goal.id}"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Text(
                    text = "${(targetProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("goal_progress_${goal.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp).testTag("goal_progress_bar_${goal.id}"),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "₹${goal.currentAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("goal_current_amount_${goal.id}")
                )
                Text(text = "Goal: ₹${goal.targetAmount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

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
        }
    }
}

@Composable
fun GoalTransactionDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount (₹)") },
                singleLine = true,
                modifier = Modifier.testTag("goal_dialog_amount_input"),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
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
fun GoalRecentEntries(goalId: Long, viewModel: GoalsViewModel) {
    val entries by viewModel.getTransactionsForGoal(goalId).collectAsState(initial = emptyList())

    if (entries.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Recent history", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                        Text(
                            text = if (isIncome) "- ₹${tx.amount}" else "+ ₹${tx.amount}", 
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
