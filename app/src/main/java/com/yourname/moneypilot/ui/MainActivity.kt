package com.yourname.moneypilot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourname.moneypilot.ui.features.budgets.AddEditBudgetScreen
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.dashboard.DashboardScreen
import com.yourname.moneypilot.ui.features.goals.AddEditGoalScreen
import com.yourname.moneypilot.ui.features.goals.GoalsScreen
import com.yourname.moneypilot.ui.features.settings.SettingsScreen
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionScreen
import com.yourname.moneypilot.ui.features.transactions.TransactionsScreen
import com.yourname.moneypilot.ui.navigation.Screen
import com.yourname.moneypilot.ui.theme.MoneyPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyPilotTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Goals,
        Screen.Budgets,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val showBottomBar = screens.any { it.route == currentDestination?.route }
            
            if (showBottomBar) {
                NavigationBar {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { 
                DashboardScreen(onAddTransaction = {
                    navController.navigate("add_transaction")
                }) 
            }
            composable(Screen.Transactions.route) { TransactionsScreen() }
            composable(Screen.Goals.route) { 
                GoalsScreen(onAddGoal = {
                    navController.navigate("add_goal")
                }) 
            }
            composable(Screen.Budgets.route) { 
                BudgetsScreen(onAddBudget = {
                    navController.navigate("add_budget")
                }) 
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            
            composable("add_transaction") {
                AddEditTransactionScreen(onPopBackStack = {
                    navController.popBackStack()
                })
            }
            composable("add_goal") {
                AddEditGoalScreen(onPopBackStack = {
                    navController.popBackStack()
                })
            }
            composable("add_budget") {
                AddEditBudgetScreen(onPopBackStack = {
                    navController.popBackStack()
                })
            }
        }
    }
}
