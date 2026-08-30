package com.yourname.moneypilot.ui.features.planning

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.*
import com.yourname.moneypilot.ui.theme.motion.*
import com.yourname.moneypilot.ui.features.goals.GoalsScreen
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.investments.InvestmentsScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningHubScreen(
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onGoalClick: (Long) -> Unit,
    onAddBudget: (String?) -> Unit,
    onEditBudget: (Long, String?) -> Unit,
    onAddInvestment: () -> Unit,
    onInvestmentClick: (Long) -> Unit,
    onAddPlannedExpense: () -> Unit,
    onEditBigBill: (Long) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Goals", "Budgets", "Investments", "Planned Expenses", "Forecast", "Simulate", "Distribution")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text("Financial Planning", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassSurface(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                opacity = GlassLevel.Medium,
                shape = RoundedCornerShape(12.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 2.dp
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
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                ) 
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            SharedAxisXForward
                        } else {
                            SharedAxisXBackward
                        }.using(SizeTransform(clip = false))
                    },
                    label = "planning_tab_transition"
                ) { targetIndex ->
                    when (targetIndex) {
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
                            onAddInvestment = onAddInvestment,
                            onInvestmentClick = onInvestmentClick
                        )
                        3 -> PlannedExpensesScreen(
                            onAddPlannedExpense = onAddPlannedExpense,
                            onEditPlannedExpense = onEditBigBill
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
    }
}
