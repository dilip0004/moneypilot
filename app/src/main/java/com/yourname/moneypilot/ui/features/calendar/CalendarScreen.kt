package com.yourname.moneypilot.ui.features.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.CompactTransactionItem
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    currentMonth: YearMonth,
    onAddTransaction: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val calendarState by viewModel.state.collectAsState()

    LaunchedEffect(currentMonth) {
        viewModel.onMonthChange(currentMonth)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // REMOVED: Top Monthly TotalsRow (Redundant with Dashboard Hub)

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CalendarGrid(
                        currentMonth = calendarState.currentMonth,
                        dailySummaries = calendarState.dailySummaries,
                        selectedDate = calendarState.selectedDate,
                        onDateSelected = { viewModel.onDateSelected(it) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = calendarState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Daily totals for selected date
        item {
            val selSummary = calendarState.dailySummaries[calendarState.selectedDate]
            val income = selSummary?.totalIncome ?: 0.0
            val expense = selSummary?.totalExpense ?: 0.0
            val total = income - expense
            TotalsRow(income = income, expense = expense, total = total)
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        if (calendarState.selectedDateTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records for this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(calendarState.selectedDateTransactions) { transactionWithDetails ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        CompactTransactionItem(txWithDetails = transactionWithDetails, timePattern = "h:mm a")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
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
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
    
    val maxMonthExpense = remember(dailySummaries) {
        dailySummaries.values.maxOfOrNull { it.totalExpense }?.takeIf { it > 0.0 } ?: 1.0
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val totalGridCells = (daysInMonth + firstDayOfMonth + 6) / 7 * 7
        val gridItems = List(totalGridCells) { index ->
            val dayNumber = index - firstDayOfMonth + 1
            if (dayNumber in 1..daysInMonth) currentMonth.atDay(dayNumber) else null
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(240.dp),
            userScrollEnabled = false
        ) {
            items(gridItems) { date ->
                if (date != null) {
                    CalendarCell(
                        date = date,
                        summary = dailySummaries[date],
                        maxMonthExpense = maxMonthExpense,
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
    maxMonthExpense: Double,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1.0f, label = "cell_scale")
    
    val intensity = remember(summary, maxMonthExpense) {
        if (summary != null && summary.totalExpense > 0) {
            (summary.totalExpense / maxMonthExpense).coerceIn(0.0, 1.0).toFloat()
        } else 0f
    }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .scale(scale)
            .then(
                if (isSelected) Modifier.shadow(4.dp, CircleShape) else Modifier
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (intensity > 0 && !isSelected) {
                    Modifier.border(
                        BorderStroke(
                            width = (1.dp + (3.dp * intensity)),
                            color = financeColors.expense.copy(alpha = 0.2f + (0.6f * intensity))
                        ),
                        CircleShape
                    )
                } else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = contentColor
            )
            
            if (summary != null && !isSelected) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (summary.totalIncome > 0) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(financeColors.income))
                    }
                }
            }
        }
    }
}

@Composable
fun TotalsRow(income: Double, expense: Double, total: Double) {
    val financeColors = LocalFinanceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), 
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "In: ₹${income.toInt()}", style = MaterialTheme.typography.labelMedium, color = financeColors.income)
        Text(text = "Out: ₹${expense.toInt()}", style = MaterialTheme.typography.labelMedium, color = financeColors.expense)
        Text(
            text = "Net: ₹${total.toInt()}", 
            style = MaterialTheme.typography.labelMedium, 
            fontWeight = FontWeight.Bold,
            color = if (total >= 0) financeColors.income else financeColors.expense
        )
    }
}
