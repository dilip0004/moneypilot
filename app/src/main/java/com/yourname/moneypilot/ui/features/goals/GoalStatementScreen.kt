package com.yourname.moneypilot.ui.features.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.MainViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.yourname.moneypilot.ui.components.*
import com.yourname.moneypilot.ui.common.CompactTransactionItem
import com.yourname.moneypilot.ui.common.ScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalStatementScreen(
    onPopBackStack: () -> Unit,
    viewModel: GoalStatementViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val title = (uiState as? ScreenState.Success)?.data?.goal?.name ?: "Goal Ledger"
            GlassTopBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            GoalSummaryHeader(data.goal!!, isPrivacyMode)
                        }
                        
                        item {
                            SectionHeader("Transaction History")
                        }

                        if (data.transactions.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No transactions yet", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        } else {
                            items(data.transactions, key = { it.transaction.id }) { tx ->
                                GlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    opacity = GlassLevel.High
                                ) {
                                    Box(Modifier.padding(12.dp)) {
                                        CompactTransactionItem(tx, isPrivacyMode = isPrivacyMode)
                                    }
                                }
                            }
                        }
                    }
                }
                is ScreenState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun GoalSummaryHeader(goal: com.yourname.moneypilot.data.local.database.entities.GoalEntity, isPrivacyMode: Boolean) {
    FinancialSummarySurface(
        title = "Savings Progress",
        primaryValue = if(isPrivacyMode) "••••" else "₹${goal.currentAmount}",
        progress = if(goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f,
        secondaryInfo = {
            SummaryStat(label = "Target", value = if(isPrivacyMode) "••••" else "₹${goal.targetAmount}")
            val rawProgress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
            SummaryStat(label = "Progress", value = "${(rawProgress * 100).toInt()}%")
        }
    )
}
