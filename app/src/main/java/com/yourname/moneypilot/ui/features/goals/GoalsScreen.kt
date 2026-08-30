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
import com.yourname.moneypilot.ui.components.*
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Black) }
            )
        },
        floatingActionButton = {
            MoneyPilotFAB(
                onClick = onAddGoal,
                icon = Icons.Default.Add,
                label = "New Goal",
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ScreenState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ScreenState.Success -> {
                    val allGoals = state.data.goals
                    val activeGoals = allGoals.filter { it.status == "ACTIVE" }
                    val filteredGoals = if (showCompletedGoals) allGoals else activeGoals

                    Column(modifier = Modifier.fillMaxSize()) {
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
                is ScreenState.Empty -> GoalsEmptyState(onAddGoal)
                is ScreenState.Error -> Text(state.message, Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            }
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
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f

    FinancialSummarySurface(
        title = "Total Savings Progress",
        primaryValue = if(isPrivacyMode) "••••" else totalSaved.formatCompact(currencySymbol),
        progress = overallProgress,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        secondaryInfo = {
            SummaryStat(label = "Target", value = if(isPrivacyMode) "••••" else totalTarget.formatCompact(currencySymbol))
            SummaryStat(label = "Goals", value = goals.size.toString())
            IconButton(onClick = onToggleCompleted, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isShowingCompleted) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
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

    GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable { onGoalClick() },
        shape = RoundedCornerShape(16.dp),
        opacity = GlassLevel.High
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor.copy(alpha = 0.15f)
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
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(text = status, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                IconButton(onClick = onEditGoal, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val savedText = if(isPrivacyMode) "••••" else goal.currentAmount.formatCompact(currencySymbol)
                val targetText = if(isPrivacyMode) "••••" else goal.targetAmount.formatCompact(currencySymbol)
                
                Text(
                    text = "$savedText / $targetText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Text(
                    text = "${(targetProgress * 100).toInt()}%",
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
                trackColor = Color.White.copy(alpha = 0.1f),
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
                    label = "Monthly Need",
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text("Deposit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onWithdraw,
                        modifier = Modifier.weight(1.3f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Withdraw Funds", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = alignment) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = valueColor)
    }
}

private fun formatTimeRemaining(targetDate: LocalDate): String {
    val now = LocalDate.now()
    if (targetDate.isBefore(now)) return "Overdue"
    val period = Period.between(now, targetDate)
    return when {
        period.years > 0 -> "${period.years}y Left"
        period.months > 0 -> "${period.months}m Left"
        else -> "${period.days}d Left"
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
                    modifier = Modifier.fillMaxWidth(),
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
                shape = RoundedCornerShape(10.dp)
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun GoalsEmptyState(onAddGoal: () -> Unit) {
    EmptyState(
        icon = Icons.Default.TrackChanges,
        title = "Start your savings goal",
        subtitle = "Track major purchases, emergency funds, or vacations with precision.",
        action = {
            Button(onClick = onAddGoal) { Text("Create Goal") }
        }
    )
}
