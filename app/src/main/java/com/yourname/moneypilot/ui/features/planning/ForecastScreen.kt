package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
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
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors
import java.time.format.DateTimeFormatter

@Composable
fun ForecastScreen(
    viewModel: ForecastViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val financeColors = LocalFinanceColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is ScreenState.Success -> {
                val data = state.data
                val forecast = data.forecast

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ForecastHeroCard(data, financeColors, isPrivacyMode)
                    }

                    if (forecast != null) {
                        item {
                            Text("30-Day Outlook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        items(forecast.dailyProjections.filter { it.pendingBills.isNotEmpty() || it.isBelowSafetyLimit }) { projection ->
                            ProjectionItem(projection, financeColors, isPrivacyMode)
                        }
                        
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
            is ScreenState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Add a primary wallet to see financial forecasts.")
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ForecastHeroCard(data: ForecastState, colors: com.yourname.moneypilot.ui.theme.FinanceColors, isPrivacyMode: Boolean) {
    val runway = data.forecast?.estimatedRunwayDays ?: 0
    val runwayColor = when {
        runway < 7 -> colors.expense
        runway < 20 -> colors.warning
        else -> colors.income
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = runwayColor.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Financial Runway", style = MaterialTheme.typography.labelMedium, color = runwayColor)
            Text(
                text = if (isPrivacyMode) "•• Days" else if (runway > 365) "Safe" else "$runway Days",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = runwayColor
            )
            val velocityLabel = if (isPrivacyMode) "••••" else "₹${data.dailyVelocity.toInt()}"
            Text(
                text = "Based on spending of $velocityLabel/day in ${data.primaryWalletName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = runwayColor.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Upcoming Bills", style = MaterialTheme.typography.labelSmall)
                    val billsLabel = if(isPrivacyMode) "••••" else "₹ ${data.forecast?.totalUpcomingCommitments?.toInt() ?: 0}"
                    Text(billsLabel, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("End of Month", style = MaterialTheme.typography.labelSmall)
                    val eomLabel = if(isPrivacyMode) "••••" else "₹ ${data.forecast?.projectedEndOfMonthBalance?.toInt() ?: 0}"
                    Text(eomLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProjectionItem(
    projection: com.yourname.moneypilot.domain.model.WalletProjection, 
    colors: com.yourname.moneypilot.ui.theme.FinanceColors,
    isPrivacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (projection.isBelowSafetyLimit) 
            CardDefaults.cardColors(containerColor = colors.expense.copy(alpha = 0.05f))
            else CardDefaults.cardColors()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = projection.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (projection.pendingBills.isNotEmpty()) {
                    Text(
                        text = "Due: ${projection.pendingBills.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.expense
                    )
                }
                if (projection.isBelowSafetyLimit) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = colors.warning, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Low balance buffer", style = MaterialTheme.typography.labelSmall, color = colors.warning)
                    }
                }
            }
            val balanceLabel = if (isPrivacyMode) "••••" else "₹ ${projection.projectedBalance.toInt()}"
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (projection.projectedBalance < 0) colors.expense else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
