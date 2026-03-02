package com.yourname.moneypilot.ui.features.loans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = { Text(state.loan?.name ?: "Loan") },
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
                val (snapshot, points) = remember(loan) {
                    LoanCalculator.computeSnapshot(loan, emptyList()) // Pass empty list for now
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
                        Text("Transaction history for this loan will be shown here.", style = MaterialTheme.typography.bodyMedium)
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
