package com.yourname.moneypilot.ui.features.budgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.domain.usecase.budget.AdvisoryType
import com.yourname.moneypilot.domain.usecase.budget.BudgetAdvisory
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors

@Composable
fun BudgetsScreen(
    onAddBudget: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBudget,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        // Section 3.6 Compliance: Advisory Planning
                        if (state.data.advisories.isNotEmpty()) {
                            item {
                                Text("Planning Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(state.data.advisories) { advisory ->
                                BudgetAdvisoryCard(advisory, isPrivacyMode)
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        }

                        item {
                            Text("Active Budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        items(state.data.budgets) { budgetDetails ->
                            BudgetItem(budgetDetails, isPrivacyMode)
                        }
                    }
                }
                is ScreenState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No budgets set for this month")
                    }
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(advisory.categoryName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(advisory.reason, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val suggestion = if(isPrivacyMode) "••••" else "₹${advisory.suggestedLimit.toInt()}"
                Text("Suggested limit: $suggestion", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = iconColor)
            }
        }
    }
}

@Composable
fun BudgetItem(budgetDetails: BudgetWithDetails, isPrivacyMode: Boolean) {
    val budget = budgetDetails.budget
    val category = budgetDetails.category
    val subcategory = budgetDetails.subcategory
    val financeColors = LocalFinanceColors.current
    
    val targetProgress = if (budget.amount > 0) (budget.spentAmount / budget.amount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceAtMost(1f),
        label = "budget_progress_animation"
    )
    
    val isOverBudget = budget.spentAmount > budget.amount
    val isNearLimit = !isOverBudget && (targetProgress * 100) >= budget.alertThreshold
    
    val statusColor = when {
        isOverBudget -> financeColors.expense
        isNearLimit -> financeColors.warning
        else -> financeColors.budget
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = category.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (subcategory != null) {
                        Text(
                            text = "Subcategory: ${subcategory.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(targetProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (isNearLimit) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val displaySpent = if(isPrivacyMode) "••••" else "₹${budget.spentAmount.toInt()}"
                val displayLimit = if(isPrivacyMode) "••••" else "₹${budget.amount.toInt()}"
                
                Text(
                    text = "$displaySpent spent",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverBudget) statusColor else MaterialTheme.colorScheme.onSurface
                )
                Text(text = "Limit: $displayLimit", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
