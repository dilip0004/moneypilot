package com.yourname.moneypilot.ui.features.reports

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.AppDateNavigator
import com.yourname.moneypilot.ui.components.MoneyPilotSegmentedControl
import com.yourname.moneypilot.ui.theme.IncomeGreen
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.motionTween
import com.yourname.moneypilot.util.formatCompact
import com.yourname.moneypilot.util.formatCurrency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    onPopBackStack: () -> Unit = {},
    viewModel: ReportsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val currencySymbol = preferences?.currency ?: "₹"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    // Spec 1: Period Selector
                    MoneyPilotSegmentedControl(
                        options = TimeRange.entries,
                        selectedOption = reportState.timeRange,
                        onOptionSelected = { viewModel.onTimeRangeChange(it) },
                        labelExtractor = { it.name.lowercase().replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() } },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        iconExtractor = { Icons.Default.CalendarToday }
                    )

                    // Spec 2: Report Type Selector
                    MoneyPilotSegmentedControl(
                        options = ReportType.entries,
                        selectedOption = reportState.reportType,
                        onOptionSelected = { viewModel.onReportTypeChange(it) },
                        labelExtractor = { 
                            when(it) {
                                ReportType.EXPENSE -> "Expense"
                                ReportType.INCOME -> "Income"
                                ReportType.CASH_FLOW -> "Cash Flow"
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ScreenState.Success -> {
                    val data = state.data
                    var selectedViz by remember { mutableIntStateOf(0) }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
                    ) {
                        item {
                            val label = when (data.timeRange) {
                                TimeRange.WEEKLY -> {
                                    val formatter = DateTimeFormatter.ofPattern("dd MMM")
                                    "${data.rangeStart.format(formatter)} – ${data.rangeEnd.format(formatter)}"
                                }
                                TimeRange.MONTHLY -> "${data.selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${data.selectedDate.year}"
                                TimeRange.YEARLY -> "${data.selectedDate.year}"
                            }
                            
                            AppDateNavigator(
                                label = label,
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KPICard(
                                    label = "Savings Rate",
                                    value = if(isPrivacyMode) "••%" else "${(data.keyAnalytics.savingsRate?.times(100))?.toInt() ?: 0}%",
                                    subLabel = if(data.keyAnalytics.savingsRate == null) "No Income" else "Saved",
                                    icon = Icons.Default.AutoGraph,
                                    modifier = Modifier.weight(1f)
                                )
                                KPICard(
                                    label = "Daily Avg",
                                    value = if(isPrivacyMode) "••••" else data.keyAnalytics.dailyAverage?.formatCurrency(currencySymbol) ?: "₹0",
                                    subLabel = "per day",
                                    icon = Icons.Default.Speed,
                                    modifier = Modifier.weight(1f)
                                )
                                KPICard(
                                    label = "Entries",
                                    value = "${data.keyAnalytics.transactionCount}",
                                    subLabel = "Transactions",
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            TotalAmountCard(
                                type = data.reportType,
                                amount = data.totalAmount,
                                currencySymbol = currencySymbol,
                                isPrivacyMode = isPrivacyMode
                            )
                        }

                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                MoneyPilotSegmentedControl(
                                    options = listOf("Distribution", "Trend"),
                                    selectedOption = if (selectedViz == 0) "Distribution" else "Trend",
                                    onOptionSelected = { selectedViz = if (it == "Distribution") 0 else 1 },
                                    labelExtractor = { it },
                                    modifier = Modifier.width(200.dp),
                                    height = 32.dp,
                                    showIcon = false
                                )
                            }
                        }

                        item {
                            ChartCard(
                                reportType = data.reportType,
                                selectedViz = selectedViz,
                                categoryBreakdown = data.categoryBreakdown,
                                chartData = data.chartData,
                                timeRange = data.timeRange,
                                isPrivacyMode = isPrivacyMode
                            )
                        }

                        // Spec 10: Spending Breakdown Title
                        if (data.reportType != ReportType.CASH_FLOW && data.categoryBreakdown.isNotEmpty()) {
                            item(key = "breakdown_title") {
                                Text(
                                    "Spending Breakdown", 
                                    style = MaterialTheme.typography.labelLarge, 
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            
                            // Spec 12: Performance - Stable keys and indexed colors
                            items(
                                data.categoryBreakdown,
                                key = { rank -> "${rank.name}_${rank.subcategoryId}_${rank.categoryId}" }
                            ) { rank ->
                                SpendingBreakdownItem(
                                    rank = rank,
                                    color = CHART_COLORS[data.categoryBreakdown.indexOf(rank) % CHART_COLORS.size],
                                    isPrivacyMode = isPrivacyMode
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
fun KPICard(
    label: String,
    value: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TotalAmountCard(
    type: ReportType,
    amount: Double,
    currencySymbol: String,
    isPrivacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val label = when(type) {
                    ReportType.EXPENSE -> "Total Expenses"
                    ReportType.INCOME -> "Total Income"
                    ReportType.CASH_FLOW -> "Net Cash Flow"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if(isPrivacyMode) "••••" else amount.formatCurrency(currencySymbol),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (type == ReportType.CASH_FLOW && amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ChartCard(
    reportType: ReportType,
    selectedViz: Int,
    categoryBreakdown: List<CategoryRank>,
    chartData: Map<Int, Double>,
    timeRange: TimeRange,
    isPrivacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (reportType == ReportType.CASH_FLOW) {
                CashFlowBarChartCompact(chartData)
            } else {
                AnimatedContent(
                    targetState = selectedViz,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "viz_transition"
                ) { viz ->
                    if (viz == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp), // Spec 1: Controlled height
                            contentAlignment = Alignment.Center
                        ) {
                            PieChartWithCallouts(categoryBreakdown, isPrivacyMode)
                        }
                    } else {
                        TrendLineGraphCompact(
                            data = chartData,
                            color = if (reportType == ReportType.INCOME) IncomeGreen else ExpenseRed,
                            timeRange = timeRange,
                            isPrivacyMode = isPrivacyMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PieChartWithCallouts(ranks: List<CategoryRank>, isPrivacyMode: Boolean = false) {
    // Spec 3: Filter out 0% categories for visualization
    val visibleRanks = remember(ranks) { ranks.filter { it.amount > 0 } }
    if (visibleRanks.isEmpty()) return
    
    val total = remember(visibleRanks) { visibleRanks.sumOf { it.amount } }
    val animationProgress = remember { Animatable(0f) }
    val onSurface = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(visibleRanks) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(800))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Spec 1: Substantially smaller donut radius. Diameter ~55% of height/minDimension
            val donutRadius = size.minDimension * 0.22f 
            val innerStrokeWidth = 14.dp.toPx()
            var startAngle = -90f
            
            val rightLabels = mutableListOf<LabelTarget>()
            val leftLabels = mutableListOf<LabelTarget>()

            visibleRanks.forEachIndexed { index, rank ->
                val sweepAngle = (rank.amount / total).toFloat() * 360f * animationProgress.value
                val color = CHART_COLORS[index % CHART_COLORS.size]
                
                // 2. Draw Donut Slices (Corrected: using explicit topLeft and size)
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - donutRadius, center.y - donutRadius),
                    size = Size(donutRadius * 2, donutRadius * 2),
                    style = Stroke(width = innerStrokeWidth, cap = StrokeCap.Butt)
                )
                
                // 3 & 4. Calculate Label Positions from actual angular midpoints
                if (animationProgress.value > 0.8f) {
                    val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                    val isRight = cos(midAngleRad) > 0
                    
                    val anchorPoint = Offset(
                        center.x + cos(midAngleRad).toFloat() * (donutRadius + 4.dp.toPx()),
                        center.y + sin(midAngleRad).toFloat() * (donutRadius + 4.dp.toPx())
                    )
                    
                    val target = LabelTarget(
                        rank = rank,
                        color = color,
                        midAngleRad = midAngleRad,
                        anchorPoint = anchorPoint,
                        yPos = anchorPoint.y
                    )
                    
                    if (isRight) rightLabels.add(target) else leftLabels.add(target)
                }
                
                startAngle += sweepAngle
            }

            // 6. Collision Avoidance - Vertical distribution
            distributeLabels(rightLabels, size.height)
            distributeLabels(leftLabels, size.height)

            val paint = android.graphics.Paint().apply {
                this.color = onSurface.toArgb()
                this.textSize = 18f
                this.isFakeBoldText = true
            }

            // 5. Draw Leader Lines and Labels
            (rightLabels + leftLabels).forEach { target ->
                val isRight = cos(target.midAngleRad) > 0
                val xEnd = if (isRight) size.width - 6.dp.toPx() else 6.dp.toPx()
                
                // Spec 5: Leader Lines originating from correct slice
                val elbowX = if (isRight) center.x + donutRadius + 24.dp.toPx() else center.x - donutRadius - 24.dp.toPx()
                
                // Leader Line Elbow
                drawLine(
                    color = target.color.copy(alpha = 0.4f),
                    start = target.anchorPoint,
                    end = Offset(elbowX, target.yPos),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = target.color.copy(alpha = 0.4f),
                    start = Offset(elbowX, target.yPos),
                    end = Offset(xEnd, target.yPos),
                    strokeWidth = 1.dp.toPx()
                )

                // Label Text (Spec 2)
                val labelText = if(isPrivacyMode) "${target.rank.icon} ••• ${target.rank.formattedPercentage}" 
                               else "${target.rank.icon} ${target.rank.name} · ${target.rank.formattedPercentage}"
                
                paint.textAlign = if (isRight) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
                
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    if (isRight) size.width - 8.dp.toPx() else 8.dp.toPx(),
                    target.yPos - 4.dp.toPx(),
                    paint
                )
            }
        }
        
        // Spec 8: Dynamic Center Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Top Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
            val topCategory = visibleRanks.firstOrNull()
            Text(
                topCategory?.let { if(isPrivacyMode) "••••" else it.name } ?: "None",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 60.dp)
            )
            if (topCategory?.subcategoryName != null && !isPrivacyMode) {
                Text(
                    text = topCategory.subcategoryName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private data class LabelTarget(
    val rank: CategoryRank,
    val color: Color,
    val midAngleRad: Double,
    val anchorPoint: Offset,
    var yPos: Float = 0f
)

private fun distributeLabels(targets: MutableList<LabelTarget>, canvasHeight: Float) {
    if (targets.isEmpty()) return
    
    // Sort by anchor point Y to maintain consistent ordering
    targets.sortBy { it.anchorPoint.y }
    
    val minGap = 24.dp.value * 1.5f 
    
    // Initial Y assignment
    targets.forEach { it.yPos = it.anchorPoint.y }

    // Iterative overlap correction
    repeat(15) {
        for (i in 0 until targets.size - 1) {
            val current = targets[i]
            val next = targets[i+1]
            if (next.yPos - current.yPos < minGap) {
                val overlap = minGap - (next.yPos - current.yPos)
                current.yPos -= overlap / 2f
                next.yPos += overlap / 2f
            }
        }
    }
    
    // Boundary check
    val padding = 20.dp.value
    targets.forEach {
        it.yPos = it.yPos.coerceIn(padding, canvasHeight - padding)
    }
}

@Composable
fun SpendingBreakdownItem(rank: CategoryRank, color: Color, isPrivacyMode: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(rank.icon, fontSize = 14.sp)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val nameDisplay = if(isPrivacyMode) "••••" else rank.name
                Text(text = nameDisplay, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Spec 12: Performance - Use pre-calculated formatted amount
                Text(if(isPrivacyMode) "••••" else rank.formattedAmount, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            if (rank.subcategoryName != null && !isPrivacyMode) {
                Text(rank.subcategoryName, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { rank.percentage },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = color,
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        }
        // Spec 12: Performance - Use pre-calculated formatted percentage
        Text(text = rank.formattedPercentage, modifier = Modifier.width(36.dp), textAlign = TextAlign.End, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TrendLineGraphCompact(data: Map<Int, Double>, color: Color, timeRange: TimeRange, isPrivacyMode: Boolean = false) {
    if (data.isEmpty()) return
    val values = data.values.toList()
    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val animationProgress = remember { Animatable(0f) }
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(1000))
    }

    Column(modifier = Modifier.height(160.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            val path = Path()
            val fillPath = Path()
            data.values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value.toFloat() / max.toFloat() * height * animationProgress.value)
                if (index == 0) { path.moveTo(x, y); fillPath.moveTo(x, height); fillPath.lineTo(x, y) }
                else { path.lineTo(x, y); path.lineTo(x, y) }
                if (index == data.size - 1) { fillPath.lineTo(x, height); fillPath.close() }
            }
            drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)))
            drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val xLabels = when (timeRange) {
                TimeRange.WEEKLY -> listOf("M", "T", "W", "T", "F", "S", "S")
                TimeRange.MONTHLY -> listOf("1", "10", "20", "30")
                TimeRange.YEARLY -> listOf("J", "M", "M", "J", "S", "N")
            }
            xLabels.forEach { label -> Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = onSurface) }
        }
    }
}

@Composable
fun CashFlowBarChartCompact(data: Map<Int, Double>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val max = data.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        data.values.forEach { amount ->
            val heightFactor = (amount.toFloat() / max.toFloat()).coerceAtMost(1f).coerceAtLeast(0.05f)
            Box(modifier = Modifier.weight(1f).fillMaxHeight(heightFactor).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        }
    }
}
