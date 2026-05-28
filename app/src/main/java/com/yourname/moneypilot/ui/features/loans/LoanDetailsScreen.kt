package com.yourname.moneypilot.ui.features.loans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.domain.loan.LoanCalculator
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailsScreen(
    loanId: Long,
    onBack: () -> Unit,
    onEditLoan: (Long) -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showPrepaymentDialog by remember { mutableStateOf(false) }
    var showRoiDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loanId) { viewModel.load(loanId) }

    if (showPrepaymentDialog) {
        AddPrepaymentDialog(
            wallets = state.wallets,
            onDismiss = { showPrepaymentDialog = false },
            onConfirm = { amount, walletId, note ->
                viewModel.addPrepayment(amount, walletId, LocalDate.now(), note)
                showPrepaymentDialog = false
            }
        )
    }

    if (showRoiDialog) {
        AddEventDialog(
            title = "Log Interest Rate Change",
            label = "New Annual ROI %",
            onDismiss = { showRoiDialog = false },
            onConfirm = { rate, note ->
                viewModel.addRoiChange(rate, LocalDate.now(), note)
                showRoiDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.loan?.name ?: "Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditLoan(loanId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Loan")
                    }
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
                val snapshot = state.snapshot ?: return@Scaffold
                val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                val dateFmt = DateTimeFormatter.ofPattern("MMM yyyy")

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Outstanding Principal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fmt.format(snapshot.outstandingPrincipal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatBox("Interest Rate", "${snapshot.currentInterestRate}%", Modifier.weight(1f))
                                    StatBox("Monthly EMI", fmt.format(snapshot.currentEmi), Modifier.weight(1.5f))
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Loan Timeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Months Remaining", style = MaterialTheme.typography.bodySmall)
                                    Text("${snapshot.monthsRemaining}", fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Expected End Date", style = MaterialTheme.typography.bodySmall)
                                    Text(snapshot.expectedEndDate.format(DateTimeFormatter.ofPattern("MMM yyyy")), fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Principal Paid", style = MaterialTheme.typography.bodySmall)
                                    Text(fmt.format(snapshot.totalPrincipalPaid), fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Interest Paid", style = MaterialTheme.typography.bodySmall)
                                    Text(fmt.format(snapshot.totalInterestPaid), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF00A36C).copy(alpha = 0.1f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A36C).copy(alpha = 0.3f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF00A36C), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Interest Saved", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00A36C))
                                    Text(fmt.format(snapshot.interestSavedApprox), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00A36C))
                                }
                            }

                            if (snapshot.tenureSavedMonths > 0) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("🏃 Tenure Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(8.dp))
                                        Text("${snapshot.tenureSavedMonths} Months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Ended by ${snapshot.expectedEndDate.format(dateFmt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                IconButton(onClick = { showRoiDialog = true }) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = "ROI Change", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showPrepaymentDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Log Prepayment", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (state.events.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    "No historical changes recorded. Use the icons above to log ROI changes or prepayments.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.events.sortedByDescending { it.eventDate }) { event ->
                            LoanEventItem(event, fmt)
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrepaymentDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedWalletId by remember { mutableStateOf<Long?>(wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Prepayment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Lump Sum Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val walletName = wallets.find { it.id == selectedWalletId }?.name ?: "Select Wallet"
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet.name) },
                                onClick = {
                                    selectedWalletId = wallet.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = amount.toDoubleOrNull() != null && selectedWalletId != null,
                onClick = { onConfirm(amount.toDouble(), selectedWalletId!!, note) }
            ) { Text("Confirm & Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddEventDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val num = value.toDoubleOrNull() ?: 0.0
                if (num > 0) onConfirm(num, note) 
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoanEventItem(event: LoanEventEntity, fmt: NumberFormat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Column(modifier = Modifier.weight(1f)) {
            val title = when(event.eventType) {
                "REPAYMENT_POSTED" -> "Monthly Repayment"
                "RATE_CHANGE" -> "Interest Rate Change"
                "PREPAYMENT" -> "Principal Prepayment"
                "INITIAL_LOAN" -> "Loan Disbursed"
                else -> event.eventType.replace("_", " ")
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(event.eventDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            val detail = when(event.eventType) {
                "REPAYMENT_POSTED" -> "EMI processed successfully"
                "RATE_CHANGE" -> "New ROI: ${event.newInterestRate}%"
                "PREPAYMENT" -> "Principal Reduced"
                "INITIAL_LOAN" -> "Original amount: ${fmt.format(event.amount ?: 0.0)}"
                else -> event.note ?: ""
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        if (event.eventType == "REPAYMENT_POSTED" || event.eventType == "PREPAYMENT") {
            Text(
                text = "-${fmt.format(event.amount ?: 0.0)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00A36C)
            )
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DonutChart(values: List<Double>, modifier: Modifier = Modifier) {
    val total = values.sum().takeIf { it > 0 } ?: 1.0
    val colors = listOf(Color(0xFF7B5CFA), Color(0xFFFF5733))
    Canvas(modifier = modifier) {
        var start = -90f
        val stroke = Stroke(width = 20.dp.toPx())
        values.forEachIndexed { i, v ->
            val sweep = (v / total * 360f).toFloat()
            drawArc(
                color = colors[i % colors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
            start += sweep
        }
    }
}
