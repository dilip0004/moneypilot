package com.yourname.moneypilot.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDateTimePickerField(
    label: String,
    value: LocalDateTime,
    onChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val context = LocalContext.current

    OutlinedTextField(
        value = value.format(formatter),
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().clickable { showDate = true },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDate = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
            }
        }
    )

    if (showDate) {
        val initialMillis = value.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        onChange(LocalDateTime.of(date, value.toLocalTime()))
                        showDate = false
                        showTime = true
                    } else {
                        showDate = false
                    }
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { Column { DatePicker(state = state) } }
    }

    if (showTime) {
        val lt = value.toLocalTime()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val newTime = LocalTime.of(hour, minute)
                onChange(LocalDateTime.of(value.toLocalDate(), newTime))
                showTime = false
            },
            lt.hour,
            lt.minute,
            false
        ).apply {
            setOnCancelListener { showTime = false }
        }.show()
    }
}
