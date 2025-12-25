package com.yourname.moneypilot.ui.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.ContentCard
import com.yourname.moneypilot.ui.components.GradientBackground
import com.yourname.moneypilot.ui.features.dashboard.TransactionItem
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddTransaction: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val calendarState by viewModel.state.collectAsState()

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Text(
                            text = "${calendarState.currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${calendarState.currentMonth.year}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onMonthChange(calendarState.currentMonth.minusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onMonthChange(calendarState.currentMonth.plusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onAddTransaction(calendarState.selectedDate) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                // Calendar Grid area - partially showing gradient
                CalendarGrid(
                    currentMonth = calendarState.currentMonth,
                    dailySummaries = calendarState.dailySummaries,
                    selectedDate = calendarState.selectedDate,
                    onDateSelected = { viewModel.onDateSelected(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Layer 2 - Floating Content Card
                ContentCard(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Transactions for ${calendarState.selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (calendarState.selectedDateTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No records for this day",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(calendarState.selectedDateTransactions) { transaction ->
                                TransactionItem(transaction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    dailySummaries: Map<LocalDate, com.yourname.moneypilot.domain.usecase.transaction.DailySummary>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
    
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (day == "Sun" || day == "Sat") ExpenseRed.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        val totalGridCells = (daysInMonth + firstDayOfMonth + 6) / 7 * 7
        val gridItems = List(totalGridCells) { index ->
            val dayNumber = index - firstDayOfMonth + 1
            if (dayNumber in 1..daysInMonth) {
                currentMonth.atDay(dayNumber)
            } else {
                null
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(280.dp),
            userScrollEnabled = false
        ) {
            items(gridItems) { date ->
                if (date != null) {
                    CalendarCell(
                        date = date,
                        summary = dailySummaries[date],
                        isSelected = date == selectedDate,
                        isToday = date == LocalDate.now(),
                        onClick = { onDateSelected(date) }
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    date: LocalDate,
    summary: com.yourname.moneypilot.domain.usecase.transaction.DailySummary?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
            if (summary != null && !isSelected) {
                Row(horizontalArrangement = Arrangement.Center) {
                    if (summary.totalIncome > 0) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(IncomeGreen))
                    }
                    if (summary.totalExpense > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ExpenseRed))
                    }
                }
            }
        }
    }
}
