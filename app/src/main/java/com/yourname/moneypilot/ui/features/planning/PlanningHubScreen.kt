package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
import com.yourname.moneypilot.ui.features.goals.GoalsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningHubScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onAddBudget: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Goals", "Budgets", "Distribution")

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = { Text("Financial Planning", fontWeight = FontWeight.Bold) }
                    )
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTabIndex) {
                0 -> GoalsScreen(
                    onAddGoal = onAddGoal,
                    onEditGoal = onEditGoal
                )
                1 -> BudgetsScreen(
                    onAddBudget = onAddBudget
                )
                2 -> DistributionScreen(
                    onPopBackStack = {} // Handled internally in hub
                )
            }
        }
    }
}
