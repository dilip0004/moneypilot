package com.yourname.moneypilot.ui.features.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.ui.common.ScreenState
import java.time.format.DateTimeFormatter

@Composable
fun GoalsScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Text(
                text = "Savings Goals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGoal) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.data.goals) { goal ->
                            GoalItem(
                                goal = goal,
                                onClick = { onEditGoal(goal.id) }
                            )
                            GoalRecentEntries(goal.id, viewModel)
                        }
                    }
                }
                is ScreenState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No goals set yet")
                    }
                }
            }
        }
    }
}

@Composable
fun GoalItem(
    goal: GoalEntity,
    onClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = goal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Target: ${goal.targetDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "₹${goal.currentAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(text = "Goal: ₹${goal.targetAmount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GoalRecentEntries(goalId: Long, viewModel: GoalsViewModel) {
    val entries by viewModel.getTransactionsForGoal(goalId).collectAsState(initial = emptyList())

    if (entries.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = "Recent entries", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            entries.take(3).forEach { txWithDetails ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        val tx = txWithDetails.transaction
                        val title = txWithDetails.category?.name ?: tx.note ?: "Uncategorized"
                        Column {
                            Text(title, style = MaterialTheme.typography.bodyMedium)
                            Text(tx.dateTime.toLocalDate().toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "₹${tx.amount}", style = MaterialTheme.typography.bodyLarge, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
