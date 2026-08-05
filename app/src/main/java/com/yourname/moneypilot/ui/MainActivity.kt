package com.yourname.moneypilot.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.yourname.moneypilot.ui.features.accounts.*
import com.yourname.moneypilot.ui.features.backup.BackupScreen
import com.yourname.moneypilot.ui.features.budgets.*
import com.yourname.moneypilot.ui.features.categories.CategoryManagerScreen
import com.yourname.moneypilot.ui.features.dashboard.DashboardHubScreen
import com.yourname.moneypilot.ui.features.distribution.DistributionScreen
import com.yourname.moneypilot.ui.features.goals.*
import com.yourname.moneypilot.ui.features.import.BankImportScreen
import com.yourname.moneypilot.ui.features.investments.*
import com.yourname.moneypilot.ui.features.loans.*
import com.yourname.moneypilot.ui.features.planning.*
import com.yourname.moneypilot.ui.features.reconciliation.ReconciliationScreen
import com.yourname.moneypilot.ui.features.reports.ReportsScreen
import com.yourname.moneypilot.ui.features.settings.*
import com.yourname.moneypilot.ui.features.transactions.*
import com.yourname.moneypilot.ui.features.webapp.WebAppAccessScreen
import com.yourname.moneypilot.ui.navigation.Screen
import com.yourname.moneypilot.ui.theme.MoneyPilotTheme
import com.yourname.moneypilot.util.SecurityPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var preferencesRepository: com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
    @Inject lateinit var securityPreferences: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MoneyPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        checkAuthAndProceed()
    }

    private fun checkAuthAndProceed() {
        lifecycleScope.launch {
            try {
                val preferences = preferencesRepository.userPreferencesFlow.first()
                val hasPin = securityPreferences.isPinSet()
                val isFromWidget = intent?.action == "ACTION_ADD_TRANSACTION"

                if (hasPin && !isFromWidget && !isRunningUiTest()) {
                    val biometricManager = BiometricManager.from(this@MainActivity)
                    val canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

                    if (preferences.useBiometrics && canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                        showBiometricPrompt()
                    } else {
                        showPinAuthScreen()
                    }
                } else {
                    proceedToContent()
                }
            } catch (e: Exception) {
                Timber.e(e, "Auth flow failed")
                proceedToContent()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || 
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT) {
                        showPinAuthScreen()
                    } else {
                        finish()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    proceedToContent()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MoneyPilot")
            .setSubtitle("Unlock your financial data")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showPinAuthScreen() {
        setContent {
            MoneyPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PinAuthenticationScreen(
                        onSuccess = { proceedToContent() },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun proceedToContent() {
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val preferences by mainViewModel.userPreferences.collectAsState()

            if (preferences == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val darkTheme = when (preferences?.theme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.OLED -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                    else -> isSystemInDarkTheme()
                }

                MoneyPilotTheme(
                    darkTheme = darkTheme,
                    trueBlack = preferences?.useTrueBlack == true || preferences?.theme == AppTheme.OLED,
                    accentColor = androidx.compose.ui.graphics.Color(preferences?.primaryColor ?: 0xFF7B5CFA.toInt())
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainScreen(intent = intent, mainViewModel = mainViewModel)
                    }
                }
            }
        }
    }

    private fun isRunningUiTest(): Boolean {
        return try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (_: Throwable) {
            false
        }
    }
}

@Composable
fun MainScreen(intent: Intent?, mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        mainViewModel.integrityAlert.collectLatest { mismatchCount ->
            val result = snackbarHostState.showSnackbar(
                message = "Ledger Integrity Warning: $mismatchCount wallets have drifted balances.",
                actionLabel = "Repair",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                navController.navigate("reconciliation")
            }
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        }

                        NavigationBarItem(
                            modifier = Modifier.testTag(tag),
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
        // ARCHITECTURE FIX: Apply bottom padding from Scaffold (for NavigationBar) 
        // but ignore top padding as each screen handles its own status bar insets.
        NavHost(
            navController = navController,
            startDestination = Screen.Transactions.route,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            composable(Screen.Transactions.route) { 
                DashboardHubScreen(
                    onAddTransaction = { navController.navigate("add_transaction?date=$it") }, 
                    onEditTransaction = { navController.navigate("add_transaction?transactionId=$it") }, 
                    onOpenSettings = { navController.navigate(Screen.Settings.route) }
                ) 
            }
            composable(Screen.Stats.route) { ReportsScreen(onPopBackStack = { navController.popBackStack() }) }
            composable(Screen.Accounts.route) { AccountsHubScreen(onAddAccount = { navController.navigate("add_account") }, onAddLoan = { navController.navigate("add_loan") }, onLoanClick = { navController.navigate("loan_details/$it") }, onAccountClick = { navController.navigate("wallet_statement/$it") }, onEditAccount = { navController.navigate("add_account?walletId=$it") }) }
            composable(Screen.Planning.route) { PlanningHubScreen(onAddGoal = { navController.navigate("add_goal") }, onEditGoal = { navController.navigate("add_goal?goalId=$it") }, onGoalClick = { navController.navigate("goal_statement/$it") }, onAddBudget = { navController.navigate("add_budget") }, onAddInvestment = { navController.navigate("add_investment") }, onAddBigBill = { navController.navigate("add_big_bill") }, onEditBigBill = { navController.navigate("add_big_bill?bigBillId=$it") }) }
            composable(Screen.Settings.route) { SettingsScreen(onNavigateToCategories = { navController.navigate("categories") }, onNavigateToAppearance = { navController.navigate("appearance") }, onNavigateToSecurity = { navController.navigate("security") }, onNavigateToNotifications = { navController.navigate("notifications") }, onNavigateToBackup = { navController.navigate("backup") }, onNavigateToDiagnostics = { navController.navigate("diagnostics") }, onNavigateToWebApp = { navController.navigate("webapp_access") } ) }
            
            composable("backup") { BackupScreen(onPopBackStack = { navController.popBackStack() }, onNavigateToImport = { navController.navigate("bank_import") }) }
            composable("bank_import") { BankImportScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("categories") { CategoryManagerScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("appearance") { AppearanceScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("security") { SecurityScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("notifications") { NotificationsScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("diagnostics") { DiagnosticsScreen(onPopBackStack = { navController.popBackStack() }, onNavigateToReconciliation = { navController.navigate("reconciliation") }) }
            composable("reconciliation") { ReconciliationScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("webapp_access") { WebAppAccessScreen(onPopBackStack = { navController.popBackStack() }) }
            
            composable("add_account?walletId={walletId}", arguments = listOf(navArgument("walletId") { type = NavType.LongType; defaultValue = -1L })) { AddEditAccountScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("loan_details/{loanId}", arguments = listOf(navArgument("loanId") { type = NavType.LongType })) { LoanDetailsScreen(loanId = it.arguments?.getLong("loanId") ?: 0L, onBack = { navController.popBackStack() }, onEditLoan = { navController.navigate("add_loan?loanId=$it") }) }
            composable("wallet_statement/{walletId}", arguments = listOf(navArgument("walletId") { type = NavType.LongType })) { WalletStatementScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("goal_statement/{goalId}", arguments = listOf(navArgument("goalId") { type = NavType.LongType })) { GoalStatementScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_transaction?date={date}&transactionId={transactionId}", arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null }, navArgument("transactionId") { type = NavType.StringType; nullable = true; defaultValue = null })) { AddEditTransactionScreen(onPopBackStack = { navController.popBackStack() }, onNavigateToTransfer = { navController.navigate("transfer") }) }
            composable("add_goal?goalId={goalId}", arguments = listOf(navArgument("goalId") { type = NavType.LongType; defaultValue = -1L })) { AddEditGoalScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_investment?investmentId={investmentId}", arguments = listOf(navArgument("investmentId") { type = NavType.LongType; defaultValue = -1L })) { AddEditInvestmentScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_big_bill?bigBillId={bigBillId}", arguments = listOf(navArgument("bigBillId") { type = NavType.LongType; defaultValue = -1L })) { AddEditBigBillScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_loan?loanId={loanId}", arguments = listOf(navArgument("loanId") { type = NavType.LongType; defaultValue = -1L })) { AddEditLoanScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("transfer") { TransferScreen(onPopBackStack = { navController.popBackStack() }) }
            composable("add_budget") { AddEditBudgetScreen(onPopBackStack = { navController.popBackStack() }) }
        }
    }
}
