package com.yourname.moneypilot.ui.features.reconciliation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.domain.usecase.ledger.IntegrityMismatch
import com.yourname.moneypilot.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    onPopBackStack: () -> Unit,
    viewModel: ReconciliationViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false

    LaunchedEffect(Unit) {
        viewModel.runIntegrityCheck()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ledger Integrity Check") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.mismatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("✅ All wallet balances are consistent with the transaction ledger.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            "${state.mismatches.size} wallets have inconsistent balances. This can happen due to incomplete operations. You can repair them below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(state.mismatches) { mismatch ->
                        MismatchItem(
                            mismatch = mismatch,
                            isPrivacyMode = isPrivacyMode,
                            onRepair = { viewModel.repairMismatch(mismatch) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MismatchItem(mismatch: IntegrityMismatch, isPrivacyMode: Boolean, onRepair: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(mismatch.walletName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val storedLabel = if (isPrivacyMode) "••••" else mismatch.storedBalance.toString()
            val calculatedLabel = if (isPrivacyMode) "••••" else mismatch.calculatedBalance.toString()
            
            Text("Stored Balance: $storedLabel", color = MaterialTheme.colorScheme.error)
            Text("Calculated Balance: $calculatedLabel", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRepair) {
                Text("Create Adjustment Transaction")
            }
        }
    }
}
