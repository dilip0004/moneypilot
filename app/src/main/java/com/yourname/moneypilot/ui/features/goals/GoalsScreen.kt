package com.yourname.moneypilot.ui.features.goals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.motionTween
import com.yourname.moneypilot.util.formatCompact
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val currencySymbol = rememberCurrencySymbol()
    
    var showContributeDialog by remember { mutableStateOf<GoalEntity?>(null) }
    var showWithdrawDialog by remember { mutableStateOf<GoalEntity?>(null) }
    var showCompletedGoals by remember { mutableStateOf(false) }

    // Contribution dialog
    if (showContributeDialog != null) {
        val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()
        GoalTransactionDialog(
            title = "Deposit to ${showContributeDialog!!.name}",
            confirmLabel = "Deposit",
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
            confirmLabel = "Withdraw Funds",
            wallets = wallets,
            onDismiss = { showWithdrawDialog = null },
            onConfirm = { amount, walletId ->
                viewModel.withdrawFromGoal(showWithdrawDialog!!.id, amount, walletId)
                showWithdrawDialog = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    val allGoals = state.data.goals
                    val activeGoals = allGoals.filter { it.status == "ACTIVE" }
                    val filteredGoals = if (showCompletedGoals) allGoals else activeGoals

                    GoalsSummaryHeader(
                        goals = activeGoals,
                        currencySymbol = currencySymbol,
                        isPrivacyMode = isPrivacyMode,
                        onToggleCompleted = { showCompletedGoals = !showCompletedGoals },
                        isShowingCompleted = showCompletedGoals
                    )

                    if (filteredGoals.isEmpty()) {
                        GoalsEmptyState(onAddGoal)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).testTag("goals_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                        ) {
                            items(filteredGoals, key = { it.id }) { goal ->
                                val index = filteredGoals.indexOf(goal)
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(index * MotionConstants.StaggerDelay.toLong())
                                    visible = true
                                }

                                AnimatedVisibility(
                                    visible = visible,
                                    enter = slideInVertically(animationSpec = motionTween()) { 20 } + fadeIn(animationSpec = motionTween()),
                                    modifier = Modifier.animateItemPlacement()
                                ) {
                                    GoalDashboardCard(
                                        goal = goal,
                                        isPrivacyMode = isPrivacyMode,
                                        currencySymbol = currencySymbol,
                                        onEditGoal = { onEditGoal(goal.id) },
                                        onGoalClick = { onGoalClick(goal.id) },
                                        onContribute = { showContributeDialog = goal },
                                        onWithdraw = { showWithdrawDialog = goal }
                                    )
                                }
                            }
                        }
                    }
                }
                is ScreenState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is ScreenState.Empty -> {
                    GoalsEmptyState(onAddGoal)
                }
            }
        }

        // FAB positioned correctly above bottom nav
        FloatingActionButton(
            onClick = onAddGoal,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 8.dp) // Extra spacing from bottom nav
                .testTag("goal_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Goal", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun GoalsSummaryHeader(
    goals: List<GoalEntity>,
    currencySymbol: String,
    isPrivacyMode: Boolean,
    onToggleCompleted: () -> Unit,
    isShowingCompleted: Boolean
) {
    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentAmount }
    val totalRemaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f

    // Accurate Pace Calculation
    val paceStatus = remember(goals) {
        if (goals.isEmpty()) "N/A"
        else {
            val now = LocalDate.now()
            val behindCount = goals.count { goal ->
                val rawProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                val daysTotal = ChronoUnit.DAYS.between(goal.createdAt.toLocalDate(), goal.targetDate).coerceAtLeast(1)
                val daysPassed = ChronoUnit.DAYS.between(goal.createdAt.toLocalDate(), now).coerceAtLeast(0)
                val expectedProgress = daysPassed.toFloat() / daysTotal
                rawProgress < expectedProgress * 0.8f // 20% tolerance
            }
            when {
                behindCount == 0 -> "Healthy"
                behindCount <= goals.size / 3 -> "Steady"
                else -> "Attention"
            }
        }
    }
    val paceColor = when(paceStatus) {
        "Healthy" -> Color(0xFF00C853)
        "Steady" -> Color(0xFFFFA500)
        "Attention" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Savings Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if(isPrivacyMode) "••••" else totalSaved.formatCompact(currencySymbol),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = " of " + (if(isPrivacyMode) "••••" else totalTarget.formatCompact(currencySymbol)),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onToggleCompleted, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isShowingCompleted) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val pctText = if (overallProgress > 0 && overallProgress < 0.01f) {
                        String.format(java.util.Locale.getDefault(), "%.1f%%", overallProgress * 100)
                    } else {
                        "${(overallProgress * 100).toInt()}%"
                    }
                    Text(
                        text = pctText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { overallProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(label = "Active Goals", value = goals.size.toString())
                SummaryStat(label = "Remaining", value = if(isPrivacyMode) "••••" else totalRemaining.formatCompact(currencySymbol))
                SummaryStat(label = "Avg. Pace", value = paceStatus, color = paceColor)
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun GoalDashboardCard(
    goal: GoalEntity,
    isPrivacyMode: Boolean,
    currencySymbol: String,
    onEditGoal: () -> Unit,
    onGoalClick: () -> Unit,
    onContribute: () -> Unit,
    onWithdraw: () -> Unit
) {
    val rawProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val targetProgress = rawProgress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = motionTween(MotionConstants.DurationCard * 2),
        label = "goal_progress"
    )

    val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.targetDate).coerceAtLeast(0)
    val monthsRemaining = (daysRemaining / 30.0).coerceAtLeast(1.0)
    val monthlyRequired = remainingAmount / monthsRemaining

    // Verified calculation for status
    val status = when {
        goal.status == "COMPLETED" || rawProgress >= 1f -> "Completed"
        rawProgress >= 0.95f -> "Almost There"
        daysRemaining < 30 && rawProgress < 0.8f -> "Behind Schedule"
        daysRemaining < 90 && rawProgress < 0.5f -> "Slightly Behind"
        else -> "On Track"
    }

    val statusColor = when(status) {
        "Completed" -> Color(0xFF00C853)
        "Almost There" -> Color(0xFFFFD600)
        "Slightly Behind" -> Color(0xFFFFA500)
        "Behind Schedule" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onGoalClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = goal.icon, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(text = status, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                IconButton(onClick = onEditGoal, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val savedText = if(isPrivacyMode) "••••" else goal.currentAmount.formatCompact(currencySymbol)
                val targetText = if(isPrivacyMode) "••••" else goal.targetAmount.formatCompact(currencySymbol)
                
                Text(
                    text = "$savedText / $targetText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black
                )
                
                val pctText = if (rawProgress > 0 && rawProgress < 0.01f) {
                    String.format(java.util.Locale.getDefault(), "%.1f%%", rawProgress * 100)
                } else {
                    "${(rawProgress * 100).toInt()}%"
                }
                Text(
                    text = pctText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GoalInsightItem(
                    label = formatTimeRemaining(goal.targetDate),
                    value = goal.targetDate.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    alignment = Alignment.Start
                )
                GoalInsightItem(
                    label = "Remaining",
                    value = if(isPrivacyMode) "••••" else remainingAmount.formatCompact(currencySymbol),
                    alignment = Alignment.CenterHorizontally
                )
                GoalInsightItem(
                    label = "Need to Save",
                    value = if(isPrivacyMode) "••••" else "${monthlyRequired.formatCompact(currencySymbol)}/mo",
                    alignment = Alignment.End,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }

            if (goal.status == "ACTIVE") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onContribute,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Deposit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onWithdraw,
                        modifier = Modifier.weight(1.3f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Withdraw Funds", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoalInsightItem(
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = valueColor)
    }
}

private fun formatTimeRemaining(targetDate: LocalDate): String {
    val now = LocalDate.now()
    if (targetDate.isBefore(now)) return "Overdue"
    
    val period = Period.between(now, targetDate)
    val years = period.years
    val months = period.months
    val days = period.days

    return when {
        years > 0 -> "$years Year${if (years > 1) "s" else ""} Left"
        months > 0 -> "$months Month${if (months > 1) "s" else ""} Left"
        else -> "$days Day${if (days > 1) "s" else ""} Left"
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
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_dialog_amount_input"),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
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
                        label = { Text("Payment Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                modifier = Modifier.testTag("goal_dialog_confirm_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GoalsEmptyState(onAddGoal: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Start your first savings goal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Track major purchases, emergency funds, or vacations with precision.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddGoal,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Goal")
        }
    }
}
