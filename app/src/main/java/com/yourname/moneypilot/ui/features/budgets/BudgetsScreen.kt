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
    val isScrolling = lazyListState.isScrollInProgress
    val fabExpanded by remember { derivedStateOf { !isScrolling } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Month Navigator & Summary (Stay visible during loading)
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                    ) {
                        // Advisory Section
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

        ExtendedFloatingActionButton(
            expanded = fabExpanded,
            onClick = { onAddBudget(selectedMonthStr) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Budget") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 8.dp)
        )
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
        IconButton(
            onClick = { onDateChange(selectedDate.minusMonths(1)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Prev Month",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        IconButton(
            onClick = { onDateChange(selectedDate.plusMonths(1)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BudgetsOverviewCard(totalBudget: Double, totalSpent: Double, currencySymbol: String, isPrivacyMode: Boolean) {
    val usage = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f
    val remaining = (totalBudget - totalSpent).coerceAtLeast(0.0)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if(isPrivacyMode) "••••" else totalSpent.formatCompact(currencySymbol),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = if (totalSpent > totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("of ${if(isPrivacyMode) "••••" else totalBudget.formatCompact(currencySymbol)} budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if(isPrivacyMode) "••••" else remaining.formatCompact(currencySymbol) + " left",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { usage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (usage >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
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
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = category.icon, fontSize = 18.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name, 
                        style = MaterialTheme.typography.bodyLarge, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subcategory != null) {
                        Text(
                            text = subcategory.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(usage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                    val displaySpent = if(isPrivacyMode) "••••" else budget.spentAmount.formatCompact(currencySymbol)
                    val displayLimit = if(isPrivacyMode) "••••" else budget.amount.formatCompact(currencySymbol)
                    Text(
                        text = "$displaySpent / $displayLimit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { usage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun BudgetsEmptyState(onAddBudget: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No budgets set yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            "Create a budget to start controlling your spending and saving more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddBudget) {
            Text("Create Budget")
        }
    }
}

@Composable
fun BudgetAdvisoryCard(advisory: BudgetAdvisory, isPrivacyMode: Boolean) {
    val colors = LocalFinanceColors.current
    val cardColor = when(advisory.type) {
        AdvisoryType.OPTIMIZE -> Color(0xFF00A36C).copy(alpha = 0.08f)
        AdvisoryType.INCREASE -> colors.warning.copy(alpha = 0.08f)
        AdvisoryType.CAUTION -> colors.expense.copy(alpha = 0.08f)
    }
    val iconColor = when(advisory.type) {
        AdvisoryType.OPTIMIZE -> Color(0xFF00A36C)
        AdvisoryType.INCREASE -> colors.warning
        AdvisoryType.CAUTION -> colors.expense
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = iconColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = advisory.categoryName, 
                    style = MaterialTheme.typography.labelMedium, 
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = advisory.reason, 
                    style = MaterialTheme.typography.bodySmall, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            val suggestion = if(isPrivacyMode) "••••" else "₹${advisory.suggestedLimit.toInt()}"
            Text(
                text = "Target: $suggestion", 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Black, 
                color = iconColor
            )
        }
    }
}
