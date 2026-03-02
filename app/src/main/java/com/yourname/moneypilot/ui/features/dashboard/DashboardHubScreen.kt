package com.yourname.moneypilot.ui.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.features.calendar.CalendarScreen
import com.yourname.moneypilot.ui.features.transactions.TransactionsScreen
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHubScreen(
    onAddTransaction: (LocalDate) -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardHubViewModel = hiltViewModel()
) {
    val hubState by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Daily", "Calendar", "Monthly", "Total", "Note")

    val TAG_TAB_DAILY = "tx_tab_daily"
    val TAG_TAB_CALENDAR = "tx_tab_calendar"
    val TAG_TAB_MONTHLY = "tx_tab_monthly"
    val TAG_TAB_TOTAL = "tx_tab_total"
    val TAG_TAB_NOTE = "tx_tab_note"
    val TAG_FAB_ADD = "tx_fab_add"

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { viewModel.onMonthChange(hubState.currentMonth.minusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                            }
                            Text(
                                text = "${hubState.currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${hubState.currentMonth.year}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.onMonthChange(hubState.currentMonth.plusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                            }
                        }

                        IconButton(onClick = { /* Search logic */ }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        edgePadding = 8.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    height = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible
                                    ) 
                                },
                                modifier = Modifier.testTag(
                                    when (title.lowercase(Locale.getDefault())) {
                                        "daily" -> "tab_daily"
                                        "calendar" -> "tab_calendar"
                                        "monthly" -> "tab_monthly"
                                        "total" -> "tab_total"
                                        "note" -> "tab_note"
                                        else -> "tab_${title.lowercase(Locale.getDefault())}"
                                    }
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryItem(label = "Income", value = "₹ ${hubState.monthlyIncome}", color = MaterialTheme.colorScheme.primary)
                        SummaryItem(label = "Expenses", value = "₹ ${hubState.monthlyExpense}", color = MaterialTheme.colorScheme.error)
                        SummaryItem(label = "Total", value = "₹ ${hubState.monthlyIncome - hubState.monthlyExpense}", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransaction(LocalDate.now()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
                ,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTabIndex) {
                0 -> {
                    TransactionsScreen(
                        showSearchBar = false,
                        onAddTransaction = { onAddTransaction(LocalDate.now()) },
                        onEditTransaction = onEditTransaction
                    )
                }
                1 -> {
                    CalendarScreen(
                        currentMonth = hubState.currentMonth,
                        onAddTransaction = onAddTransaction
                    )
                }
                2 -> MonthlySummaryTab(hubState)
                3 -> TotalNetWorthTab(hubState)
                4 -> NotesTab()
            }
        }
    }
}

@Composable
fun MonthlySummaryTab(state: DashboardHubState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Monthly Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cash Flow", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow("Total Income", "₹ ${state.monthlyIncome}", MaterialTheme.colorScheme.primary)
                    FlowRow("Total Expense", "₹ ${state.monthlyExpense}", MaterialTheme.colorScheme.error)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    FlowRow("Net Surplus", "₹ ${state.monthlyIncome - state.monthlyExpense}", MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun TotalNetWorthTab(state: DashboardHubState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Net Worth", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("₹ ${state.totalBalance}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { Text("Your Wallets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        
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
                    Text("₹ ${wallet.currentBalance}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun NotesTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
            Text("Notes & Tags View", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Search by memos coming soon", style = MaterialTheme.typography.bodySmall)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
