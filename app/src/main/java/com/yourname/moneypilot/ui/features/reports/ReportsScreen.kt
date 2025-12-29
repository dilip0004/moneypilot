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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

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
                Column(modifier = Modifier.statusBarsPadding()) {
                    TopAppBar(title = { })
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        TimeRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = reportState.timeRange == range,
                                onClick = { viewModel.onTimeRangeChange(range) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeRange.entries.size)
                            ) {
                                val label = range.name.lowercase().replaceFirstChar { char ->
                                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                                }
                                Text(label, fontSize = 12.sp)
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
                                    val label = type.name.replace("_", " ").lowercase().replaceFirstChar { char ->
                                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                                    }
                                    Text(
                                        text = label, 
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                var selectedViz by remember { mutableIntStateOf(0) }
                                
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
                                    
                                    if (data.reportType == ReportType.CASH_FLOW) {
                                        CashFlowBarChartCompact(data.chartData)
                                    } else {
                                        TabRow(
                                            selectedTabIndex = selectedViz,
                                            containerColor = Color.Transparent,
                                            divider = {},
                                            indicator = {},
                                            modifier = Modifier.width(200.dp).height(32.dp)
                                        ) {
                                            Tab(
                                                selected = selectedViz == 0,
                                                onClick = { selectedViz = 0 },
                                                text = { Text("Distribution", fontSize = 10.sp) }
                                            )
                                            Tab(
                                                selected = selectedViz == 1,
                                                onClick = { selectedViz = 1 },
                                                text = { Text("Trend", fontSize = 10.sp) }
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            if (selectedViz == 0) {
                                                PieChartLabeled(data.categoryBreakdown)
                                            } else {
                                                TrendLineGraphCompact(
                                                    data = data.chartData,
                                                    color = if (data.reportType == ReportType.INCOME) IncomeGreen else if (data.reportType == ReportType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.primary,
                                                    timeRange = data.timeRange
                                                )
                                            }
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
        IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
        val label = when (range) {
            TimeRange.WEEKLY -> {
                val formatter = DateTimeFormatter.ofPattern("dd MMM")
                "${rangeStart.format(formatter)} - ${rangeEnd.format(formatter)}"
            }
            TimeRange.MONTHLY -> {
                val monthName = date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                "$monthName ${date.year}"
            }
            TimeRange.YEARLY -> "${date.year}"
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    }
}

@Composable
fun PieChartLabeled(ranks: List<CategoryRank>) {
    val total = ranks.sumOf { it.amount }
    val animationProgress = remember { Animatable(0f) }
    val onSurface = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(ranks) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(1000))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(32.dp).fillMaxWidth()) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            var startAngle = -90f
            
            ranks.forEachIndexed { index, rank ->
                if (total > 0) {
                    val sweepAngle = (rank.amount / total).toFloat() * 360f * animationProgress.value
                    val color = CHART_COLORS[index % CHART_COLORS.size]
                    
                    // Draw Slice
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    
                    // Draw Pointer line and Label
                    if (sweepAngle > 10f && animationProgress.value > 0.9f) {
                        val midAngle = (startAngle + sweepAngle / 2) * (Math.PI / 180f).toFloat()
                        
                        // Line Start (inside slice)
                        val lineStart = Offset(
                            center.x + cos(midAngle).toFloat() * (radius * 0.6f),
                            center.y + sin(midAngle).toFloat() * (radius * 0.6f)
                        )
                        
                        // Line End (outside slice)
                        val lineEnd = Offset(
                            center.x + cos(midAngle).toFloat() * (radius * 1.25f),
                            center.y + sin(midAngle).toFloat() * (radius * 1.25f)
                        )
                        
                        drawLine(
                            color = onSurface.copy(alpha = 0.4f),
                            start = lineStart,
                            end = lineEnd,
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        val pctValue = (rank.percentage * 100).toInt()
                        val displayText = "${rank.icon} ${rank.name}  $pctValue%"
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            displayText,
                            lineEnd.x,
                            lineEnd.y + if (sin(midAngle) > 0) 20f else -10f,
                            android.graphics.Paint().apply {
                                this.color = onSurface.toArgb()
                                this.textSize = 24f
                                this.textAlign = if (cos(midAngle) > 0) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                                this.isFakeBoldText = true
                            }
                        )
                    }
                    
                    startAngle += sweepAngle
                }
            }
        }
    }
}

@Composable
fun TrendLineGraphCompact(data: Map<Int, Double>, color: Color, timeRange: TimeRange) {
    if (data.isEmpty()) return
    
    val values = data.values.toList()
    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val animationProgress = remember { Animatable(0f) }
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(1000))
    }

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 8.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            
            val path = Path()
            val fillPath = Path()
            
            // Draw Y-axis labels (3 levels)
            val paint = android.graphics.Paint().apply {
                this.color = onSurface.toArgb()
                this.textSize = 20f
                this.textAlign = android.graphics.Paint.Align.LEFT
            }
            drawContext.canvas.nativeCanvas.drawText("₹${max.toInt()}", 0f, 20f, paint)
            drawContext.canvas.nativeCanvas.drawText("₹${(max / 2).toInt()}", 0f, height / 2, paint)
            drawContext.canvas.nativeCanvas.drawText("0", 0f, height, paint)

            data.values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value.toFloat() / max.toFloat() * height * animationProgress.value)
                
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                
                if (index == data.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                )
            )
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val xLabels = when (timeRange) {
                TimeRange.WEEKLY -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                TimeRange.MONTHLY -> listOf("1", "5", "10", "15", "20", "25", "30")
                TimeRange.YEARLY -> listOf("Jan", "Mar", "May", "Jul", "Sep", "Nov")
            }
            xLabels.forEach { label ->
                Text(label, fontSize = 10.sp, color = onSurface)
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
                val pct = (rank.percentage * 100).toInt()
                Text(
                    text = "${rank.name} ($pct%)", 
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
            val heightFactor = (amount.toFloat() / max.toFloat()).coerceAtLeast(0.05f)
            Box(modifier = Modifier.weight(1f).fillMaxHeight(heightFactor).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
        }
    }
}
