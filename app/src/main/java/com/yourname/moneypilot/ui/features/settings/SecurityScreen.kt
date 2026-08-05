package com.yourname.moneypilot.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onPopBackStack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinToSet by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (isPinSet) "Change PIN" else "Set PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pinToSet,
                        onValueChange = { if (it.length <= 6) pinToSet = it },
                        label = { Text("Enter PIN (4-6 digits)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        isError = errorMsg != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        isError = errorMsg != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinToSet.length !in listOf(4, 6)) {
                            errorMsg = "PIN must be 4 or 6 digits"
                            return@TextButton
                        }
                        if (pinToSet != confirmPin) {
                            errorMsg = "PINs do not match"
                            return@TextButton
                        }
                        scope.launch {
                            viewModel.setPin(pinToSet)
                            showPinDialog = false
                            pinToSet = ""
                            confirmPin = ""
                            errorMsg = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pinToSet = ""
                        confirmPin = ""
                        errorMsg = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Security & Privacy") },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            
            ListItem(
                headlineContent = { Text("Privacy Mode") },
                supportingContent = { Text("Mask balances and transaction amounts") },
                leadingContent = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = preferences?.isPrivacyModeEnabled ?: false,
                        onCheckedChange = { viewModel.updatePrivacyMode(it) }
                    )
                }
            )

            // #7.0: Configurable Net Worth Toggle
            ListItem(
                headlineContent = { Text("Include Goals in Net Worth") },
                supportingContent = { Text("Count active savings goals as assets in your total balance") },
                leadingContent = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = preferences?.includeGoalsInNetWorth ?: true,
                        onCheckedChange = { viewModel.updateIncludeGoalsInNetWorth(it) }
                    )
                }
            )

            HorizontalDivider()

            Text("App Lock", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use Biometrics")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    enabled = isPinSet,
                    checked = (preferences?.useBiometrics ?: false) && isPinSet,
                    onCheckedChange = { viewModel.updateUseBiometrics(it) }
                )
            }

            Button(
                onClick = { showPinDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Text(if (isPinSet) "Change PIN" else "Set up PIN")
                }
            }

            if (isPinSet) {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearPin()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove PIN", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
