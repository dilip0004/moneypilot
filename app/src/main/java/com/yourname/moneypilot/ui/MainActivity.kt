package com.yourname.moneypilot.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yourname.moneypilot.data.local.preferences.AppTheme
import com.yourname.moneypilot.domain.loan.LoanAutoDeductionProcessor
import com.yourname.moneypilot.domain.monthly.MonthlyRolloverProcessor
import com.yourname.moneypilot.ui.features.accounts.AccountsHubScreen
import com.yourname.moneypilot.ui.features.accounts.AddEditAccountScreen
import com.yourname.moneypilot.ui.features.backup.BackupScreen
import com.yourname.moneypilot.ui.features.budgets.AddEditBudgetScreen
import com.yourname.moneypilot.ui.features.budgets.BudgetsScreen
import com.yourname.moneypilot.ui.features.categories.CategoryManagerScreen
import com.yourname.moneypilot.ui.features.dashboard.DashboardHubScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
import com.yourname.moneypilot.ui.features.goals.AddEditGoalScreen
import com.yourname.moneypilot.ui.features.investments.AddEditInvestmentScreen
import com.yourname.moneypilot.ui.features.investments.InvestmentsScreen
import com.yourname.moneypilot.ui.features.loans.AddEditLoanScreen
import com.yourname.moneypilot.ui.features.loans.LoanDetailsScreen
import com.yourname.moneypilot.ui.features.planning.AddEditBigBillScreen
import com.yourname.moneypilot.ui.features.planning.PlanningHubScreen
import com.yourname.moneypilot.ui.features.reports.ReportsScreen
import com.yourname.moneypilot.ui.features.settings.AppearanceScreen
import com.yourname.moneypilot.ui.features.settings.DiagnosticsScreen
import com.yourname.moneypilot.ui.features.settings.NotificationsScreen
import com.yourname.moneypilot.ui.features.settings.SecurityScreen
import com.yourname.moneypilot.ui.features.settings.SettingsScreen
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionScreen
import com.yourname.moneypilot.ui.features.transactions.TransferScreen
import com.yourname.moneypilot.ui.navigation.Screen
import com.yourname.moneypilot.ui.theme.MoneyPilotTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var loanAutoDeductionProcessor: LoanAutoDeductionProcessor

    @javax.inject.Inject
    lateinit var monthlyRolloverProcessor: MonthlyRolloverProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Run processors on app startup (catch-up logic)
        lifecycleScope.launch {
            loanAutoDeductionProcessor.process()
            monthlyRolloverProcessor.process()
        }

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

            val isOled = preferences?.theme == AppTheme.OLED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted -> hasNotificationPermission = isGranted }
                )

                LaunchedEffect(Unit) {
                    if (!hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MoneyPilotTheme(darkTheme = darkTheme, trueBlack = isOled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val navItems = listOf(
        Screen.Transactions,
        Screen.Stats,
        Screen.Accounts,
        Screen.Planning,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            val showBottomBar = navItems.any { it.route == currentDestination?.route }
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            composable(Screen.Transactions.route) {
                DashboardHubScreen(
                    onAddTransaction = { date -> navController.navigate("add_transaction?date=$date") },
                    onEditTransaction = { transactionId -> navController.navigate("add_transaction?transactionId=$transactionId") },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } }
                )
            }

            composable(Screen.Stats.route) {
                ReportsScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable(Screen.Accounts.route) {
                AccountsHubScreen(
                    onAddAccount = { navController.navigate("add_account") },
                    onAddLoan = { navController.navigate("add_loan") },
                    onLoanClick = { loanId -> navController.navigate("loan_details/$loanId") }
                )
            }

            composable(Screen.Planning.route) {
                PlanningHubScreen(
                    onAddGoal = { navController.navigate("add_goal") },
                    onEditGoal = { id -> navController.navigate("add_goal?goalId=$id") },
                    onAddBudget = { navController.navigate("add_budget") },
                    onAddInvestment = { navController.navigate("add_investment") },
                    onAddBigBill = { navController.navigate("add_big_bill") }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToAccounts = { navController.navigate("accounts_list") },
                    onNavigateToCategories = { navController.navigate("categories") },
                    onNavigateToBudgets = { navController.navigate("budgets_list") },
                    onNavigateToDistribution = { navController.navigate("distribution") },
                    onNavigateToAppearance = { navController.navigate("appearance") },
                    onNavigateToSecurity = { navController.navigate("security") },
                    onNavigateToNotifications = { navController.navigate("notifications") },
                    onNavigateToBackup = { navController.navigate("backup") },
                    onNavigateToDiagnostics = { navController.navigate("diagnostics") }
                )
            }

            composable("backup") { BackupScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("categories") { CategoryManagerScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("distribution") { DistributionScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("investments") { InvestmentsScreen() }
            composable("appearance") { AppearanceScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("security") { SecurityScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("notifications") { NotificationsScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("diagnostics") { DiagnosticsScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_account") { AddEditAccountScreen(onPopBackStack = { navController.popBackStack() }) }

            composable("accounts_list") {
                AccountsHubScreen(
                    onAddAccount = { navController.navigate("add_account") },
                    onAddLoan = { navController.navigate("add_loan") },
                    onLoanClick = { loanId -> navController.navigate("loan_details/$loanId") }
                )
            }

            composable("budgets_list") {
                BudgetsScreen(onAddBudget = { navController.navigate("add_budget") })
            }

            composable(
                route = "add_transaction?date={date}&transactionId={transactionId}",
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                AddEditTransactionScreen(
                    onPopBackStack = { navController.popBackStack() },
                    onNavigateToTransfer = { navController.navigate("transfer") }
                )
            }

            composable(
                route = "add_goal?goalId={goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditGoalScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable(
                route = "add_investment?investmentId={investmentId}",
                arguments = listOf(navArgument("investmentId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditInvestmentScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable(
                route = "add_big_bill?bigBillId={bigBillId}",
                arguments = listOf(navArgument("bigBillId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditBigBillScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable(
                route = "add_loan?loanId={loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditLoanScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable("transfer") { TransferScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_budget") { AddEditBudgetScreen(onPopBackStack = { navController.popBackStack() }) }

            composable(
                route = "loan_details/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("loanId") ?: -1L
                LoanDetailsScreen(
                    loanId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
