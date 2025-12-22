package com.yourname.moneypilot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourname.moneypilot.ui.features.accounts.AccountsScreen
import com.yourname.moneypilot.ui.features.accounts.AddEditAccountScreen
import com.yourname.moneypilot.ui.features.budgets.AddEditBudgetScreen
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.dashboard.DashboardScreen
import com.yourname.moneypilot.ui.features.goals.AddEditGoalScreen
import com.yourname.moneypilot.ui.features.goals.GoalsScreen
import com.yourname.moneypilot.ui.features.investments.InvestmentsScreen
import com.yourname.moneypilot.ui.features.reports.ReportsScreen
import com.yourname.moneypilot.ui.features.settings.AppearanceScreen
import com.yourname.moneypilot.ui.features.settings.NotificationsScreen
import com.yourname.moneypilot.ui.features.settings.SecurityScreen
import com.yourname.moneypilot.ui.features.settings.SettingsScreen
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionScreen
import com.yourname.moneypilot.ui.features.transactions.TransactionsScreen
import com.yourname.moneypilot.ui.features.transactions.TransferScreen
import com.yourname.moneypilot.ui.navigation.Screen
import com.yourname.moneypilot.ui.theme.MoneyPilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val preferences by mainViewModel.userPreferences.collectAsState()

            MoneyPilotTheme(
                darkTheme = preferences?.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme(),
                dynamicColor = preferences?.useDynamicColor ?: true,
                trueBlack = preferences?.useTrueBlack ?: false
            ) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Transactions,
        Screen.Stats,
        Screen.Accounts,
        Screen.Planning,
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
            startDestination = Screen.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Transactions.route) { 
                TransactionsScreen(
                    onAddTransaction = { navController.navigate("add_transaction") }
                )
            }
            
            composable(Screen.Stats.route) { 
                StatsHubScreen(
                    onNavigateToReports = { navController.navigate("reports") }
                )
            }
            
            composable(Screen.Accounts.route) { 
                AccountsHubScreen(
                    onNavigateToAccounts = { navController.navigate("accounts_list") },
                    onNavigateToInvestments = { navController.navigate("investments") }
                )
            }
            
            composable(Screen.Planning.route) { 
                PlanningHubScreen(
                    onNavigateToBudgets = { navController.navigate("budgets") },
                    onNavigateToGoals = { navController.navigate("goals") }
                )
            }
            
            composable(Screen.Settings.route) { 
                SettingsScreen(
                    onNavigateToAccounts = { navController.navigate("accounts_list") },
                    onNavigateToAppearance = { navController.navigate("appearance") },
                    onNavigateToSecurity = { navController.navigate("security") },
                    onNavigateToNotifications = { navController.navigate("notifications") }
                ) 
            }

            // Sub-screens
            composable("reports") {
                ReportsScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable("accounts_list") {
                AccountsScreen(onAddAccount = { navController.navigate("add_account") })
            }
            
            composable("investments") {
                InvestmentsScreen()
            }
            
            composable("budgets") {
                BudgetsScreen(onAddBudget = { navController.navigate("add_budget") })
            }
            
            composable("goals") {
                GoalsScreen(onAddGoal = { navController.navigate("add_goal") })
            }
            
            composable("appearance") {
                AppearanceScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable("security") {
                SecurityScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable("notifications") {
                NotificationsScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable("add_account") {
                AddEditAccountScreen(onPopBackStack = { navController.popBackStack() })
            }
            
            composable("add_transaction") {
                AddEditTransactionScreen(
                    onPopBackStack = { navController.popBackStack() },
                    onNavigateToTransfer = { navController.navigate("transfer") }
                )
            }
            composable("transfer") {
                TransferScreen(onPopBackStack = { navController.popBackStack() })
            }
            composable("add_goal") {
                AddEditGoalScreen(onPopBackStack = { navController.popBackStack() })
            }
            composable("add_budget") {
                AddEditBudgetScreen(onPopBackStack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun AccountsHubScreen(onNavigateToAccounts: () -> Unit, onNavigateToInvestments: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Accounts Hub", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.padding(16.dp))
        Button(onClick = onNavigateToAccounts, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Accounts (Bank, Cash)")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = onNavigateToInvestments, modifier = Modifier.fillMaxWidth()) {
            Text("Investments (Stocks, Crypto)")
        }
    }
}

@Composable
fun PlanningHubScreen(onNavigateToBudgets: () -> Unit, onNavigateToGoals: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Planning Hub", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.padding(16.dp))
        Button(onClick = onNavigateToBudgets, modifier = Modifier.fillMaxWidth()) {
            Text("Budgets")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = onNavigateToGoals, modifier = Modifier.fillMaxWidth()) {
            Text("Savings Goals")
        }
    }
}

@Composable
fun StatsHubScreen(onNavigateToReports: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Statistics", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.padding(16.dp))
        Button(onClick = onNavigateToReports, modifier = Modifier.fillMaxWidth()) {
            Text("View Visual Reports")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            "Track your spending patterns and cash flow distribution.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
