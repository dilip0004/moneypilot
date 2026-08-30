package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import com.yourname.moneypilot.ui.components.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.features.transactions.components.glassTextFieldColors
import com.yourname.moneypilot.ui.components.AppDateTimePickerField
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onPopBackStack: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val snackbarHostState = remember { SnackbarHostState() }

    var fromWalletExpanded by remember { mutableStateOf(false) }
    var toWalletExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is TransferViewModel.UiEvent.TransferSuccess -> onPopBackStack()
                is TransferViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassTopBar(
                title = { Text("Fund Transfer") },
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
            // #37: Added Date/Time selection
            AppDateTimePickerField(
                label = "Transfer Date & Time",
                value = state.dateTime,
                onChange = { viewModel.onEvent(TransferEvent.DateChanged(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            // From Wallet Dropdown
            ExposedDropdownMenuBox(
                expanded = fromWalletExpanded,
                onExpandedChange = { fromWalletExpanded = !fromWalletExpanded }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.fromWalletId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("From Wallet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromWalletExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = fromWalletExpanded,
                    onDismissRequest = { fromWalletExpanded = false }
                ) {
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                viewModel.onEvent(TransferEvent.FromWalletChanged(wallet.id))
                                fromWalletExpanded = false
                            }
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Transfer Icon",
                modifier = Modifier.align(Alignment.CenterHorizontally).size(32.dp),
                tint = Color.White
            )

            // To Wallet Dropdown
            ExposedDropdownMenuBox(
                expanded = toWalletExpanded,
                onExpandedChange = { toWalletExpanded = !toWalletExpanded }
            ) {
                OutlinedTextField(
                    value = state.wallets.find { it.id == state.toWalletId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To Wallet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toWalletExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = glassTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = toWalletExpanded,
                    onDismissRequest = { toWalletExpanded = false }
                ) {
                    state.wallets.forEach { wallet ->
                        DropdownMenuItem(
                            text = { Text(wallet.name) },
                            onClick = {
                                viewModel.onEvent(TransferEvent.ToWalletChanged(wallet.id))
                                toWalletExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onEvent(TransferEvent.EnteredAmount(it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = glassTextFieldColors()
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(TransferEvent.EnteredDescription(it)) },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = glassTextFieldColors()
            )

            Button(
                onClick = { viewModel.onEvent(TransferEvent.PerformTransfer) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Transfer Funds")
            }
        }
    }
}
