package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.features.loans.LoansScreen
import com.yourname.moneypilot.ui.features.loans.LoansViewModel
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsHubScreen(
    onAddAccount: () -> Unit,
    onAddLoan: () -> Unit,
    onLoanClick: (Long) -> Unit,
    onAccountClick: (Long) -> Unit,
    onEditAccount: (Long) -> Unit,
    accountsViewModel: AccountsViewModel = hiltViewModel(),
    loansViewModel: LoansViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Wallets", "Loans")
    var searchQuery by remember { mutableStateOf("") }
    val currencySymbol = rememberCurrencySymbol()

    val accountsState by accountsViewModel.uiState.collectAsState()
    val loansState by loansViewModel.uiState.collectAsState()

    // Calculate Summary Values
    val walletBalance = remember(accountsState) {
        (accountsState as? ScreenState.Success)?.data?.accounts
            ?.filter { it.type != "CREDIT" && it.type != "CREDIT_CARD" }
            ?.sumOf { it.currentBalance } ?: 0.0
    }
    
    val creditCardDue = remember(accountsState) {
        (accountsState as? ScreenState.Success)?.data?.accounts
            ?.filter { it.type == "CREDIT" || it.type == "CREDIT_CARD" }
            ?.sumOf { it.currentBalance } ?: 0.0
    }
    
    val loanOutstanding = remember(loansState) {
        (loansState as? ScreenState.Success)?.data?.totalBorrowed ?: 0.0
    }

    val netWorth = walletBalance + creditCardDue - loanOutstanding

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    TopAppBar(
                        title = { Text("Accounts & Debt", fontWeight = FontWeight.ExtraBold) },
                        actions = {
                            // Optional search icon if we want to toggle search bar
                        }
                    )
                    
                    PositionSummaryCard(
                        netWorth = netWorth,
                        walletBalance = walletBalance,
                        loanOutstanding = loanOutstanding,
                        creditCardDue = creditCardDue,
                        currencySymbol = currencySymbol
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search accounts or loans...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                    ) 
                                },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (selectedTabIndex == 0) onAddAccount else onAddLoan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = if (selectedTabIndex == 0) "Add Account" else "Add Loan",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "tab_switching"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> AccountsScreen(
                        onAccountClick = onAccountClick,
                        onEditAccount = onEditAccount,
                        searchQuery = searchQuery,
                        viewModel = accountsViewModel
                    )
                    1 -> LoansScreen(
                        onLoanClick = onLoanClick,
                        searchQuery = searchQuery,
                        viewModel = loansViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun PositionSummaryCard(
    netWorth: Double,
    walletBalance: Double,
    loanOutstanding: Double,
    creditCardDue: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Estimated Net Worth",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        netWorth.formatCurrency(currencySymbol),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (netWorth >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItemSmall("Wallets", walletBalance, currencySymbol, MaterialTheme.colorScheme.onPrimaryContainer)
                SummaryItemSmall("Loans", loanOutstanding, currencySymbol, MaterialTheme.colorScheme.error)
                SummaryItemSmall("CC Due", creditCardDue, currencySymbol, MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SummaryItemSmall(label: String, value: Double, symbol: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
        Text(
            value.formatCurrency(symbol),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
