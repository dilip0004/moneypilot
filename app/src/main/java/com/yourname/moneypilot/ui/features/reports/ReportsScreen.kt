package com.yourname.moneypilot.ui.features.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

private val CHART_COLORS = listOf(
    Color(0xFF7B5CFA), // Purple
    Color(0xFF0067FF), // Blue
    Color(0xFF00A36C), // Green
    Color(0xFFFF5733), // Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF607D8B), // Gray
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFFC107)  // Amber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onPopBackStack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = { Text("Financial Stats", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onPopBackStack) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TimeRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = reportState.timeRange == range,
                                onClick = { viewModel.onTimeRangeChange(range) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeRange.entries.size)
                            ) {
                                Text(range.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) })
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = reportState.reportType.ordinal,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        ReportType.entries.forEach { type ->
                            Tab(
                                selected = reportState.reportType == type,
                                onClick = { viewModel.onReportTypeChange(type) },
                                text = { 
                                    Text(
                                        text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScreenState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        item {
                            DateNavigator(
                                date = data.selectedDate,
                                rangeStart = data.rangeStart,
                                rangeEnd = data.rangeEnd,
                                range = data.timeRange,
                                onPrev = { 
                                    val nextDate = when(data.timeRange) {
                                        TimeRange.WEEKLY -> data.selectedDate.minusWeeks(1)
                                        TimeRange.MONTHLY -> data.selectedDate.minusMonths(1)
                                        TimeRange.YEARLY -> data.selectedDate.minusYears(1)
                                    }
                                    viewModel.onDateChange(nextDate)
                                },
                                onNext = { 
                                    val nextDate = when(data.timeRange) {
                                        TimeRange.WEEKLY -> data.selectedDate.plusWeeks(1)
                                        TimeRange.MONTHLY -> data.selectedDate.plusMonths(1)
                                        TimeRange.YEARLY -> data.selectedDate.plusYears(1)
                                    }
                                    viewModel.onDateChange(nextDate)
                                }
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val title = if (data.reportType == ReportType.CASH_FLOW) "Net Flow" else "Total ${data.reportType.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }}"
                                    Text(title, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = "₹ ${String.format(Locale.getDefault(), "%.2f", data.totalAmount)}",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (data.reportType == ReportType.INCOME) IncomeGreen else if (data.reportType == ReportType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(32.dp))
                                    
                                    if (data.reportType == ReportType.CASH_FLOW) {
                                        CashFlowBarChart(data.chartData)
                                    } else {
                                        PieChart(data.categoryBreakdown)
                                    }
                                }
                            }
                        }

                        if (data.reportType != ReportType.CASH_FLOW && data.categoryBreakdown.isNotEmpty()) {
                            item {
                                Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            itemsIndexed(data.categoryBreakdown) { index, rank ->
                                CategoryRankItem(
                                    rank = rank,
                                    categoryColor = CHART_COLORS[index % CHART_COLORS.size]
                                )
                            }
                        } else if (data.categoryBreakdown.isEmpty() && data.reportType != ReportType.CASH_FLOW) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("No data for this selection", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available for this selection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DateNavigator(
    date: LocalDate, 
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    range: TimeRange, 
    onPrev: () -> Unit, 
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, null) }
        val label = when (range) {
            TimeRange.WEEKLY -> "${rangeStart.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${rangeEnd.format(DateTimeFormatter.ofPattern("dd MMM"))}"
            TimeRange.MONTHLY -> "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"
            TimeRange.YEARLY -> "${date.year}"
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, null) }
    }
}

@Composable
fun PieChart(ranks: List<CategoryRank>) {
    val total = ranks.sumOf { it.amount }
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            var startAngle = -90f
            ranks.forEachIndexed { index, rank ->
                if (total > 0) {
                    val sweepAngle = (rank.amount / total).toFloat() * 360f
                    drawArc(
                        color = CHART_COLORS[index % CHART_COLORS.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
            if (total == 0.0) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 32.dp.toPx())
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > 0) "${ranks.size}" else "0",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Categories", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CategoryRankItem(rank: CategoryRank, categoryColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = categoryColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(rank.icon, fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rank.name, fontWeight = FontWeight.Medium)
                Text("₹ ${String.format(Locale.getDefault(), "%.2f", rank.amount)}", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { rank.percentage },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = categoryColor,
                strokeCap = StrokeCap.Round,
                trackColor = categoryColor.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun CashFlowBarChart(data: Map<Int, Double>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val max = data.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        data.values.forEach { amount ->
            val heightFactor = (amount / max).toFloat().coerceAtLeast(0.05f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFactor)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
            )
        }
    }
}
