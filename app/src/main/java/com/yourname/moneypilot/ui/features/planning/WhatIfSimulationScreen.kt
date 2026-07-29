package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.domain.usecase.analytics.SimulationImpact
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.format.DateTimeFormatter

@Composable
fun WhatIfSimulationScreen(
    viewModel: WhatIfSimulationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val financeColors = LocalFinanceColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is ScreenState.Success -> {
                val data = state.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SimulationHeaderCard(data.totalSimulatedSavings)
                    }

                    item {
                        Text("Simulate Spending Cuts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Reduce monthly spend in these categories to see the impact on your goals.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    items(data.categories, key = { it.categoryId ?: 0L }) { category ->
                        CategoryReductionSlider(
                            categoryName = category.name,
                            categoryIcon = category.icon,
                            currentSpend = category.amount,
                            reductionPercentage = data.categoryReductions[category.categoryId] ?: 0f,
                            onReductionChange = { viewModel.onReductionChanged(category.categoryId ?: -1L, it) }
                        )
                    }

                    if (data.simulationResults.isNotEmpty()) {
                        item {
                            Text("Goal Acceleration Impact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        items(data.simulationResults, key = { it.goalId }) { impact ->
                            GoalImpactCard(impact)
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun SimulationHeaderCard(addedSavings: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Savings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("Extra Monthly Savings", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "₹ ${addedSavings.toInt()}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Available to boost your financial goals",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryReductionSlider(
    categoryName: String,
    categoryIcon: String,
    currentSpend: Double,
    reductionPercentage: Float,
    onReductionChange: (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$categoryIcon $categoryName", fontWeight = FontWeight.Bold)
                Text("Spent: ₹${currentSpend.toInt()}", style = MaterialTheme.typography.bodySmall)
            }
            
            Slider(
                value = reductionPercentage,
                onValueChange = onReductionChange,
                valueRange = 0f..1f,
                steps = 19
            )
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val reductionAmount = (currentSpend * reductionPercentage).toInt()
                Text("Cut: ${(reductionPercentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Text("Save: ₹$reductionAmount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GoalImpactCard(impact: SimulationImpact) {
    val dateFmt = DateTimeFormatter.ofPattern("MMM yyyy")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if(impact.monthsSaved > 0) Color(0xFF00A36C).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(impact.goalName, fontWeight = FontWeight.Bold)
                Text(
                    text = "End Date: ${impact.simulatedEndDate.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (impact.monthsSaved > 0) {
                    Text(
                        text = "🏃 Accelerates by ${impact.monthsSaved} months!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00A36C),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (impact.monthsSaved > 0) {
                Icon(Icons.Default.AutoGraph, null, tint = Color(0xFF00A36C))
            }
        }
    }
}
