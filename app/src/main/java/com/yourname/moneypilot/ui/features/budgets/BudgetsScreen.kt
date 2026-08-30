package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.domain.usecase.budget.AdvisoryType
import com.yourname.moneypilot.domain.usecase.budget.BudgetAdvisory
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.*
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.motionTween
import com.yourname.moneypilot.util.formatCompact
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun BudgetsScreen(
    onAddBudget: (String?) -> Unit,
    onEditBudget: (Long, String?) -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val budgetsState by viewModel.state.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val currencySymbol = rememberCurrencySymbol()
    
    val selectedMonthStr = budgetsState.selectedDate.toString()
    
    val lazyListState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { !lazyListState.isScrollInProgress } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text("Budget Allocation", fontWeight = FontWeight.Black) }
            )
        },
        floatingActionButton = {
            MoneyPilotFAB(
                onClick = { onAddBudget(selectedMonthStr) },
                icon = Icons.Default.Add,
                label = "Set Budget",
                expanded = fabExpanded,
                modifier = Modifier
                    .padding(bottom = 8.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            BudgetMonthNavigator(
                selectedDate = budgetsState.selectedDate,
                onDateChange = { viewModel.onDateChange(it) }
            )
            
            BudgetsOverviewCard(
                totalBudget = budgetsState.totalBudget,
                totalSpent = budgetsState.totalSpent,
                currencySymbol = currencySymbol,
                isPrivacyMode = isPrivacyMode
            )

            when (val currentUiState = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                }
                is ScreenState.Success -> {
                    val data = currentUiState.data
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                    ) {
                        if (data.advisories.isNotEmpty()) {
                            items(data.advisories) { advisory ->
                                BudgetAdvisoryCard(advisory, isPrivacyMode)
                            }
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }

                        items(data.budgets, key = { it.budget.id }) { budgetDetails ->
                            BudgetListItem(
                                budgetDetails = budgetDetails, 
                                isPrivacyMode = isPrivacyMode, 
                                currencySymbol = currencySymbol,
                                onClick = { 
                                    onEditBudget(budgetDetails.budget.id, budgetsState.selectedDate.toString()) 
                                }
                            )
                        }
                    }
                }
                is ScreenState.Error -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${currentUiState.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        BudgetsEmptyState(onAddBudget = { onAddBudget(selectedMonthStr) })
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetMonthNavigator(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateChange(selectedDate.minusMonths(1)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
        }
        
        Text(
            text = "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        
        IconButton(onClick = { onDateChange(selectedDate.plusMonths(1)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BudgetsOverviewCard(totalBudget: Double, totalSpent: Double, currencySymbol: String, isPrivacyMode: Boolean) {
    val usage = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f
    val remaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
    
    FinancialSummarySurface(
        title = "Budget Utilization",
        primaryValue = if(isPrivacyMode) "••••" else totalSpent.formatCompact(currencySymbol),
        progress = usage,
        progressColor = if (usage >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        secondaryInfo = {
            SummaryStat(label = "Limit", value = if(isPrivacyMode) "••••" else totalBudget.formatCompact(currencySymbol))
            SummaryStat(
                label = "Remaining", 
                value = if(isPrivacyMode) "••••" else remaining.formatCompact(currencySymbol),
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
fun BudgetListItem(
    budgetDetails: BudgetWithDetails, 
    isPrivacyMode: Boolean, 
    currencySymbol: String,
    onClick: () -> Unit
) {
    val budget = budgetDetails.budget
    val category = budgetDetails.category
    val subcategory = budgetDetails.subcategory
    val financeColors = LocalFinanceColors.current
    
    val usage = if (budget.amount > 0) (budget.spentAmount / budget.amount).toFloat() else 0f
    
    val isOverBudget = budget.spentAmount > budget.amount
    val isNearLimit = !isOverBudget && (usage * 100) >= budget.alertThreshold
    
    val statusColor = when {
        isOverBudget -> financeColors.expense
        isNearLimit -> financeColors.warning
        else -> MaterialTheme.colorScheme.primary
    }
    
    MoneyPilotListItem(
        icon = category.icon,
        title = category.name,
        subtitle = subcategory?.name,
        statusColor = statusColor,
        onClick = onClick,
        trailingContent = {
            Text(
                text = "${(usage * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
                fontWeight = FontWeight.Black
            )
            val displayLimit = if(isPrivacyMode) "••••" else budget.amount.formatCompact(currencySymbol)
            Text(
                text = "/ $displayLimit",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    )
}

@Composable
fun BudgetsEmptyState(onAddBudget: () -> Unit) {
    EmptyState(
        icon = Icons.Default.BarChart,
        title = "No budgets set yet",
        subtitle = "Create a budget to start controlling your spending and saving more.",
        action = {
            Button(onClick = onAddBudget) { Text("Create Budget") }
        }
    )
}

@Composable
fun BudgetAdvisoryCard(advisory: BudgetAdvisory, isPrivacyMode: Boolean) {
    val colors = LocalFinanceColors.current
    val statusColor = when(advisory.type) {
        AdvisoryType.OPTIMIZE -> Color(0xFF00A36C)
        AdvisoryType.INCREASE -> colors.warning
        AdvisoryType.CAUTION -> colors.expense
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = statusColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = advisory.categoryName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = statusColor)
                Text(text = advisory.reason, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
            }
            val suggestion = if(isPrivacyMode) "••••" else "₹${advisory.suggestedLimit.toInt()}"
            Text(text = "Aim: $suggestion", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = statusColor)
        }
    }
}
