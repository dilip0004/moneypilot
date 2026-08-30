package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.components.GlassTopBar
import com.yourname.moneypilot.ui.components.FinancialSummarySurface
import com.yourname.moneypilot.ui.components.MoneyPilotFAB
import com.yourname.moneypilot.ui.components.MoneyPilotListItem
import com.yourname.moneypilot.ui.components.MoneyPilotSegmentedControl
import com.yourname.moneypilot.ui.components.SectionHeader
import com.yourname.moneypilot.ui.components.SummaryItem
import com.yourname.moneypilot.ui.features.loans.LoansScreen
import com.yourname.moneypilot.ui.features.loans.LoansViewModel
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXBackward
import com.yourname.moneypilot.ui.theme.motion.SharedAxisXForward
import com.yourname.moneypilot.ui.theme.motion.motionTween
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
        containerColor = Color.Transparent,
        topBar = {
            GlassTopBar(
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
                }
            )
        },
        floatingActionButton = {
            MoneyPilotFAB(
                onClick = if (selectedTabIndex == 0) onAddAccount else onAddLoan,
                icon = Icons.Default.Add,
                label = if (selectedTabIndex == 0) "Add Wallet" else "Add Loan",
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
                        SharedAxisXForward
                    } else {
                        SharedAxisXBackward
                    }.using(SizeTransform(clip = false))
                },
                label = "tab_switching"
            ) { targetIndex ->
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isHeaderExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        FinancialSummarySurface(
                            title = "Estimated Net Worth",
                            primaryValue = netWorth.formatCurrency(currencySymbol),
                            progress = if(walletBalance > 0) (netWorth / walletBalance).toFloat() else 0f,
                            progressColor = if (netWorth >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            secondaryInfo = {
                                SummaryItem(label = "Wallets", value = walletBalance.formatCompact(currencySymbol), color = Color(0xFF00C853))
                                SummaryItem(label = "Debt", value = (loanOutstanding + creditCardDue).formatCompact(currencySymbol), color = MaterialTheme.colorScheme.error)
                            }
                        )
                    }

                    // Dense Control Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).height(44.dp),
                            placeholder = { Text("Search...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )

                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.Transparent,
                            divider = {},
                            modifier = Modifier.width(160.dp).height(40.dp),
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                        color = MaterialTheme.colorScheme.primary,
                                        height = 2.dp
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
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
    }
}
