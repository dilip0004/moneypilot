package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.DashboardCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    viewModel: InvestmentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Investments") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add Investment Logic */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Investment")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    InvestmentContent(state.data)
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No investments tracked yet.")
                    }
                }
                is ScreenState.Error -> {
                    Text("Error: ${state.message}")
                }
            }
        }
    }
}

@Composable
fun InvestmentContent(state: InvestmentsState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DashboardCard(
                title = "Total Portfolio Value",
                amount = "INR ${state.totalValue}"
            )
        }
        
        items(state.investments) { investment ->
            InvestmentItem(investment)
        }
    }
}

@Composable
fun InvestmentItem(investment: InvestmentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = investment.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${investment.symbol} • ${investment.type}", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "INR ${investment.quantity * investment.currentPrice}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${investment.quantity} units @ ${investment.currentPrice}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
