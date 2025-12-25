package com.yourname.moneypilot.ui.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

@Composable
fun CalendarScreen(
    onAddTransaction: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val calendarState by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(paddingValues = PaddingValues(top = 8.dp))) {
        CalendarGrid(
            currentMonth = calendarState.currentMonth,
            dailySummaries = calendarState.dailySummaries,
            selectedDate = calendarState.selectedDate,
            onDateSelected = { viewModel.onDateSelected(it) }
        )
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
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

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
                Row {
                    if (summary.totalIncome > 0) Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(IncomeGreen))
                    if (summary.totalExpense > 0) Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(ExpenseRed))
                }
            }
        }
    }
}
