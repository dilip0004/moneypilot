package com.yourname.moneypilot.ui.features.dashboard

import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXForward
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXBackward
import com.yourname.moneypilot.ui.theme.motion.motionTween
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.features.calendar.CalendarScreen
import com.yourname.moneypilot.ui.features.transactions.TransactionsScreen
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.util.rememberCurrencySymbol
import java.time.LocalDate
import java.time.format.TextStyle
import androidx.compose.ui.graphics.Brush
import com.yourname.moneypilot.ui.components.AppDateNavigator
import com.yourname.moneypilot.ui.components.MoneyPilotSegmentedControl
import java.util.Locale
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHubScreen(
    onAddTransaction: (LocalDate) -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardHubViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val hubState by viewModel.state.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    
    val financeColors = LocalFinanceColors.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Daily", "Calendar", "Monthly", "Yearly", "Total")
    val currencySymbol = rememberCurrencySymbol()

    val isYearlyTab = selectedTabIndex == 3

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    val headerText = if (isYearlyTab) {
                        hubState.currentYear.toString()
                    } else {
                        "${hubState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${hubState.currentMonth.year}"
                    }

                    AppDateNavigator(
                        label = headerText,
                        onPrev = {
                            if (isYearlyTab) viewModel.onYearChange(hubState.currentYear.minusYears(1))
                            else viewModel.onMonthChange(hubState.currentMonth.minusMonths(1))
                        },
                        onNext = {
                            if (isYearlyTab) viewModel.onYearChange(hubState.currentYear.plusYears(1))
                            else viewModel.onMonthChange(hubState.currentMonth.plusMonths(1))
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        actions = {
                            IconButton(onClick = { /* Search logic */ }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(22.dp))
                            }
                        }
                    )

                    MoneyPilotSegmentedControl(
                        options = tabs,
                        selectedOption = tabs[selectedTabIndex],
                        onOptionSelected = { selectedTabIndex = tabs.indexOf(it) },
                        labelExtractor = { it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        height = 36.dp,
                        showIcon = false
                    )

                    val income = if (isYearlyTab) hubState.yearlyIncome else hubState.monthlyIncome
                    val expense = if (isYearlyTab) hubState.yearlyExpense else hubState.monthlyExpense
                    val net = income - expense
                    val savingsRate = if (income > 0) (net / income) * 100 else 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, top = 2.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryItem(label = "Inflow", value = if(isPrivacyMode) "••••" else currencySymbol + income.toInt(), color = financeColors.income)
                        SummaryItem(label = "Outflow", value = if(isPrivacyMode) "••••" else currencySymbol + expense.toInt(), color = financeColors.expense)
                        SummaryItem(label = "Net", value = if(isPrivacyMode) "••••" else currencySymbol + net.toInt(), color = if (net >= 0) financeColors.income else financeColors.expense)
                        SummaryItem(
                            label = "Saved", 
                            value = if(isPrivacyMode) "••%" else "${savingsRate.toInt()}%", 
                            color = if (savingsRate >= 20) financeColors.income else if (savingsRate > 0) Color(0xFFFFA500) else financeColors.expense
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            val primary = MaterialTheme.colorScheme.primary
            val gradient = Brush.linearGradient(
                colors = listOf(primary, primary.copy(alpha = 0.8f))
            )
            
            FloatingActionButton(
                onClick = { onAddTransaction(hubState.selectedDate) },
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("fab_add_transaction")
                    .padding(bottom = 12.dp) // Space from bottom nav
                    .background(gradient, CircleShape)
                    .size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) // Handle elevation on container if needed or just use default
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        SharedAxisXForward
                    } else {
                        SharedAxisXBackward
                    }.using(SizeTransform(clip = false))
                },
                label = "hub_tab_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> TransactionsScreen(
                        currentMonth = hubState.currentMonth,
                        showSearchBar = false,
                        onAddTransaction = { onAddTransaction(hubState.selectedDate) },
                        onEditTransaction = onEditTransaction,
                        isPrivacyMode = isPrivacyMode
                    )
                    1 -> CalendarScreen(
                        currentMonth = hubState.currentMonth, 
                        selectedDateOverride = hubState.selectedDate,
                        onDateSelected = { viewModel.onDateSelected(it) },
                        onAddTransaction = onAddTransaction,
                        isPrivacyMode = isPrivacyMode
                    )
                    2 -> MonthlySummaryTab(hubState, currencySymbol, isPrivacyMode)
                    3 -> YearlySummaryTab(hubState, currencySymbol, isPrivacyMode)
                    4 -> TotalNetWorthTab(hubState, currencySymbol, isPrivacyMode)
                }
            }
        }
    }
}

@Composable
fun TotalNetWorthTab(state: DashboardHubState, currencySymbol: String, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Net Worth Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Net Worth", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = if(isPrivacyMode) "••••" else "$currencySymbol ${state.totalNetWorth.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.totalNetWorth >= 0) financeColors.income else financeColors.expense
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Assets", style = MaterialTheme.typography.labelSmall, color = financeColors.income)
                            Text(
                                if(isPrivacyMode) "••••" else "$currencySymbol ${state.totalAssets.toInt()}", 
                                fontWeight = FontWeight.Bold,
                                color = financeColors.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Liabilities", style = MaterialTheme.typography.labelSmall, color = financeColors.expense)
                            Text(
                                if(isPrivacyMode) "••••" else "$currencySymbol ${state.totalLiabilities.toInt()}", 
                                fontWeight = FontWeight.Bold,
                                color = financeColors.expense
                            )
                        }
                    }
                }
            }
        }

        item { Text("Wallet Assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }

        items(state.wallets) { wallet ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(wallet.color).copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(wallet.icon, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(wallet.name, fontWeight = FontWeight.Bold)
                            Text(wallet.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val isLiability = wallet.type == "CREDIT" || wallet.type == "CREDIT_CARD"
                    Text(
                        if(isPrivacyMode) "••••" else "$currencySymbol ${wallet.currentBalance}", 
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (isLiability) financeColors.expense else financeColors.income
                    )
                }
            }
        }
    }
}

@Composable
fun YearlySummaryTab(state: DashboardHubState, currencySymbol: String, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Yearly Performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "Yearly Income",
                    value = if(isPrivacyMode) "••••" else "$currencySymbol ${state.yearlyIncome.toInt()}",
                    color = financeColors.income,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Yearly Expense",
                    value = if(isPrivacyMode) "••••" else "$currencySymbol ${state.yearlyExpense.toInt()}",
                    color = financeColors.expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val net = state.yearlyIncome - state.yearlyExpense
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Financial Summary (${state.currentYear})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow("Net Savings", if(isPrivacyMode) "••••" else "$currencySymbol $net", if (net >= 0) financeColors.income else financeColors.expense)
                    val savingsRate = if (state.yearlyIncome > 0) (net / state.yearlyIncome) * 100 else 0.0
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow("Avg. Savings Rate", "${String.format("%.1f", savingsRate)}%", if (savingsRate >= 20) financeColors.income else Color(0xFFFFA500))
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryTab(state: DashboardHubState, currencySymbol: String, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Monthly Health", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "Savings Rate",
                    value = "${String.format("%.1f", state.savingsRate)}%",
                    color = if (state.savingsRate >= 20) financeColors.income else if (state.savingsRate > 0) Color(0xFFFFA500) else financeColors.expense,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Net Surplus",
                    value = if(isPrivacyMode) "••••" else "$currencySymbol ${state.netSurplus.toInt()}",
                    color = if (state.netSurplus >= 0) financeColors.income else financeColors.expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cash Flow Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow("Total Inflow", if(isPrivacyMode) "••••" else "$currencySymbol ${state.monthlyIncome}", financeColors.income)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow("Total Outflow", if(isPrivacyMode) "••••" else "$currencySymbol ${state.monthlyExpense}", financeColors.expense)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    FlowRow("Net Result", if(isPrivacyMode) "••••" else "$currencySymbol ${state.netSurplus}", if (state.netSurplus >= 0) financeColors.income else financeColors.expense)
                }
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(MotionConstants.DurationButton),
        label = "metric_card_scale"
    )

    Card(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(interactionSource = interactionSource, indication = null) { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun FlowRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = color,
            fontSize = 13.sp
        )
    }
}
