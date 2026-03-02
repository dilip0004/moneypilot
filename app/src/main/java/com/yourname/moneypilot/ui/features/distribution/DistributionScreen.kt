package com.yourname.moneypilot.ui.features.distribution

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionScreen(
    onPopBackStack: () -> Unit,
    viewModel: DistributionViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Surplus Distribution Rules") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.addRule(
                    DistributionRuleEntity(
                        sourceWalletId = 1, 
                        targetWalletId = 2, 
                        percentage = 50.0,
                        priority = 0
                    )
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules) { rule ->
                Card {
                    ListItem(
                        headlineContent = { Text("From Wallet ${rule.sourceWalletId} to Wallet ${rule.targetWalletId}") },
                        supportingContent = { Text("Percentage: ${rule.percentage}% | Priority: ${rule.priority}") },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteRule(rule) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Rule")
                            }
                        }
                    )
                }
            }
        }
    }
}
