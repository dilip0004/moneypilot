package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onAddInvestment: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddInvestment) {
                Icon(Icons.Default.Add, contentDescription = "Add Investment")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScreenState.Success -> {
                    InvestmentList(state.data, isPrivacyMode)
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No investments tracked yet. Start building your portfolio!")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun InvestmentList(data: InvestmentState, isPrivacyMode: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            PortfolioHeroCard(data.totalValue, data.totalGain, data.gainPercentage, isPrivacyMode)
        }
        
        item {
            Text("Your Holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(data.investments) { investment ->
            InvestmentItem(investment, isPrivacyMode)
        }
    }
}

@Composable
fun PortfolioHeroCard(totalValue: Double, gain: Double, gainPct: Double, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Portfolio Value", style = MaterialTheme.typography.labelMedium)
            Text(
                text = if(isPrivacyMode) "••••" else "₹ $totalValue",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isProfit = gain >= 0
                Icon(
                    imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isProfit) financeColors.income else financeColors.expense,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val gainText = if(isPrivacyMode) "••••" else "${if (isProfit) "+" else ""}₹$gain ($gainPct%)"
                Text(
                    text = gainText,
                    color = if (isProfit) financeColors.income else financeColors.expense,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun InvestmentItem(investment: InvestmentEntity, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    val totalHoldings = investment.quantity * investment.currentPrice
    val totalGain = (investment.currentPrice - investment.averagePrice) * investment.quantity
    val isProfit = totalGain >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = investment.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val quantityLabel = if(isPrivacyMode) "•••• units" else "${investment.quantity} shares"
                val priceLabel = if(isPrivacyMode) "@ ••••" else "@ ₹${investment.averagePrice}"
                Text(
                    text = "$quantityLabel $priceLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = if(isPrivacyMode) "••••" else "₹ $totalHoldings", fontWeight = FontWeight.ExtraBold)
                Text(
                    text = if(isPrivacyMode) "••••" else "${if (isProfit) "+" else ""}₹${totalGain.toInt()}",
                    color = if (isProfit) financeColors.income else financeColors.expense,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
