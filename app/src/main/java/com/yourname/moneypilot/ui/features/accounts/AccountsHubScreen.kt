package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.features.loans.LoansScreen
import com.yourname.moneypilot.ui.features.loans.LoansViewModel
import com.yourname.moneypilot.util.formatCompact
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

    // Header Expansion State
    var isHeaderExpanded by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f && isHeaderExpanded) {
                    isHeaderExpanded = false
                } else if (available.y > 10f && !isHeaderExpanded) {
                    isHeaderExpanded = true
                }
                return Offset.Zero
            }
        }
    }

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
            Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Title and Collapsed Net Worth
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Accounts & Debt", fontWeight = FontWeight.Black, fontSize = 16.sp)
                                AnimatedVisibility(visible = !isHeaderExpanded) {
                                    Text(
                                        text = "Net Worth: ${netWorth.formatCompact(currencySymbol)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (netWorth >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    AnimatedVisibility(
                        visible = isHeaderExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        PositionSummaryCard(
                            netWorth = netWorth,
                            walletBalance = walletBalance,
                            loanOutstanding = loanOutstanding,
                            creditCardDue = creditCardDue,
                            currencySymbol = currencySymbol
                        )
                    }

                    // Compact Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(48.dp),
                        placeholder = { Text("Search accounts or loans...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )

                    // Dense TabRow
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        divider = {},
                        modifier = Modifier.height(42.dp),
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
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Bold
                                    ) 
                                },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                modifier = Modifier.padding(bottom = 12.dp), // Move up from bottom nav
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = "Add",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Estimated Net Worth",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            Text(
                netWorth.formatCurrency(currencySymbol),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = if (netWorth >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                letterSpacing = (-0.5).sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryItemCompact("Wallets", walletBalance, currencySymbol, Color(0xFF00C853))
                SummaryDivider()
                SummaryItemCompact("Loans", loanOutstanding, currencySymbol, MaterialTheme.colorScheme.error)
                SummaryDivider()
                SummaryItemCompact("CC Due", creditCardDue, currencySymbol, MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SummaryDivider() {
    Box(modifier = Modifier.size(1.dp, 16.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
}

@Composable
fun SummaryItemCompact(label: String, value: Double, symbol: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Text(
            value.formatCompact(symbol),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
    }
}
