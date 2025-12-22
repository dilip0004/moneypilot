package com.yourname.moneypilot.ui.features.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onPopBackStack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Analytics") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScreenState.Success -> {
                    val summary = state.data
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Monthly Summary", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Income", color = IncomeGreen)
                                        Text("INR ${summary.totalIncome}", fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Expenses", color = ExpenseRed)
                                        Text("INR ${summary.totalExpense}", fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Cash Flow", fontWeight = FontWeight.SemiBold)
                                        Text("INR ${summary.totalIncome - summary.totalExpense}", color = if (summary.totalIncome >= summary.totalExpense) IncomeGreen else ExpenseRed)
                                    }
                                }
                            }
                        }

                        if (summary.categorySpending.isNotEmpty()) {
                            item {
                                Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    PieChart(summary.categorySpending)
                                }
                            }
                        }
                        
                        item {
                            Text("Daily Spending Trend", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val maxSpending = summary.dailySpending.values.maxOrNull() ?: 1.0
                                (1..31).forEach { day ->
                                    val amount = summary.dailySpending[day] ?: 0.0
                                    val heightFactor = (amount / maxSpending).toFloat()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(heightFactor.coerceAtLeast(0.05f))
                                            .background(if (amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text("No data available for this month.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun PieChart(data: Map<Long?, Double>) {
    val total = data.values.sum()
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)
    
    Canvas(modifier = Modifier.size(180.dp)) {
        var startAngle = -90f
        data.values.forEachIndexed { index, value ->
            val sweepAngle = (value / total).toFloat() * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}
