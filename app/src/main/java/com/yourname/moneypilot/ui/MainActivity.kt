package com.yourname.moneypilot.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourname.moneypilot.data.local.preferences.AppTheme
import com.yourname.moneypilot.domain.loan.LoanAutoDeductionProcessor
import com.yourname.moneypilot.domain.monthly.MonthlyRolloverProcessor
import com.yourname.moneypilot.domain.usecase.ledger.VerifyLedgerIntegrityUseCase
import com.yourname.moneypilot.ui.QuickAddActivity
import com.yourname.moneypilot.ui.features.accounts.AccountDetailsScreen
import com.yourname.moneypilot.ui.features.accounts.AccountsHubScreen
import com.yourname.moneypilot.ui.features.accounts.AddEditAccountScreen
import com.yourname.moneypilot.ui.features.accounts.WalletStatementScreen
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
import com.yourname.moneypilot.ui.features.reconciliation.ReconciliationScreen
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.activity.compose.rememberLauncherForActivityResult

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var loanAutoDeductionProcessor: LoanAutoDeductionProcessor

    @Inject
    lateinit var monthlyRolloverProcessor: MonthlyRolloverProcessor

    @Inject
    lateinit var verifyLedgerIntegrityUseCase: VerifyLedgerIntegrityUseCase

    @Inject
    lateinit var preferencesRepository: com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository

    private var biometricAuthenticated = false
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private fun isRunningUiTest(): Boolean {
        return try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (_: Throwable) {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isFromWidget = intent?.action == "ACTION_ADD_TRANSACTION"

        lifecycleScope.launch {
            val preferences = preferencesRepository.userPreferencesFlow.first()

            if (!isRunningUiTest() && preferences.useBiometrics && !isFromWidget && !biometricAuthenticated) {
                setupBiometricPrompt()
                biometricPrompt.authenticate(promptInfo)
            } else {
                proceedToContent(intent)
            }
        }
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val currentIntent = this.intent
        biometricPrompt = BiometricPrompt(this as androidx.fragment.app.FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Timber.e("Biometric error: $errString")
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    biometricAuthenticated = true
                    proceedToContent(currentIntent)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MoneyPilot Authentication")
            .setSubtitle("Verify your identity to access your finances")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    }

    private fun proceedToContent(intent: Intent?) {
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
            val useTrueBlackPref = preferences?.useTrueBlack == true

            if (!isRunningUiTest() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

            MoneyPilotTheme(
                darkTheme = darkTheme,
                trueBlack = useTrueBlackPref || isOled,
                accentColor = androidx.compose.ui.graphics.Color(preferences?.primaryColor ?: 0xFF7B5CFA.toInt()),
                fontFamilyName = preferences?.fontFamily ?: "DEFAULT"
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(intent = intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch {
            if (intent?.action == "ACTION_ADD_TRANSACTION") {
                val preferences = preferencesRepository.userPreferencesFlow.first()
                if (biometricAuthenticated || !preferences.useBiometrics) {
                    proceedToContent(intent)
                }
            }
        }
    }
}

@Composable
fun MainScreen(intent: Intent?) {
    val navController = rememberNavController()

    // Handle widget intent to navigate directly to add transaction screen
    LaunchedEffect(intent) {
        if (intent?.action == "ACTION_ADD_TRANSACTION") {
            navController.navigate("add_transaction?date=${System.currentTimeMillis()}")
        }
    }

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
                        val tag = when (screen) {
                            Screen.Transactions -> "bottom_nav_transactions"
                            Screen.Stats -> "bottom_nav_stats"
                            Screen.Accounts -> "bottom_nav_accounts"
                            Screen.Planning -> "bottom_nav_planning"
                            Screen.Settings -> "bottom_nav_settings"
                            else -> null
                        }

                        NavigationBarItem(
                            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
                            icon = { androidx.compose.material3.Icon(screen.icon, contentDescription = null) },
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            composable(Screen.Transactions.route) {
                DashboardHubScreen(
                    onAddTransaction = { date ->
                        navController.navigate("add_transaction?date=${date}")
                    },
                    onEditTransaction = { transactionId ->
                        navController.navigate("add_transaction?transactionId=$transactionId")
                    },
                    onOpenSettings = {
                        navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                    }
                )
            }

            composable(Screen.Stats.route) {
                ReportsScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable(Screen.Accounts.route) {
                AccountsHubScreen(
                    onAddAccount = { navController.navigate("add_account") },
                    onAddLoan = { navController.navigate("add_loan") },
                    onLoanClick = { loanId -> navController.navigate("loan_details/$loanId") },
                    onAccountClick = { walletId -> navController.navigate("wallet_statement/$walletId") }
                )
            }

            composable(Screen.Planning.route) {
                PlanningHubScreen(
                    onAddGoal = { navController.navigate("add_goal") },
                    onEditGoal = { id -> navController.navigate("add_goal?goalId=$id") },
                    onAddBudget = { navController.navigate("add_budget") },
                    onAddInvestment = { navController.navigate("add_investment") },
                    onAddBigBill = { navController.navigate("add_big_bill") },
                    onEditBigBill = { id -> navController.navigate("add_big_bill?bigBillId=$id") }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCategories = { navController.navigate("categories") },
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
            composable("investments") {
                InvestmentsScreen(onAddInvestment = { navController.navigate("add_investment") })
            }
            composable("appearance") { AppearanceScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("security") { SecurityScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("notifications") { NotificationsScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("diagnostics") { DiagnosticsScreen(onPopBackStack = { navController.popBackStack() }, onNavigateToReconciliation = { navController.navigate("reconciliation") }) }

            composable(
                route = "add_account?walletId={walletId}",
                arguments = listOf(navArgument("walletId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddEditAccountScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable("reconciliation") { ReconciliationScreen(onPopBackStack = { navController.popBackStack() }) }

            composable("accounts_list") {
                AccountsHubScreen(
                    onAddAccount = { navController.navigate("add_account") },
                    onAddLoan = { navController.navigate("add_loan") },
                    onLoanClick = { loanId -> navController.navigate("loan_details/$loanId") },
                    onAccountClick = { walletId -> navController.navigate("wallet_statement/$walletId") }
                )
            }

            composable("budgets_list") {
                BudgetsScreen(onAddBudget = { navController.navigate("add_budget") })
            }

            composable(
                route = "account_details/{walletId}",
                arguments = listOf(navArgument("walletId") { type = NavType.LongType })
            ) {
                AccountDetailsScreen(
                    onPopBackStack = { navController.popBackStack() },
                    onEditAccount = { id -> navController.navigate("add_account?walletId=$id") }
                )
            }

            composable(
                route = "wallet_statement/{walletId}",
                arguments = listOf(navArgument("walletId") { type = NavType.LongType })
            ) {
                WalletStatementScreen(onPopBackStack = { navController.popBackStack() })
            }

            composable(
                route = "add_transaction?date={date}&transactionId={transactionId}",
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("transactionId") { type = NavType.StringType; nullable = true; defaultValue = null }
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

            composable(
                route = "loan_details/{loanId}",
                arguments = listOf(navArgument("loanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
                LoanDetailsScreen(
                    loanId = loanId,
                    onBack = { navController.popBackStack() },
                    onEditLoan = { id -> navController.navigate("add_loan?loanId=$id") }
                )
            }

            composable("transfer") { TransferScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_budget") { AddEditBudgetScreen(onPopBackStack = { navController.popBackStack() }) }
        }
    }
}