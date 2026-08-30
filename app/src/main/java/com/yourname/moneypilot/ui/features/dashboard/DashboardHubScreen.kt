package com.yourname.moneypilot.ui.features.dashboard

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.components.*
import com.yourname.moneypilot.ui.features.calendar.CalendarScreen
import com.yourname.moneypilot.ui.features.transactions.TransactionsScreen
import com.yourname.moneypilot.ui.features.transactions.MoodFeedScreen
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXBackward
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXForward
import com.yourname.moneypilot.ui.theme.motion.motionTween
import com.yourname.moneypilot.util.rememberCurrencySymbol
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Mood Feed", "Daily", "Calendar", "Monthly", "Yearly", "Total")
    val currencySymbol = rememberCurrencySymbol()

    val isYearlyTab = selectedTabIndex == 4

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            Column {
                // Top Bar with Navigation (Hidden for Mood Feed per requirements)
                if (selectedTabIndex != 0) {
                    GlassTopBar(
                        title = {
                            val headerText = if (isYearlyTab) {
                                hubState.currentYear.toString()
                            } else {
                                "${hubState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${hubState.currentMonth.year}"
                            }
                            Text(headerText, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isYearlyTab) viewModel.onYearChange(hubState.currentYear.minusYears(1))
                                else viewModel.onMonthChange(hubState.currentMonth.minusMonths(1))
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                if (isYearlyTab) viewModel.onYearChange(hubState.currentYear.plusYears(1))
                                else viewModel.onMonthChange(hubState.currentMonth.plusMonths(1))
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            }
                            IconButton(onClick = { /* Search logic */ }) {
                                Icon(Icons.Default.Search, null)
                            }
                        }
                    )
                } else {
                    // Spacer for status bar when TopBar is hidden in Mood Feed
                    Spacer(Modifier.statusBarsPadding())
                }

                // Transactions Sub-Navigation - ALWAYS VISIBLE
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 2.dp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                ) 
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex != 0) { // No FAB on Mood Feed
                MoneyPilotFAB(
                    onClick = { onAddTransaction(hubState.selectedDate) },
                    icon = Icons.Default.Add,
                    label = "Add Record",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "hub_tab_transition"
            ) { targetIndex ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (targetIndex > 2) { // 3, 4, 5 (Monthly, Yearly, Total)
                        val income = if (isYearlyTab) hubState.yearlyIncome else hubState.monthlyIncome
                        val expense = if (isYearlyTab) hubState.yearlyExpense else hubState.monthlyExpense
                        val net = income - expense
                        val savingsRate = if (income > 0) (net / income).toFloat() else 0f

                        FinancialSummarySurface(
                            title = if (isYearlyTab) "Yearly Summary" else "Monthly Summary",
                            primaryValue = if(isPrivacyMode) "••••" else currencySymbol + net.toInt(),
                            progress = savingsRate,
                            progressColor = if (savingsRate >= 0.2f) financeColors.income else if (savingsRate > 0) Color(0xFFFFA500) else financeColors.expense,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            secondaryInfo = {
                                SummaryItem(label = "Inflow", value = if(isPrivacyMode) "••••" else currencySymbol + income.toInt(), color = financeColors.income)
                                SummaryItem(label = "Outflow", value = if(isPrivacyMode) "••••" else currencySymbol + expense.toInt(), color = financeColors.expense)
                                SummaryItem(
                                    label = "Saved", 
                                    value = if(isPrivacyMode) "••%" else "${(savingsRate * 100).toInt()}%", 
                                    color = if (savingsRate >= 0.2f) financeColors.income else if (savingsRate > 0) Color(0xFFFFA500) else financeColors.expense
                                )
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (targetIndex) {
                            0 -> MoodFeedScreen(
                                currentMonth = hubState.currentMonth,
                                onEditTransaction = onEditTransaction
                            )
                            1 -> TransactionsScreen(
                                currentMonth = hubState.currentMonth,
                                showSearchBar = false,
                                onAddTransaction = { onAddTransaction(hubState.selectedDate) },
                                onEditTransaction = onEditTransaction,
                                isPrivacyMode = isPrivacyMode
                            )
                            2 -> CalendarScreen(
                                currentMonth = hubState.currentMonth, 
                                selectedDateOverride = hubState.selectedDate,
                                onDateSelected = { viewModel.onDateSelected(it) },
                                onAddTransaction = onAddTransaction,
                                isPrivacyMode = isPrivacyMode
                            )
                            3 -> MonthlySummaryTab(hubState, currencySymbol, isPrivacyMode)
                            4 -> YearlySummaryTab(hubState, currencySymbol, isPrivacyMode)
                            5 -> TotalNetWorthTab(hubState, currencySymbol, isPrivacyMode)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TotalNetWorthTab(state: DashboardHubState, currencySymbol: String, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        item {
            FinancialSummarySurface(
                title = "Total Net Worth",
                primaryValue = if(isPrivacyMode) "••••" else "$currencySymbol ${state.totalNetWorth.toInt()}",
                progress = if(state.totalAssets > 0) (state.totalNetWorth / state.totalAssets).toFloat() else 0f,
                progressColor = if (state.totalNetWorth >= 0) financeColors.income else financeColors.expense,
                secondaryInfo = {
                    SummaryItem(label = "Assets", value = if(isPrivacyMode) "••••" else "$currencySymbol ${state.totalAssets.toInt()}", color = financeColors.income)
                    SummaryItem(label = "Liabilities", value = if(isPrivacyMode) "••••" else "$currencySymbol ${state.totalLiabilities.toInt()}", color = financeColors.expense)
                }
            )
        }

        item { SectionHeader("Wallet Assets") }

        items(state.wallets, key = { it.id }) { wallet ->
            val isLiability = wallet.type == "CREDIT" || wallet.type == "CREDIT_CARD"
            MoneyPilotListItem(
                icon = wallet.icon,
                title = wallet.name,
                subtitle = wallet.type,
                statusColor = if (isLiability) financeColors.expense else financeColors.income,
                onClick = { /* Navigate to wallet statement */ },
                trailingContent = {
                    Text(
                        text = if(isPrivacyMode) "••••" else "$currencySymbol ${wallet.currentBalance.toInt()}", 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black, 
                        color = if (isLiability) financeColors.expense else financeColors.income
                    )
                }
            )
        }
    }
}

@Composable
fun YearlySummaryTab(state: DashboardHubState, currencySymbol: String, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
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
            MoneyPilotSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Financial Summary (${state.currentYear})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow("Net Savings", if(isPrivacyMode) "••••" else "$currencySymbol $net", if (net >= 0) financeColors.income else financeColors.expense)
                    val savingsRate = if (state.yearlyIncome > 0) (net / state.yearlyIncome) * 100 else 0.0
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow("Avg. Savings Rate", "${String.format(Locale.getDefault(), "%.1f", savingsRate)}%", if (savingsRate >= 20) financeColors.income else Color(0xFFFFA500))
                }
            }
        }
    }
}

@Composable
fun MonthlySummaryTab(state: DashboardHubState, currencySymbol: String, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    label = "Savings Rate",
                    value = "${String.format(Locale.getDefault(), "%.1f", state.savingsRate)}%",
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
            MoneyPilotSurface(modifier = Modifier.fillMaxWidth()) {
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

    GlassSurface(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(interactionSource = interactionSource, indication = null) { },
        opacity = GlassLevel.High,
        shape = RoundedCornerShape(16.dp),
        tint = Color.Black
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun FlowRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.9f))
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}
