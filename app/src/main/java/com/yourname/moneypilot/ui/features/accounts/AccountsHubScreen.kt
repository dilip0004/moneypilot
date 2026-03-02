package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.moneypilot.ui.features.loans.LoansScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsHubScreen(
    onAddAccount: () -> Unit,
    onAddLoan: () -> Unit,
    onLoanClick: (Long) -> Unit,
    onAccountClick: (Long) -> Unit // Added
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Wallets", "Loans")

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column {
                    TopAppBar(
                        title = { Text("Accounts & Debt", fontWeight = FontWeight.Bold) }
                    )
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTabIndex) {
                0 -> AccountsScreen(onAddAccount = onAddAccount, onAccountClick = onAccountClick) // Updated
                1 -> LoansScreen(onLoanClick = onLoanClick, onAddLoan = onAddLoan)
            }
        }
    }
}
