package com.yourname.moneypilot.ui.features.loans

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
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
                fmt.maximumFractionDigits = 0
                val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
                val monthYearFmt = DateTimeFormatter.ofPattern("MMM yyyy")

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Top Card: Outstanding Principal & Core Stats
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Outstanding Principal", 
                                    style = MaterialTheme.typography.labelMedium, 
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    fmt.format(snapshot.outstandingPrincipal), 
                                    style = MaterialTheme.typography.headlineMedium, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                                
                                val progress = if (snapshot.originalPrincipal > 0) 
                                    ((snapshot.originalPrincipal - snapshot.outstandingPrincipal) / snapshot.originalPrincipal).toFloat().coerceIn(0f, 1f)
                                    else 0f
                                
                                Spacer(Modifier.height(12.dp))
                                
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Paid: ${fmt.format(snapshot.originalPrincipal - snapshot.outstandingPrincipal)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Original: ${fmt.format(snapshot.originalPrincipal)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    CompactStatItem("${snapshot.currentInterestRate}%", "Interest", Modifier.weight(1f))
                                    VerticalDivider(Modifier.height(32.dp).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    CompactStatItem(fmt.format(snapshot.currentEmi), "Monthly EMI", Modifier.weight(1.5f))
                                    VerticalDivider(Modifier.height(32.dp).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    
                                    val years = snapshot.monthsRemaining / 12
                                    val months = snapshot.monthsRemaining % 12
                                    val tenureStr = if (years > 0) "${years}Y ${months}M" else "${months}M"
                                    CompactStatItem(tenureStr, "Remaining", Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 2. Next EMI Info
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Next EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${snapshot.nextDueDate.format(dateFmt)} · ${fmt.format(snapshot.currentEmi)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // 3. Repayment Timeline
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Repayment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("${snapshot.monthsRemaining} months remaining", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Started ${loan.startDate.format(monthYearFmt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(snapshot.expectedEndDate.format(monthYearFmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Expected completion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Principal Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(fmt.format(snapshot.totalPrincipalPaid), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Interest Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(fmt.format(snapshot.totalInterestPaid), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Interest Saved
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF00A36C).copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00A36C).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF00A36C), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Interest Saved", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00A36C), fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (snapshot.interestSavedApprox > 0) fmt.format(snapshot.interestSavedApprox) else "₹0",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00A36C)
                                )
                                val savedMsg = if (snapshot.tenureSavedMonths > 0) 
                                    "Saved ${snapshot.tenureSavedMonths} months of tenure" 
                                    else "Make a prepayment to save interest"
                                Text(savedMsg, style = MaterialTheme.typography.labelSmall, color = Color(0xFF00A36C).copy(alpha = 0.7f))
                            }
                        }
                    }

                    // 5. Audit Trail Header
                    item {
                        var isAuditVisible by remember { mutableStateOf(true) }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isAuditVisible = !isAuditVisible }
                                ) {
                                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                    Icon(
                                        if (isAuditVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

                            AnimatedVisibility(visible = isAuditVisible) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (state.events.isEmpty()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                                    } else {
                                        state.events.sortedByDescending { it.eventDate }.forEach { event ->
                                            LoanEventItem(event, fmt)
                                        }
                                    }
                                }
                            }
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
fun CompactStatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
