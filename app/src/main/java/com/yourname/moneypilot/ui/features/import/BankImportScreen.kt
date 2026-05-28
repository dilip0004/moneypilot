package com.yourname.moneypilot.ui.features.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.domain.model.CsvColumnMapping
import com.yourname.moneypilot.domain.model.ImportedTransaction
import com.yourname.moneypilot.ui.theme.LocalFinanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankImportScreen(
    onPopBackStack: () -> Unit,
    viewModel: BankImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                viewModel.onEvent(BankImportEvent.FileSelected(inputStream))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank Statement Import") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.importSuccessCount != null) {
                ImportSuccessView(state.importSuccessCount!!, onPopBackStack)
            } else {
                if (state.importedTransactions.isEmpty() && !state.isLoading) {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select CSV File")
                    }
                }

                if (state.importedTransactions.isNotEmpty() || state.isLoading) {
                    MappingConfigSection(state.mapping) { newMapping ->
                        viewModel.onEvent(BankImportEvent.ColumnMappingChanged(newMapping))
                    }
                    
                    WalletSelector(state.wallets, state.selectedWalletId) {
                        viewModel.onEvent(BankImportEvent.WalletSelected(it))
                    }

                    HorizontalDivider()

                    Box(modifier = Modifier.weight(1f)) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(state.importedTransactions) { index, tx ->
                                    ImportedTransactionItem(
                                        tx = tx,
                                        categories = state.categories,
                                        onToggle = { viewModel.onEvent(BankImportEvent.ToggleTransactionSelection(index)) },
                                        onCategorySelect = { viewModel.onEvent(BankImportEvent.CategorySelected(index, it)) }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.onEvent(BankImportEvent.StartImport) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading && state.importedTransactions.any { it.isSelected },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Import ${state.importedTransactions.count { it.isSelected }} Transactions")
                    }
                }
            }
        }
    }
}

@Composable
fun MappingConfigSection(mapping: CsvColumnMapping, onMappingChange: (CsvColumnMapping) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CSV Columns (0-indexed)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MappingChip("Date", mapping.dateIndex) { onMappingChange(mapping.copy(dateIndex = it)) }
                MappingChip("Desc", mapping.descriptionIndex) { onMappingChange(mapping.copy(descriptionIndex = it)) }
                MappingChip("Amt", mapping.amountIndex) { onMappingChange(mapping.copy(amountIndex = it)) }
            }
        }
    }
}

@Composable
fun MappingChip(label: String, index: Int, onIndexChange: (Int) -> Unit) {
    var text by remember(index) { mutableStateOf(if (index == -1) "" else index.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { 
            text = it
            it.toIntOrNull()?.let { onIndexChange(it) }
        },
        label = { Text(label) },
        modifier = Modifier.width(80.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportedTransactionItem(
    tx: ImportedTransaction, 
    categories: List<CategoryEntity>,
    onToggle: () -> Unit,
    onCategorySelect: (Long) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val isExpense = (tx.parsedAmount ?: 0.0) < 0
    var expandedCategory by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == tx.categoryId }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tx.isDuplicate) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = if(tx.isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = tx.isSelected, onCheckedChange = { onToggle() }, enabled = !tx.isDuplicate)
                
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        tx.rawDescription, 
                        style = MaterialTheme.typography.bodyMedium, 
                        maxLines = 1, 
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${tx.rawDate}${if(tx.isDuplicate) " • DUPLICATE" else ""}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = if(tx.isDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    tx.rawAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isExpense) financeColors.expense else financeColors.income
                )
            }

            if (tx.isSelected) {
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.icon} ${category.name}") },
                                onClick = {
                                    onCategorySelect(category.id)
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportSuccessView(count: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00A36C), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Import Successful", style = MaterialTheme.typography.headlineSmall)
        Text("$count transactions added", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone) { Text("Finish") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSelector(
    wallets: List<com.yourname.moneypilot.data.local.database.entities.WalletEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWallet = wallets.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedWallet?.name ?: "Select Target Wallet",
            onValueChange = {},
            readOnly = true,
            label = { Text("Import To Wallet") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            wallets.forEach { wallet ->
                DropdownMenuItem(
                    text = { Text(wallet.name) },
                    onClick = {
                        onSelect(wallet.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
