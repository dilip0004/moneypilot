package com.yourname.moneypilot.ui.features.distribution

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionScreen(
    onPopBackStack: () -> Unit,
    viewModel: DistributionViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddRuleDialog(
            wallets = wallets,
            onDismiss = { showAddDialog = false },
            onConfirm = { rule ->
                viewModel.addRule(rule)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Surplus Distribution") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header Explanation (Section 8.0 Compliance)
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PriorityHigh, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Rules are processed monthly. Surplus (Income - Expenses) is moved from source to target wallets in order of priority.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No distribution rules defined.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules.sortedBy { it.priority }) { rule ->
                        val sourceName = wallets.find { it.id == rule.sourceWalletId }?.name ?: "Unknown"
                        val targetName = wallets.find { it.id == rule.targetWalletId }?.name ?: "Unknown"
                        
                        RuleItem(
                            rule = rule, 
                            sourceName = sourceName, 
                            targetName = targetName,
                            onDelete = { viewModel.deleteRule(rule) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleItem(rule: DistributionRuleEntity, sourceName: String, targetName: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            overlineContent = { Text("Priority ${rule.priority}") },
            headlineContent = { Text("$sourceName → $targetName", fontWeight = FontWeight.Bold) },
            supportingContent = {
                val valueText = if (rule.percentage != null) "${rule.percentage}% of Surplus" else "Fixed ₹${rule.fixedAmount}"
                Text(valueText)
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                }
            },
            leadingContent = {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (DistributionRuleEntity) -> Unit
) {
    var sourceId by remember { mutableStateOf<Long?>(wallets.firstOrNull { it.isPrimary }?.id ?: wallets.firstOrNull()?.id) }
    var targetId by remember { mutableStateOf<Long?>(null) }
    var isPercentage by remember { mutableStateOf(true) }
    var amountValue by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("0") }

    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Distribution Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Source Wallet
                ExposedDropdownMenuBox(expanded = sourceExpanded, onExpandedChange = { sourceExpanded = it }) {
                    OutlinedTextField(
                        value = wallets.find { it.id == sourceId }?.name ?: "Select Source",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source Wallet (From)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(text = { Text(wallet.name) }, onClick = { sourceId = wallet.id; sourceExpanded = false })
                        }
                    }
                }

                // Target Wallet
                ExposedDropdownMenuBox(expanded = targetExpanded, onExpandedChange = { targetExpanded = it }) {
                    OutlinedTextField(
                        value = wallets.find { it.id == targetId }?.name ?: "Select Target",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Wallet (To)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = targetExpanded, onDismissRequest = { targetExpanded = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(text = { Text(wallet.name) }, onClick = { targetId = wallet.id; targetExpanded = false })
                        }
                    }
                }

                // Type Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type: ", style = MaterialTheme.typography.bodyMedium)
                    FilterChip(
                        selected = isPercentage,
                        onClick = { isPercentage = true },
                        label = { Text("Percentage") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = !isPercentage,
                        onClick = { isPercentage = false },
                        label = { Text("Fixed Amt") }
                    )
                }

                // Value & Priority
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountValue,
                        onValueChange = { amountValue = it },
                        label = { Text(if(isPercentage) "Value (%)" else "Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = sourceId != null && targetId != null && sourceId != targetId && amountValue.toDoubleOrNull() != null,
                onClick = {
                    onConfirm(
                        DistributionRuleEntity(
                            sourceWalletId = sourceId!!,
                            targetWalletId = targetId!!,
                            percentage = if (isPercentage) amountValue.toDouble() else null,
                            fixedAmount = if (!isPercentage) amountValue.toDouble() else null,
                            priority = priority.toIntOrNull() ?: 0
                        )
                    )
                }
            ) { Text("Save Rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
