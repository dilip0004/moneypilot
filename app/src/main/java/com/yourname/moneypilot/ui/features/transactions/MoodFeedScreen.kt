package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.util.formatCompact
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

// --- Mood Palettes ---
private object MoodPalette {
    val Bullish = Color(0xFF00382E) to Color(0xFF001A15)
    val Neutral = Color(0xFF332000) to Color(0xFF1A1400)
    val Bearish = Color(0xFF380000) to Color(0xFF1A0000)

    val BullishAccent = Color(0xFF00E676)
    val NeutralAccent = Color(0xFFFFD600)
    val BearishAccent = Color(0xFFFF1744)
}

@Composable
fun MoodFeedScreen(
    currentMonth: YearMonth,
    onEditTransaction: (String) -> Unit,
    viewModel: MoodFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = rememberCurrencySymbol()

    LaunchedEffect(currentMonth) {
        viewModel.updateMonth(currentMonth)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScreenState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ScreenState.Success -> {
                MoodFeedContent(
                    data = state.data,
                    currencySymbol = currencySymbol,
                    onEditTransaction = onEditTransaction
                )
            }
            is ScreenState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions for this month", color = Color.White.copy(alpha = 0.6f))
                }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodFeedContent(
    data: MoodFeedState,
    currencySymbol: String,
    onEditTransaction: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { data.days.size })
    
    // Calculate interpolated background color
    val backgroundColor = remember(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        val currentDay = data.days.getOrNull(pagerState.currentPage)
        val nextDay = data.days.getOrNull(pagerState.currentPage + 1)
        
        val startColor = getMoodPrimaryColor(currentDay?.mood ?: FinancialMood.NEUTRAL)
        val endColor = getMoodPrimaryColor(nextDay?.mood ?: currentDay?.mood ?: FinancialMood.NEUTRAL)
        
        lerp(startColor, endColor, pagerState.currentPageOffsetFraction)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Dynamic Dynamic Background
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor))
        
        // Ambient Layers
        GridLayer()
        ParticleLayer()

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val dayData = data.days[page]
            val isSelected = pagerState.currentPage == page
            
            MoodDayPage(
                dayData = dayData,
                isSelected = isSelected,
                currencySymbol = currencySymbol,
                onEditTransaction = onEditTransaction
            )
        }
    }
}

@Composable
fun MoodDayPage(
    dayData: DayMoodData,
    isSelected: Boolean,
    currencySymbol: String,
    onEditTransaction: (String) -> Unit
) {
    val moodColor = getMoodAccentColor(dayData.mood)
    val weatherIcon = when (dayData.mood) {
        FinancialMood.BULLISH -> "☀️"
        FinancialMood.NEUTRAL -> "⛅"
        FinancialMood.BEARISH -> "🌧️"
    }

    // Number Animation
    val animatedBalance = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            animatedBalance.snapTo(0f)
            animatedBalance.animateTo(
                targetValue = dayData.netBalance.toFloat(),
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Watermark
        Text(
            text = dayData.netBalance.let { 
                val valK = abs(it) / 1000
                val sign = if (it >= 0) "+" else "−"
                "$sign${valK.toInt()}K"
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp)
                .alpha(0.05f)
                .graphicsLayer(scaleX = 4f, scaleY = 4f),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = moodColor,
            maxLines = 1
        )

        // Weather Icon (Upper Right)
        Text(
            text = weatherIcon,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 24.dp)
                .graphicsLayer {
                    val float = (System.currentTimeMillis() % 2000) / 2000f
                    translationY = (kotlin.math.sin(float * 2 * Math.PI) * 10).toFloat()
                },
            fontSize = 48.sp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Header
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -20 }
            ) {
                Column {
                    Text(
                        text = dayData.dayName,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "· ${dayData.formattedDate}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (dayData.isToday) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = moodColor,
                            letterSpacing = 2.sp
                        )
                    }
                    Text(
                        text = "📋 ${dayData.transactionCount} transactions",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Hero Net Balance
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(tween(500, delayMillis = 200)) + fadeIn(tween(500, delayMillis = 200))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sign = if (dayData.netBalance >= 0) "+" else "−"
                    Text(
                        text = sign + abs(animatedBalance.value).toDouble().formatCurrency(currencySymbol),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = moodColor,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "NET BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    
                    val moodDesc = when(dayData.mood) {
                        FinancialMood.BULLISH -> "Strong positive day"
                        FinancialMood.BEARISH -> "Spending-heavy day"
                        FinancialMood.NEUTRAL -> "Balanced financial day"
                    }
                    Text(
                        text = moodDesc,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Transaction Cards
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(dayData.transactions, key = { _, tx -> tx.transaction.id }) { index, tx ->
                    var itemVisible by remember { mutableStateOf(!isSelected) }
                    LaunchedEffect(isSelected) {
                        if (isSelected) {
                            kotlinx.coroutines.delay(400L + (index * 100L))
                            itemVisible = true
                        } else {
                            itemVisible = false
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { 20 }
                    ) {
                        MoodTransactionCard(
                            txWithDetails = tx,
                            currencySymbol = currencySymbol,
                            index = index,
                            onClick = { onEditTransaction(tx.transaction.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun MoodTransactionCard(
    txWithDetails: TransactionWithDetails,
    currencySymbol: String,
    index: Int,
    onClick: () -> Unit
) {
    val tx = txWithDetails.transaction
    val financeColors = LocalFinanceColors.current
    
    // Subtle Asymmetry
    val xOffset = when (index % 3) {
        1 -> 4.dp
        2 -> (-4.dp)
        else -> 0.dp
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .offset(x = xOffset),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121212), // Solid dark
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = txWithDetails.category?.icon ?: "❓", fontSize = 24.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val title = tx.note?.takeIf { it.isNotBlank() } ?: txWithDetails.category?.name ?: "Uncategorized"
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val subtitle = txWithDetails.walletFrom?.name ?: "Wallet"
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            val isIncome = tx.type == TransactionType.Income
            val amountColor = if (isIncome) financeColors.income else financeColors.expense
            val sign = if (isIncome) "+" else "−"
            
            Text(
                text = "$sign${tx.amount.formatCurrency(currencySymbol)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = amountColor
            )
        }
    }
}

@Composable
private fun GridLayer() {
    val infiniteTransition = rememberInfiniteTransition(label = "grid")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.03f)) {
        val step = 40.dp.toPx()
        val thickness = 1.dp.toPx()
        
        // Vertical lines
        var x = (gridOffset % step)
        while (x < size.width) {
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), thickness)
            x += step
        }
        
        // Horizontal lines
        var y = (gridOffset % step)
        while (y < size.height) {
            drawLine(Color.White, Offset(0f, y), Offset(size.width, y), thickness)
            y += step
        }
    }
}

@Composable
private fun ParticleLayer() {
    val particles = remember {
        List(20) {
            MutableParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.001f + 0.0005f
            )
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "anim"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
        particles.forEach { p ->
            val currentY = (p.y - animState * p.speed * 1000) % 1f
            val displayY = if (currentY < 0) currentY + 1f else currentY
            
            drawCircle(
                color = Color.White,
                radius = p.size.dp.toPx(),
                center = Offset(p.x * size.width, displayY * size.height),
                alpha = (0.3f * (1f - displayY)).coerceIn(0f, 1f)
            )
        }
    }
}

private class MutableParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float
)

private fun getMoodPrimaryColor(mood: FinancialMood) = when (mood) {
    FinancialMood.BULLISH -> MoodPalette.Bullish.first
    FinancialMood.NEUTRAL -> MoodPalette.Neutral.first
    FinancialMood.BEARISH -> MoodPalette.Bearish.first
}

private fun getMoodAccentColor(mood: FinancialMood) = when (mood) {
    FinancialMood.BULLISH -> MoodPalette.BullishAccent
    FinancialMood.NEUTRAL -> MoodPalette.NeutralAccent
    FinancialMood.BEARISH -> MoodPalette.BearishAccent
}
