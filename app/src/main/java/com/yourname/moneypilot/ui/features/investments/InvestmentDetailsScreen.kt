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
import com.yourname.moneypilot.ui.components.*
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
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
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
                            SectionHeader("Holding Parameters")
                        }

                        item {
                            ParametersCard(inv)
                        }

                        if (data.history.isNotEmpty()) {
                            item {
                                SectionHeader("Contribution History")
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

    FinancialSummarySurface(
        title = inv.name,
        primaryValue = "₹ ${String.format("%.0f", current)}",
        secondaryInfo = {
            SummaryStat(label = "Invested", value = "₹ ${String.format("%.0f", invested)}")
            SummaryStat(
                label = "Gain/Loss", 
                value = "${if (isProfit) "+" else ""}₹ ${gain.toInt()}",
                color = if (isProfit) financeColors.income else financeColors.expense
            )
        }
    )
}

@Composable
fun ParametersCard(inv: InvestmentEntity) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        opacity = GlassLevel.High
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
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
