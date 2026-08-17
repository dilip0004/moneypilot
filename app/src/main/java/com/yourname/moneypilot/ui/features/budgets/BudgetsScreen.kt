package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val currencySymbol = rememberCurrencySymbol()
    
    val budgetsState by viewModel.state.collectAsState()
    val state = (uiState as? ScreenState.Success)?.data
    val selectedMonthStr = budgetsState.selectedDate.toString()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Month Navigator
            state?.let { s ->
                BudgetMonthNavigator(
                    selectedDate = s.selectedDate,
                    onDateChange = { viewModel.onDateChange(it) }
                )
                
                // Budget Overview
                BudgetsOverviewCard(
                    totalBudget = s.totalBudget,
                    totalSpent = s.totalSpent,
                    currencySymbol = currencySymbol,
                    isPrivacyMode = isPrivacyMode
                )
            }

            when (val currentUiState = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    val data = currentUiState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                    ) {
                        // Advisory Section
                        if (data.advisories.isNotEmpty()) {
                            items(data.advisories) { advisory ->
                                BudgetAdvisoryCard(advisory, isPrivacyMode)
                            }
                        }

                        items(data.budgets, key = { it.budget.id }) { budgetDetails ->
                            // PERFORMANCE FIX: Removed staggering animations that cause jank during scroll
                            CompactBudgetItem(
                                budgetDetails = budgetDetails, 
                                isPrivacyMode = isPrivacyMode, 
                                currencySymbol = currencySymbol,
                                onClick = { 
                                    onEditBudget(budgetDetails.budget.id, data.selectedDate.toString()) 
                                }
                            )
                        }
                    }
                }
                is ScreenState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${currentUiState.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is ScreenState.Empty -> {
                    BudgetsEmptyState(onAddBudget = { onAddBudget(selectedMonthStr) })
                }
            }
        }

        FloatingActionButton(
            onClick = { onAddBudget(selectedMonthStr) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Budget", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun BudgetMonthNavigator(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateChange(selectedDate.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Month")
        }
        Text(
            text = "${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = { onDateChange(selectedDate.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun BudgetsOverviewCard(totalBudget: Double, totalSpent: Double, currencySymbol: String, isPrivacyMode: Boolean) {
    val usage = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        if(isPrivacyMode) "••••" else totalBudget.formatCompact(currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        if(isPrivacyMode) "••••" else totalSpent.formatCompact(currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (totalSpent > totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { usage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (usage >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(usage * 100).toInt()}% Used",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (usage >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun CompactBudgetItem(
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp), // Sharper premium look
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = category.icon, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name, 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subcategory != null) {
                        Text(
                            text = subcategory.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Text(
                    text = "${(usage * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val displaySpent = if(isPrivacyMode) "••••" else budget.spentAmount.formatCompact(currencySymbol)
                val displayLimit = if(isPrivacyMode) "••••" else budget.amount.formatCompact(currencySymbol)
                
                Text(
                    text = "$displaySpent / $displayLimit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
                
                if (budget.isRecurring) {
                    Text(
                        text = "Recurring",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
        AdvisoryType.OPTIMIZE -> Color(0xFF00A36C).copy(alpha = 0.1f)
        AdvisoryType.INCREASE -> colors.warning.copy(alpha = 0.1f)
        AdvisoryType.CAUTION -> colors.expense.copy(alpha = 0.1f)
    }
    val iconColor = when(advisory.type) {
        AdvisoryType.OPTIMIZE -> Color(0xFF00A36C)
        AdvisoryType.INCREASE -> colors.warning
        AdvisoryType.CAUTION -> colors.expense
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(advisory.categoryName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(advisory.reason, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                val suggestion = if(isPrivacyMode) "••••" else "₹${advisory.suggestedLimit.toInt()}"
                Text("Suggested limit: $suggestion", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = iconColor)
            }
        }
    }
}
