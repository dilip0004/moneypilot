package com.yourname.moneypilot.ui.features.planning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
import com.yourname.moneypilot.ui.features.goals.GoalsScreen
import com.yourname.moneypilot.ui.features.investments.InvestmentsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningHubScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onGoalClick: (Long) -> Unit,
    onAddBudget: (String?) -> Unit,
    onEditBudget: (Long, String?) -> Unit,
    onAddInvestment: () -> Unit,
    onAddBigBill: () -> Unit,
    onEditBigBill: (Long) -> Unit
) {
    // Persist selected tab across navigation
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Goals", "Budgets", "Investments", "Big Bills", "Forecast", "Simulate", "Distribution")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    CenterAlignedTopAppBar(
                        title = { Text("Financial Planning", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        edgePadding = 12.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier.height(44.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    ) 
                                },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.testTag("planning_tab_${title.lowercase().replace(" ", "_")}")
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
                    onEditGoal = onEditGoal,
                    onGoalClick = onGoalClick
                )
                1 -> BudgetsScreen(
                    onAddBudget = onAddBudget,
                    onEditBudget = onEditBudget
                )
                2 -> InvestmentsScreen(
                    onAddInvestment = onAddInvestment
                )
                3 -> BigBillsScreen(
                    onAddBigBill = onAddBigBill,
                    onEditBigBill = onEditBigBill
                )
                4 -> ForecastScreen()
                5 -> WhatIfSimulationScreen()
                6 -> DistributionScreen(
                    onPopBackStack = {}
                )
            }
        }
    }
}
