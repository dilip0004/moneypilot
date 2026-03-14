package com.yourname.moneypilot.ui.features.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    onPopBackStack: () -> Unit,
    onEditAccount: (Long) -> Unit, // Added
    viewModel: AccountDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.wallet?.name ?: "Wallet Details") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.wallet?.let { wallet ->
                        IconButton(onClick = { onEditAccount(wallet.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Account")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
            } else if (state.wallet != null) {
                val wallet = state.wallet!!

                Text("Current Balance: ₹ ${wallet.currentBalance}", style = MaterialTheme.typography.headlineSmall)
                Text("Type: ${wallet.type}", style = MaterialTheme.typography.bodyLarge)

                if (wallet.type == "CREDIT_CARD") {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Credit Card Details", style = MaterialTheme.typography.titleMedium)
                    wallet.creditLimit?.let { Text("Credit Limit: ₹ $it") }
                    wallet.billingStartDay?.let { Text("Billing Cycle Start Day: $it") }
                    wallet.billingEndDay?.let { Text("Billing Cycle End Day: $it") }
                    wallet.dueDate?.let { Text("Payment Due Day: $it") }
                }
            }
        }
    }
}
