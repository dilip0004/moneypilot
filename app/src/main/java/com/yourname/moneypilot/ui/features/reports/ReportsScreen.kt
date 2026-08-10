package com.yourname.moneypilot.ui.features.reports

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
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
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Time Range Selector
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TimeRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = reportState.timeRange == range,
                                onClick = { viewModel.onTimeRangeChange(range) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeRange.entries.size)
                            ) {
                                val label = range.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }

                    // Report Type Selector (Compact)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(40.dp)
                    ) {
                        ReportType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = reportState.reportType == type,
                                onClick = { viewModel.onReportTypeChange(type) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ReportType.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                val label = when(type) {
                                    ReportType.EXPENSE -> "Expense"
                                    ReportType.INCOME -> "Income"
                                    ReportType.CASH_FLOW -> "Cash Flow"
                                }
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
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

                        // Adaptive Insights Section
                        if (data.showSpendingInsights || data.showBurnRateAlerts || data.showCategoryAlerts) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (data.showBurnRateAlerts && data.keyAnalytics.savingsRate != null && data.keyAnalytics.savingsRate < 0.1f) {
                                        AnomalySection(
                                            title = "Burn Rate Alert",
                                            message = "You've consumed 90%+ of your income.",
                                            icon = Icons.Default.WarningAmber,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    
                                    if (data.showSpendingInsights && data.reflectionPrompts.isNotEmpty()) {
                                        data.reflectionPrompts.take(1).forEach { prompt ->
                                            InsightCard(
                                                title = "Spending Insight",
                                                message = prompt,
                                                icon = Icons.Default.Info,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InsightTile(
                                    label = "Savings Rate",
                                    value = if(isPrivacyMode) "••%" else "${(data.keyAnalytics.savingsRate?.times(100))?.toInt() ?: 0}%",
                                    subLabel = if(data.keyAnalytics.savingsRate == null) "No Income" else "Saved",
                                    modifier = Modifier.weight(1f)
                                )
                                InsightTile(
                                    label = "Daily Average",
                                    value = if(isPrivacyMode) "••••" else currencySymbol + (data.keyAnalytics.dailyAverage?.toInt() ?: 0),
                                    subLabel = "per day",
                                    modifier = Modifier.weight(1f)
                                )
                                InsightTile(
                                    label = "Transactions",
                                    value = "${data.keyAnalytics.transactionCount}",
                                    subLabel = "Entries",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            MainAmountDisplay(
                                type = data.reportType,
                                amount = data.totalAmount,
                                currencySymbol = currencySymbol,
                                isPrivacyMode = isPrivacyMode
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                var selectedViz by remember { mutableIntStateOf(0) }
                                
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (data.reportType == ReportType.CASH_FLOW) {
                                        CashFlowBarChartCompact(data.chartData)
                                    } else {
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier.width(240.dp).height(32.dp)
                                        ) {
                                            TabLikeSegmentedButton(
                                                selected = selectedViz == 0,
                                                onClick = { selectedViz = 0 },
                                                label = "Distribution",
                                                index = 0,
                                                count = 2
                                            )
                                            TabLikeSegmentedButton(
                                                selected = selectedViz == 1,
                                                onClick = { selectedViz = 1 },
                                                label = "Trend",
                                                index = 1,
                                                count = 2
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(24.dp))
                                        
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(200.dp), 
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedContent(
                                                targetState = selectedViz,
                                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                                label = "viz_transition"
                                            ) { viz ->
                                                if (viz == 0) {
                                                    PieChartLabeled(data.categoryBreakdown, isPrivacyMode)
                                                } else {
                                                    TrendLineGraphCompact(
                                                        data = data.chartData,
                                                        color = if (data.reportType == ReportType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                        timeRange = data.timeRange,
                                                        isPrivacyMode = isPrivacyMode
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (data.reportType != ReportType.CASH_FLOW && data.categoryBreakdown.isNotEmpty()) {
                            itemsIndexed(
                                items = data.categoryBreakdown,
                                key = { index, rank -> "${rank.name}_$index" }
                            ) { index, rank ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(index * MotionConstants.StaggerDelay.toLong())
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = slideInVertically { 10 } + fadeIn()
                                ) {
                                    CategoryRankItemCompact(
                                        rank = rank,
                                        categoryColor = CHART_COLORS[index % CHART_COLORS.size],
                                        isPrivacyMode = isPrivacyMode,
                                        currencySymbol = currencySymbol
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAmountDisplay(
    type: ReportType,
    amount: Double,
    currencySymbol: String,
    isPrivacyMode: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val label = when(type) {
            ReportType.EXPENSE -> "Total Expenses"
            ReportType.INCOME -> "Total Income"
            ReportType.CASH_FLOW -> "Net Cash Flow"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if(isPrivacyMode) "••••" else amount.formatCurrency(currencySymbol),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = if (type == ReportType.CASH_FLOW && amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChoiceSegmentedButtonRowScope.TabLikeSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    index: Int,
    count: Int
) {
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = SegmentedButtonDefaults.itemShape(index = index, count = count)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnomalySection(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun InsightCard(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun InsightTile(label: String, value: String, subLabel: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subLabel, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DateNavigatorCompact(date: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, range: TimeRange, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.KeyboardArrowLeft, null) }
        val label = when (range) {
            TimeRange.WEEKLY -> {
                val formatter = DateTimeFormatter.ofPattern("dd MMM")
                "${rangeStart.format(formatter)} - ${rangeEnd.format(formatter)}"
            }
            TimeRange.MONTHLY -> "${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.year}"
            TimeRange.YEARLY -> "${date.year}"
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.KeyboardArrowRight, null) }
    }
}

@Composable
fun PieChartLabeled(ranks: List<CategoryRank>, isPrivacyMode: Boolean = false) {
    val total = ranks.sumOf { it.amount }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(ranks) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(1000))
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.size(160.dp)) {
            var startAngle = -90f
            
            ranks.forEachIndexed { index, rank ->
                if (total > 0) {
                    val sweepAngle = (rank.amount / total).toFloat() * 360f * animationProgress.value
                    val color = CHART_COLORS[index % CHART_COLORS.size]
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt)
                    )
                    
                    startAngle += sweepAngle
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Top Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                ranks.firstOrNull()?.let { if(isPrivacyMode) "••••" else it.name } ?: "None",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
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

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            
            val path = Path()
            val fillPath = Path()
            
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
                    colors = listOf(color.copy(alpha = 0.2f), Color.Transparent)
                )
            )
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val xLabels = when (timeRange) {
                TimeRange.WEEKLY -> listOf("M", "T", "W", "T", "F", "S", "S")
                TimeRange.MONTHLY -> listOf("1", "10", "20", "30")
                TimeRange.YEARLY -> listOf("J", "M", "M", "J", "S", "N")
            }
            xLabels.forEach { label ->
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = onSurface)
            }
        }
    }
}

@Composable
fun CategoryRankItemCompact(rank: CategoryRank, categoryColor: Color, isPrivacyMode: Boolean = false, currencySymbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = categoryColor.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(rank.icon, fontSize = 14.sp)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val nameDisplay = if(isPrivacyMode) "••••" else rank.name
                Text(
                    text = nameDisplay, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val amountText = if(isPrivacyMode) "••••" else rank.amount.formatCompact(currencySymbol)
                Text(amountText, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { rank.percentage },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = categoryColor,
                strokeCap = StrokeCap.Round,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Text(
            text = "${(rank.percentage * 100).toInt()}%",
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CashFlowBarChartCompact(data: Map<Int, Double>) {
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
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
