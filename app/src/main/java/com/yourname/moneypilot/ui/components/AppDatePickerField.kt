package com.yourname.moneypilot.ui.components

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
    label: String,
    value: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
) {
    var show by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value.format(formatter),
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().clickable { show = true },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { show = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
            }
        }
    )

    if (show) {
        val initialMillis = value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        onChange(ld)
                    }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        ) {
            Column {
                DatePicker(state = datePickerState)
            }
        }
    }
}
