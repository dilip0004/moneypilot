package com.yourname.moneypilot.ui.features.distribution

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import com.yourname.moneypilot.domain.usecase.distribution.DistributionAction
import com.yourname.moneypilot.ui.common.ScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionScreen(
    onPopBackStack: () -> Unit,
    viewModel: DistributionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DistributionViewModel.UiEvent.PlanExecuted -> {
                    snackbarHostState.showSnackbar("Plan executed successfully")
                }
                is DistributionViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Leftover Distribution") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Open Add Rule Dialog */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScreenState.Success -> {
                    DistributionContent(
                        state = state.data,
                        onExecutePlan = { viewModel.executePlan() },
                        onDeleteRule = { viewModel.deleteRule(it) }
                    )
                }
                else -> {
                    Text("No data available.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun DistributionContent(
    state: DistributionState,
    onExecutePlan: () -> Unit,
    onDeleteRule: (DistributionRuleEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Surplus", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "₹ ${state.totalSurplus}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onExecutePlan,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.plan.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute Transfer Plan")
                    }
                }
            }
        }

        item {
            Text("Distribution Rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        items(state.rules) { rule ->
            RuleItem(rule, onDeleteRule)
        }

        if (state.plan.isNotEmpty()) {
            item {
                Text("Generated Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(state.plan) { action ->
                PlanActionItem(action)
            }
        }
    }
}

@Composable
fun RuleItem(rule: DistributionRuleEntity, onDelete: (DistributionRuleEntity) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Target Account ID: ${rule.targetWalletId}", style = MaterialTheme.typography.bodyLarge)
                val detail = if (rule.percentage != null) "${rule.percentage}%" else "₹ ${rule.fixedAmount}"
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(rule) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PlanActionItem(action: DistributionAction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${action.sourceName} → ${action.targetName}", style = MaterialTheme.typography.bodyLarge)
                Text("₹ ${action.amount}", fontWeight = FontWeight.Bold)
            }
            Text("Suggested", style = MaterialTheme.typography.labelSmall)
        }
    }
}
