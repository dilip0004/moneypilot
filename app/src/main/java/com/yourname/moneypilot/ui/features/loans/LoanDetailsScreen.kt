package com.yourname.moneypilot.ui.features.loans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.domain.loan.LoanCalculator
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailsScreen(
    loanId: Long,
    onBack: () -> Unit,
    viewModel: LoanDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(loanId) { viewModel.load(loanId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.loan?.name ?: "Loan Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                val (snapshot, points) = remember(loan, state.events) {
                    LoanCalculator.computeSnapshot(loan, state.events)
                }
                val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Outstanding & Quick Stats
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Outstanding Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fmt.format(snapshot.outstandingPrincipal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatBox("Rate", "${snapshot.currentInterestRate}%", Modifier.weight(1f))
                                    StatBox("EMI", fmt.format(snapshot.emiAmount), Modifier.weight(1.5f))
                                }
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatBox("Interest Saved", fmt.format(snapshot.interestSavedApprox), Modifier.weight(1f), color = Color(0xFF00A36C))
                                    StatBox("Ends on", snapshot.expectedEndDate.format(DateTimeFormatter.ofPattern("MMM yyyy")), Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 2. Visual Breakdown
                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Repayment Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DonutChart(
                                        values = listOf(snapshot.totalPrincipalPaid, snapshot.totalInterestPaid),
                                        modifier = Modifier.size(120.dp)
                                    )
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LegendItem("Principal", Color(0xFF7B5CFA))
                                        Text(fmt.format(snapshot.totalPrincipalPaid), style = MaterialTheme.typography.bodySmall)
                                        LegendItem("Interest", Color(0xFFFF5733))
                                        Text(fmt.format(snapshot.totalInterestPaid), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Audit Trail (History)
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                    "No historical changes or repayments recorded.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.events.reversed()) { event ->
                            LoanEventItem(event, fmt)
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun LoanEventItem(event: LoanEventEntity, fmt: NumberFormat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Column(modifier = Modifier.weight(1f)) {
            val title = when(event.eventType) {
                "REPAYMENT_POSTED" -> "Monthly Repayment"
                "RATE_CHANGE" -> "Interest Rate Change"
                "PREPAYMENT" -> "Principal Prepayment"
                else -> event.eventType.replace("_", " ")
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(event.eventDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            val detail = when(event.eventType) {
                "REPAYMENT_POSTED" -> "EMI processed successfully"
                "RATE_CHANGE" -> "New ROI: ${event.newInterestRate}%"
                "PREPAYMENT" -> "Lump sum: ${fmt.format(event.amount ?: 0.0)}"
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DonutChart(values: List<Double>, modifier: Modifier = Modifier) {
    val total = values.sum().takeIf { it > 0 } ?: 1.0
    val colors = listOf(Color(0xFF7B5CFA), Color(0xFFFF5733))
    Canvas(modifier = modifier) {
        var start = -90f
        val stroke = Stroke(width = 24.dp.toPx())
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
