package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
import com.yourname.moneypilot.ui.features.goals.GoalsScreen
import com.yourname.moneypilot.ui.features.investments.InvestmentsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningHubScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onAddBudget: () -> Unit,
    onAddInvestment: () -> Unit,
    onAddBigBill: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Goals", "Budgets", "Investments", "Big Bills", "Distribution")

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = { Text("Financial Planning", fontWeight = FontWeight.Bold) }
                    )
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 16.dp,
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
                2 -> InvestmentsScreen() // Add onAddInvestment trigger to FAB inside screen
                3 -> BigBillsScreen() // Add onAddBigBill trigger to FAB inside screen
                4 -> DistributionScreen(
                    onPopBackStack = {}
                )
            }
        }
    }
}
