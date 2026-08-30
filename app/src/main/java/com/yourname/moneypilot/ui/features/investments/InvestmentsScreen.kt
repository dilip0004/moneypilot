package com.yourname.moneypilot.ui.features.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.yourname.moneypilot.ui.components.*
import com.yourname.moneypilot.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onAddInvestment: () -> Unit,
    onInvestmentClick: (Long) -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    
    var showBuyDialog by remember { mutableStateOf<InvestmentEntity?>(null) }
    var showSellDialog by remember { mutableStateOf<InvestmentEntity?>(null) }

    val wallets = (uiState as? ScreenState.Success)?.data?.wallets ?: emptyList()

    // Buy Dialog (Refined to use dynamic labeling)
    if (showBuyDialog != null) {
        val investment = showBuyDialog!!
        val actionLabel = when(investment.type) {
            "STOCKS", "CRYPTO" -> "Buy More"
            else -> "Add Contribution"
        }
        InvestmentActionDialog(
            title = "$actionLabel - ${investment.name}",
            confirmLabel = if(investment.type == "STOCKS" || investment.type == "CRYPTO") "Buy" else "Add",
            wallets = wallets,
            onDismiss = { showBuyDialog = null },
            onConfirm = { amount, walletId ->
                viewModel.buyAsset(investment.id, amount, walletId)
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
        containerColor = Color.Transparent,
        floatingActionButton = {
            MoneyPilotFAB(
                onClick = onAddInvestment,
                icon = Icons.Default.Add,
                label = "New Investment",
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("investment_add_fab")
            )
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
                        onSell = { showSellDialog = it },
                        onInvestmentClick = onInvestmentClick
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
    onSell: (InvestmentEntity) -> Unit,
    onInvestmentClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            PortfolioHeroCard(data, isPrivacyMode)
        }
        
        if (data.investments.isNotEmpty()) {
            item {
                Text("Your Holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(data.investments, key = { it.id }) { investment ->
                InvestmentItem(investment, isPrivacyMode, onBuy, onSell, onInvestmentClick)
            }
        }

        if (data.recentActivity.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Investment Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(data.recentActivity) { activity ->
                InvestmentActivityItem(activity, isPrivacyMode)
            }
        }
    }
}

@Composable
fun InvestmentItem(
    investment: InvestmentEntity, 
    isPrivacyMode: Boolean,
    onBuy: (InvestmentEntity) -> Unit,
    onSell: (InvestmentEntity) -> Unit,
    onInvestmentClick: (Long) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    
    // Fallback for legacy records
    val effectiveQty = if (investment.quantity == 0.0 && investment.type != "SIP") {
        try {
            val extra = investment.extraData?.let { kotlinx.serialization.json.Json.decodeFromString<InvestmentExtraData>(it) }
            extra?.currentBalance ?: extra?.purchaseValue ?: 0.0
        } catch (e: Exception) { 0.0 }
    } else investment.quantity

    val totalHoldings = effectiveQty * investment.currentPrice
    val totalInvested = effectiveQty * investment.averagePrice
    val totalGain = totalHoldings - totalInvested
    val isProfit = totalGain >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInvestmentClick(investment.id) },
        shape = RoundedCornerShape(12.dp),
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
fun PortfolioHeroCard(data: InvestmentState, isPrivacyMode: Boolean) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Portfolio Value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = if(isPrivacyMode) "••••" else "₹ ${String.format("%.0f", data.totalValue)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if(isPrivacyMode) "••••" else "₹ ${String.format("%.0f", data.totalInvested)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                VerticalDivider(modifier = Modifier.height(32.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val isProfit = data.totalGain >= 0
                    Text("Total Gain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if(isPrivacyMode) "••••" else "${if (isProfit) "+" else ""}₹${data.totalGain.toInt()}",
                        color = if (isProfit) financeColors.income else financeColors.expense,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InvestmentActivityItem(activity: com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails, isPrivacyMode: Boolean) {
    val tx = activity.transaction
    val financeColors = LocalFinanceColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when(tx.transactionSourceType) {
                    "INVESTMENT_BUY" -> Icons.Default.Add
                    "INVESTMENT_SELL" -> Icons.Default.Remove
                    else -> Icons.Default.Payment
                }
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.investment?.name ?: "Investment",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tx.dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = if(isPrivacyMode) "••••" else "₹ ${tx.amount.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = if (tx.type == com.yourname.moneypilot.data.local.database.entities.TransactionType.Income) financeColors.income else financeColors.expense
        )
    }
}
