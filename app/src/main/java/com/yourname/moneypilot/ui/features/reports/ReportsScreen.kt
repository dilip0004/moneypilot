package com.yourname.moneypilot.ui.features.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    Color(0xFF7B5CFA), Color(0xFF0067FF), Color(0xFF00A36C), Color(0xFFFF5733),
    Color(0xFFE91E63), Color(0xFF607D8B), Color(0xFF00BCD4), Color(0xFFFFC107)
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
                        title = { Text("Stats", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onPopBackStack) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        TimeRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = reportState.timeRange == range,
                                onClick = { viewModel.onTimeRangeChange(range) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeRange.entries.size)
                            ) {
                                Text(range.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }, fontSize = 12.sp)
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = reportState.reportType.ordinal,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (reportState.reportType.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[reportState.reportType.ordinal]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        ReportType.entries.forEach { type ->
                            Tab(
                                selected = reportState.reportType == type,
                                onClick = { viewModel.onReportTypeChange(type) },
                                text = { 
                                    Text(
                                        text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }, 
                                        fontSize = 12.sp,
                                        color = if (reportState.reportType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                is ScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ScreenState.Success -> {
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        item {
                            DateNavigatorCompact(
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

                        // THE INSIGHT ROW
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InsightTile(
                                    label = "Efficiency",
                                    value = "${(data.savingsPercentage * 100).toInt()}%",
                                    subLabel = "Saved",
                                    modifier = Modifier.weight(1f)
                                )
                                InsightTile(
                                    label = "Velocity",
                                    value = "₹${data.dailyAverage.toInt()}",
                                    subLabel = "per day",
                                    modifier = Modifier.weight(1f)
                                )
                                InsightTile(
                                    label = "Frequency",
                                    value = "${data.transactionCount}",
                                    subLabel = "Entries",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "₹ ${String.format(Locale.getDefault(), "%,.2f", data.totalAmount)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (data.reportType == ReportType.INCOME) IncomeGreen else if (data.reportType == ReportType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // FIXED PIE CHART ALIGNMENT - NOW FULL PIE
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        if (data.reportType == ReportType.CASH_FLOW) {
                                            CashFlowBarChartCompact(data.chartData)
                                        } else {
                                            PieChartCompact(data.categoryBreakdown)
                                        }
                                    }
                                }
                            }
                        }

                        if (data.reportType != ReportType.CASH_FLOW && data.categoryBreakdown.isNotEmpty()) {
                            itemsIndexed(data.categoryBreakdown) { index, rank ->
                                CategoryRankItemCompact(
                                    rank = rank,
                                    categoryColor = CHART_COLORS[index % CHART_COLORS.size]
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun InsightTile(label: String, value: String, subLabel: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DateNavigatorCompact(date: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, range: TimeRange, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ChevronLeft, null) }
        val label = when (range) {
            TimeRange.WEEKLY -> "${rangeStart.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${rangeEnd.format(DateTimeFormatter.ofPattern("dd MMM"))}"
            TimeRange.MONTHLY -> "${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.year}"
            TimeRange.YEARLY -> "${date.year}"
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ChevronRight, null) }
    }
}

@Composable
fun PieChartCompact(ranks: List<CategoryRank>) {
    val total = ranks.sumOf { it.amount }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(ranks) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            ranks.forEachIndexed { index, rank ->
                if (total > 0) {
                    val sweepAngle = (rank.amount / total).toFloat() * 360f * animationProgress.value
                    drawArc(
                        color = CHART_COLORS[index % CHART_COLORS.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true // Use center for filled pie slices
                    )
                    startAngle += sweepAngle
                }
            }
        }
    }
}

@Composable
fun CategoryRankItemCompact(rank: CategoryRank, categoryColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rank.icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${rank.name} (${(rank.percentage * 100).toInt()}%)", 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Medium
                )
                Text("₹ ${String.format(Locale.getDefault(), "%,.2f", rank.amount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { rank.percentage },
                modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 2.dp),
                color = categoryColor,
                strokeCap = StrokeCap.Round,
                trackColor = categoryColor.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun CashFlowBarChartCompact(data: Map<Int, Double>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val max = data.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        data.values.forEach { amount ->
            val heightFactor = (amount / max).toFloat().coerceAtLeast(0.05f)
            Box(modifier = Modifier.weight(1f).fillMaxHeight(heightFactor).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
        }
    }
}
