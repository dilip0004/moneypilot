package com.yourname.moneypilot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yourname.moneypilot.data.local.preferences.AppTheme
import com.yourname.moneypilot.ui.features.accounts.AccountsScreen
import com.yourname.moneypilot.ui.features.accounts.AddEditAccountScreen
import com.yourname.moneypilot.ui.features.backup.BackupScreen
import com.yourname.moneypilot.ui.features.budgets.AddEditBudgetScreen
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.calendar.CalendarScreen
import com.yourname.moneypilot.ui.features.categories.CategoryManagerScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
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
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val preferences by mainViewModel.userPreferences.collectAsState()

            val darkTheme = when (preferences?.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.OLED -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                null -> isSystemInDarkTheme()
            }

            MoneyPilotTheme(
                darkTheme = darkTheme,
                dynamicColor = preferences?.useDynamicColor ?: true,
                trueBlack = preferences?.theme == AppTheme.OLED
            ) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val screens = listOf(
        Screen.Calendar,
        Screen.Analytics,
        Screen.Records
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("MoneyPilot Profile", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Savings Goals") },
                    selected = false,
                    onClick = { 
                        navController.navigate("goals")
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Budgets") },
                    selected = false,
                    onClick = { 
                        navController.navigate("budgets")
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Wallets") },
                    selected = false,
                    onClick = { 
                        navController.navigate("accounts_list")
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Distribution") },
                    selected = false,
                    onClick = { 
                        navController.navigate("distribution")
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                val showTopBar = screens.any { it.route == currentDestination?.route }
                if (showTopBar) {
                    TopAppBar(
                        title = { Text("MoneyPilot") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            },
            bottomBar = {
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
                startDestination = Screen.Calendar.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Calendar.route) { 
                    CalendarScreen(onAddTransaction = { date ->
                        navController.navigate("add_transaction?date=${date}")
                    })
                }
                
                composable(Screen.Analytics.route) { 
                    ReportsScreen(onPopBackStack = { navController.popBackStack() })
                }
                
                composable(Screen.Records.route) { 
                    TransactionsScreen(
                        onAddTransaction = {
                            navController.navigate("add_transaction")
                        },
                        onEditTransaction = { transactionId ->
                            navController.navigate("add_transaction?transactionId=$transactionId")
                        }
                    )
                }
                
                composable("settings") { 
                    SettingsScreen(
                        onNavigateToAccounts = { navController.navigate("accounts_list") },
                        onNavigateToCategories = { navController.navigate("categories") },
                        onNavigateToBudgets = { navController.navigate("budgets") },
                        onNavigateToDistribution = { navController.navigate("distribution") },
                        onNavigateToAppearance = { navController.navigate("appearance") },
                        onNavigateToSecurity = { navController.navigate("security") },
                        onNavigateToNotifications = { navController.navigate("notifications") },
                        onNavigateToBackup = { navController.navigate("backup") }
                    ) 
                }

                composable("backup") {
                    BackupScreen(onPopBackStack = { navController.popBackStack() })
                }

                composable("categories") {
                    CategoryManagerScreen(onPopBackStack = { navController.popBackStack() })
                }

                composable("distribution") {
                    DistributionScreen(onPopBackStack = { navController.popBackStack() })
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
                
                composable(
                    route = "add_transaction?date={date}&transactionId={transactionId}",
                    arguments = listOf(
                        navArgument("date") { 
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("transactionId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { backStackEntry ->
                    AddEditTransactionScreen(
                        onPopBackStack = { navController.popBackStack() },
                        onNavigateToTransfer = { navController.navigate("transfer") }
                    )
                }
                composable("transfer") {
                    TransferScreen(onPopBackStack = {
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
}
