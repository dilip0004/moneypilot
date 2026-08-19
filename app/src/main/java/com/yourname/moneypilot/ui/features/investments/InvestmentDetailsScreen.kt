package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailsScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: InvestmentDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Holding Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    val investment = (uiState as? ScreenState.Success)?.data?.investment
                    if (investment != null) {
                        IconButton(onClick = { onEdit(investment.id) }) {
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ScreenState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ScreenState.Success -> {
                    val data = state.data
                    val inv = data.investment!!
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        item {
                            InvestmentHeader(inv, data.totalInvested, data.currentValue)
                        }

                        item {
                            Text("Holding Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        item {
                            ParametersCard(inv)
                        }

                        if (data.history.isNotEmpty()) {
                            item {
                                Text("Contribution History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(data.history) { activity ->
                                // Reuse item from main screen
                                InvestmentActivityItem(activity, false)
                            }
                        }
                    }
                }
                is ScreenState.Empty -> Text("Investment not found", Modifier.align(Alignment.Center))
                else -> {}
            }
        }
    }
}

@Composable
fun InvestmentHeader(inv: InvestmentEntity, invested: Double, current: Double) {
    val financeColors = LocalFinanceColors.current
    val gain = current - invested
    val isProfit = gain >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(inv.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(
                "₹ ${String.format("%.0f", current)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Invested", style = MaterialTheme.typography.labelSmall)
                    Text("₹ ${String.format("%.0f", invested)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                VerticalDivider(Modifier.height(32.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gain/Loss", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${if (isProfit) "+" else ""}₹ ${gain.toInt()}",
                        color = if (isProfit) financeColors.income else financeColors.expense,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ParametersCard(inv: InvestmentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ParameterRow("Asset Type", inv.type)
            inv.startDate?.let { 
                ParameterRow("Start Date", it.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
            }
            
            when(inv.type) {
                "STOCKS", "CRYPTO", "GOLD" -> {
                    ParameterRow("Quantity", String.format("%.4f", inv.quantity))
                    ParameterRow("Avg Price", "₹ ${String.format("%.2f", inv.averagePrice)}")
                }
                else -> {
                    ParameterRow("Current Balance", "₹ ${String.format("%.0f", inv.quantity)}")
                }
            }
        }
    }
}

@Composable
fun ParameterRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
