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
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXForward
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXBackward
import com.yourname.moneypilot.ui.theme.motion.FadeThroughTransition
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.moneypilot.ui.components.MoneyPilotBackground
import com.yourname.moneypilot.ui.components.MoneyPilotBottomNavigation
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
                val isAtmospheric = preferences?.appBackground != "NONE"
                val darkTheme = if (isAtmospheric) true else when (preferences?.theme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.OLED -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                    else -> isSystemInDarkTheme()
                }

                MoneyPilotTheme(
                    darkTheme = darkTheme,
                    trueBlack = preferences?.useTrueBlack == true || preferences?.theme == AppTheme.OLED,
                    accentColor = Color(preferences?.primaryColor ?: 0xFF7B5CFA.toInt())
                ) {
                    MoneyPilotBackground(
                        backgroundType = preferences?.appBackground ?: "DEFAULT",
                        customUri = preferences?.customBackgroundUri
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                            MainScreen(intent = intent, mainViewModel = mainViewModel)
                        }
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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Main Content Area
            NavHost(
                navController = navController,
                startDestination = Screen.Transactions.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val from = initialState.destination.route
                    val to = targetState.destination.route
                    val fromIndex = navItems.indexOfFirst { it.route == from }
                    val toIndex = navItems.indexOfFirst { it.route == to }

                    if (fromIndex != -1 && toIndex != -1) {
                        if (toIndex > fromIndex) {
                            slideInHorizontally(animationSpec = tween(MotionConstants.DurationScreen)) { it / 10 } + fadeIn(animationSpec = tween(MotionConstants.DurationScreen))
                        } else {
                            slideInHorizontally(animationSpec = tween(MotionConstants.DurationScreen)) { -it / 10 } + fadeIn(animationSpec = tween(MotionConstants.DurationScreen))
                        }
                    } else {
                        fadeIn(animationSpec = tween(MotionConstants.DurationScreen)) + scaleIn(initialScale = 0.98f)
                    }
                },
                exitTransition = {
                    val from = initialState.destination.route
                    val to = targetState.destination.route
                    val fromIndex = navItems.indexOfFirst { it.route == from }
                    val toIndex = navItems.indexOfFirst { it.route == to }

                    if (fromIndex != -1 && toIndex != -1) {
                        if (toIndex > fromIndex) {
                            slideOutHorizontally(animationSpec = tween(MotionConstants.DurationScreen)) { -it / 10 } + fadeOut(animationSpec = tween(MotionConstants.DurationScreen))
                        } else {
                            slideOutHorizontally(animationSpec = tween(MotionConstants.DurationScreen)) { it / 10 } + fadeOut(animationSpec = tween(MotionConstants.DurationScreen))
                        }
                    } else {
                        fadeOut(animationSpec = tween(MotionConstants.DurationScreen))
                    }
                }
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
                composable(Screen.Planning.route) { 
                    PlanningHubScreen(
                        onAddGoal = { navController.navigate("add_goal") }, 
                        onEditGoal = { navController.navigate("add_goal?goalId=$it") }, 
                        onGoalClick = { navController.navigate("goal_statement/$it") }, 
                        onAddBudget = { month -> 
                            val route = if (month != null) "add_budget?month=$month" else "add_budget"
                            navController.navigate(route) 
                        },
                        onEditBudget = { id, month ->
                            val route = if (month != null) "add_budget?budgetId=$id&month=$month" else "add_budget?budgetId=$id"
                            navController.navigate(route)
                        },
                        onAddInvestment = { navController.navigate("add_investment") }, 
                        onAddPlannedExpense = { navController.navigate("add_big_bill") }, 
                        onEditBigBill = { navController.navigate("add_big_bill?bigBillId=$it") },
                        onInvestmentClick = { navController.navigate("investment_details/$it") }
                    ) 
                }
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
                composable("investment_details/{investmentId}", arguments = listOf(navArgument("investmentId") { type = NavType.LongType })) {
                    InvestmentDetailsScreen(
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("add_investment?investmentId=$it") }
                    )
                }
                composable("add_big_bill?bigBillId={bigBillId}", arguments = listOf(navArgument("bigBillId") { type = NavType.LongType; defaultValue = -1L })) { AddEditPlannedExpenseScreen(onPopBackStack = { navController.popBackStack() }) }
                composable("add_loan?loanId={loanId}", arguments = listOf(navArgument("loanId") { type = NavType.LongType; defaultValue = -1L })) { AddEditLoanScreen(onPopBackStack = { navController.popBackStack() }) }
                composable("transfer") { TransferScreen(onPopBackStack = { navController.popBackStack() }) }
                composable(
                    "add_budget?budgetId={budgetId}&month={month}",
                    arguments = listOf(
                        navArgument("budgetId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("month") { type = NavType.StringType; nullable = true }
                    )
                ) { backStackEntry ->
                    val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: -1L
                    val month = backStackEntry.arguments?.getString("month")
                    AddEditBudgetScreen(
                        budgetId = budgetId,
                        month = month,
                        onPopBackStack = { navController.popBackStack() }
                    )
                }
            }

            // Floating Bottom Navigation Overlay
            val showBottomBar = navItems.any { it.route == currentDestination?.route }
            if (showBottomBar) {
                MoneyPilotBottomNavigation(
                    navItems = navItems,
                    currentDestination = currentDestination,
                    onItemSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
