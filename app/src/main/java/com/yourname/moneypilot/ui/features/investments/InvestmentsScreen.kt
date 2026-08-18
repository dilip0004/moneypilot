package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onAddInvestment: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    
    var showBuyDialog by remember { mutableStateOf<InvestmentEntity?>(null) }
    var showSellDialog by remember { mutableStateOf<InvestmentEntity?>(null) }

    // Buy Dialog
    if (showBuyDialog != null) {
        val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()
        InvestmentActionDialog(
            title = "Buy more ${showBuyDialog!!.name}",
            confirmLabel = "Buy",
            wallets = wallets,
            onDismiss = { showBuyDialog = null },
            onConfirm = { amount, walletId ->
                viewModel.buyAsset(showBuyDialog!!.id, amount, walletId)
                showBuyDialog = null
            }
        )
    }

    // Sell Dialog
    if (showSellDialog != null) {
        val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()
        InvestmentActionDialog(
            title = "Sell ${showSellDialog!!.name}",
            confirmLabel = "Sell",
            wallets = wallets,
            onDismiss = { showSellDialog = null },
            onConfirm = { amount, walletId ->
                viewModel.sellAsset(showSellDialog!!.id, amount, walletId)
                showSellDialog = null
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddInvestment) {
                Icon(Icons.Default.Add, contentDescription = "Add Investment")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ScreenState.Success -> {
                    InvestmentList(
                        data = state.data, 
                        isPrivacyMode = isPrivacyMode,
                        onBuy = { showBuyDialog = it },
                        onSell = { showSellDialog = it }
                    )
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No investments tracked yet. Start building your portfolio!")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun InvestmentList(
    data: InvestmentState, 
    isPrivacyMode: Boolean,
    onBuy: (InvestmentEntity) -> Unit,
    onSell: (InvestmentEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            PortfolioHeroCard(data.totalValue, data.totalGain, data.gainPercentage, isPrivacyMode)
        }
        
        item {
            Text("Your Holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(data.investments) { investment ->
            InvestmentItem(investment, isPrivacyMode, onBuy, onSell)
        }
    }
}

@Composable
fun InvestmentItem(
    investment: InvestmentEntity, 
    isPrivacyMode: Boolean,
    onBuy: (InvestmentEntity) -> Unit,
    onSell: (InvestmentEntity) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val totalHoldings = when (investment.type) {
        "STOCKS", "CRYPTO", "GOLD" -> investment.quantity * investment.currentPrice
        else -> investment.currentPrice
    }
    val totalInvested = when (investment.type) {
        "STOCKS", "CRYPTO", "GOLD" -> investment.quantity * investment.averagePrice
        else -> investment.averagePrice
    }
    val totalGain = totalHoldings - totalInvested
    val isProfit = totalGain >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), // Slightly more compact
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = investment.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
                    val subLabel = when(investment.type) {
                        "STOCKS", "CRYPTO", "GOLD" -> {
                            val qtyLabel = if(isPrivacyMode) "••••" else String.format("%.2f", investment.quantity)
                            val priceLabel = if(isPrivacyMode) "••••" else String.format("%.2f", investment.averagePrice)
                            "$qtyLabel units @ ₹$priceLabel"
                        }
                        "FD", "RD" -> {
                            "Maturity tracking active"
                        }
                        "SIP" -> {
                            "Recurring contribution"
                        }
                        else -> investment.type
                    }
                    
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if(isPrivacyMode) "••••" else "₹ ${String.format("%.0f", totalHoldings)}", 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black
                    )
                    if (totalGain != 0.0) {
                        Text(
                            text = if(isPrivacyMode) "••••" else "${if (isProfit) "+" else ""}₹${totalGain.toInt()}",
                            color = if (isProfit) financeColors.income else financeColors.expense,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onBuy(investment) },
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if(investment.type == "STOCKS" || investment.type == "CRYPTO") "Buy More" else "Add Contribution", fontSize = 11.sp)
                }
                
                OutlinedButton(
                    onClick = { onSell(investment) },
                    modifier = Modifier.weight(0.5f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text("Exit / Sell", fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentActionDialog(
    title: String,
    confirmLabel: String,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedWalletId by remember(wallets) { mutableStateOf<Long?>(wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Transaction Value (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    val walletName = wallets.find { it.id == selectedWalletId }?.name ?: "Select Wallet"
                    OutlinedTextField(
                        value = walletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        wallets.forEach { wallet ->
                            DropdownMenuItem(
                                text = { Text(wallet.name) },
                                onClick = {
                                    selectedWalletId = wallet.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    val walletId = selectedWalletId
                    if (amount != null && amount > 0 && walletId != null) {
                        onConfirm(amount, walletId)
                    }
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PortfolioHeroCard(totalValue: Double, gain: Double, gainPct: Double, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Portfolio Value", style = MaterialTheme.typography.labelMedium)
            Text(
                text = if(isPrivacyMode) "••••" else "₹ ${String.format("%.0f", totalValue)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isProfit = gain >= 0
                Icon(
                    imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isProfit) financeColors.income else financeColors.expense,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val gainText = if(isPrivacyMode) "••••" else "${if (isProfit) "+" else ""}₹${gain.toInt()} (${String.format("%.1f", gainPct)}%)"
                Text(
                    text = gainText,
                    color = if (isProfit) financeColors.income else financeColors.expense,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
