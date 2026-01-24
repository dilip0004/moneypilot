package com.yourname.moneypilot.ui.features.loans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.yourname.moneypilot.domain.loan.LoanCalculator
import com.yourname.moneypilot.ui.components.AppDatePickerField
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoanDetailsScreen(
    loanId: Long,
    onBack: () -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyDeleted by remember { mutableStateOf<com.yourname.moneypilot.data.local.database.entities.LoanEventEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<com.yourname.moneypilot.data.local.database.entities.LoanEventEntity?>(null) }

    var showRateDialog by remember { mutableStateOf(false) }
    var showPrepayDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) { viewModel.load(loanId) }

    if (showRateDialog && state.loan != null) {
        RateChangeDialog(
            onDismiss = { showRateDialog = false },
            onSave = { date, rate, note ->
                viewModel.addRateChange(state.loan!!.id, date, rate, note)
                showRateDialog = false
            }
        )
    }

    if (showEditDialog && editTarget != null) {
        EditLoanEventDialog(
            event = editTarget!!,
            onDismiss = { showEditDialog = false },
            onSave = { updated -> viewModel.updateEvent(updated); showEditDialog = false }
        )
    }

    if (showPrepayDialog && state.loan != null) {
        PrepaymentDialog(
            onDismiss = { showPrepayDialog = false },
            onSave = { date, amount, note ->
                viewModel.addPrepayment(state.loan!!.id, date, amount, note)
                showPrepayDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.loan?.name ?: "Loan") },
                actions = {
                    TextButton(onClick = { showRateDialog = true }, enabled = state.loan != null) { Text("ROI") }
                    TextButton(onClick = { showPrepayDialog = true }, enabled = state.loan != null) { Text("Prepay") }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loan == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loan not found")
            }
            else -> {
                val loan = state.loan!!
                val events = state.events
                val (snapshot, points) = remember(loan, events) {
                    LoanCalculator.computeSnapshot(loan, events)
                }
                val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Outstanding", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fmt.format(snapshot.outstandingPrincipal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StatChip("ROI", "${snapshot.currentInterestRate}%")
                                    StatChip("EMI", fmt.format(snapshot.emiAmount))
                                    StatChip("Saved", fmt.format(snapshot.interestSavedApprox))
                                    StatChip("Next due", snapshot.nextDueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Paid breakdown (till date)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                DonutChart(
                                    values = listOf(snapshot.totalPrincipalPaid, snapshot.totalInterestPaid),
                                    modifier = Modifier.height(180.dp).fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Principal: ${fmt.format(snapshot.totalPrincipalPaid)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Interest: ${fmt.format(snapshot.totalInterestPaid)}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    item {
                        Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Outstanding trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                LineChart(points = points.map { it.outstanding }, modifier = Modifier.height(180.dp).fillMaxWidth())
                            }
                        }
                    }

                    item {
                        Text("Timeline (long-press an item to delete)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }

                    items(events.reversed()) { ev ->
                        Card(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    editTarget = ev
                                    showEditDialog = true
                                },
                                onLongClick = {
                                    recentlyDeleted = ev
                                    viewModel.deleteEvent(ev)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Event deleted",
                                            actionLabel = "UNDO",
                                            withDismissAction = true
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            recentlyDeleted?.let { viewModel.addBackDeleted(it) }
                                            recentlyDeleted = null
                                        }
                                    }
                                }
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("${ev.eventType.replace('_', ' ')}", fontWeight = FontWeight.SemiBold)
                                Text(ev.eventDate.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val d = ev.note ?: when (ev.eventType) {
                                    "RATE_CHANGE" -> "New ROI: ${ev.newInterestRate}%"
                                    "PREPAYMENT" -> "Prepaid: ${fmt.format(ev.amount ?: 0.0)}"
                                    else -> ""
                                }
                                if (d.isNotBlank()) Text(d)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    AssistChip(onClick = {}, enabled = false, label = { Text("$label: $value") })
}

@Composable
private fun DonutChart(values: List<Double>, modifier: Modifier = Modifier) {
    val total = values.sum().takeIf { it > 0 } ?: 1.0
    Canvas(modifier = modifier) {
        var start = -90f
        val stroke = Stroke(width = size.minDimension * 0.18f)
        values.forEachIndexed { i, v ->
            val sweep = (v / total * 360f).toFloat()
            drawArc(
                color = androidx.compose.ui.graphics.Color.hsl((i * 120f) % 360f, 0.55f, 0.55f),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke,
                size = Size(size.minDimension, size.minDimension)
            )
            start += sweep
        }
    }
}

@Composable
private fun LineChart(points: List<Double>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val maxV = points.maxOrNull() ?: 1.0
    val minV = points.minOrNull() ?: 0.0
    val range = (maxV - minV).takeIf { it > 0 } ?: 1.0

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val step = if (points.size == 1) 0f else w / (points.size - 1)
        var lastX = 0f
        var lastY = h - ((points[0] - minV) / range * h).toFloat()
        for (i in 1 until points.size) {
            val x = step * i
            val y = h - ((points[i] - minV) / range * h).toFloat()
            drawLine(
                color = androidx.compose.ui.graphics.Color.hsl(210f, 0.55f, 0.55f),
                start = Offset(lastX, lastY),
                end = Offset(x, y),
                strokeWidth = 4f
            )
            lastX = x; lastY = y
        }
    }
}

@Composable
private fun EditLoanEventDialog(
    event: com.yourname.moneypilot.data.local.database.entities.LoanEventEntity,
    onDismiss: () -> Unit,
    onSave: (com.yourname.moneypilot.data.local.database.entities.LoanEventEntity) -> Unit
) {
    var note by remember { mutableStateOf(event.note.orEmpty()) }
    var amountText by remember { mutableStateOf(event.amount?.toString().orEmpty()) }
    var rateText by remember { mutableStateOf(event.newInterestRate?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (event.eventType) {
                    "PREPAYMENT", "REPAYMENT_POSTED" -> {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount") },
                            singleLine = true
                        )
                    }
                    "RATE_CHANGE" -> {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it },
                            label = { Text("ROI (%)") },
                            singleLine = true
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updated = when (event.eventType) {
                    "PREPAYMENT", "REPAYMENT_POSTED" -> event.copy(
                        amount = amountText.toDoubleOrNull(),
                        note = note.ifBlank { null }
                    )
                    "RATE_CHANGE" -> event.copy(
                        newInterestRate = rateText.toDoubleOrNull(),
                        note = note.ifBlank { null }
                    )
                    else -> event.copy(note = note.ifBlank { null })
                }
                onSave(updated)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RateChangeDialog(
    onDismiss: () -> Unit,
    onSave: (java.time.LocalDate, Double, String?) -> Unit
) {
    var date by remember { mutableStateOf(java.time.LocalDate.now()) }
    var rateText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record ROI change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppDatePickerField(label = "Effective date", value = date, onChange = { date = it })
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("New interest rate (%)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateText.toDoubleOrNull() ?: 0.0
                    onSave(date, rate, noteText.ifBlank { null })
                },
                enabled = (rateText.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PrepaymentDialog(
    onDismiss: () -> Unit,
    onSave: (java.time.LocalDate, Double, String?) -> Unit
) {
    var date by remember { mutableStateOf(java.time.LocalDate.now()) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record prepayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppDatePickerField(label = "Date", value = date, onChange = { date = it })
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Prepayment amount") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSave(date, amt, noteText.ifBlank { null })
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
