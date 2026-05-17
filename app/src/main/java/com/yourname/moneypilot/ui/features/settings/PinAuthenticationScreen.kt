package com.yourname.moneypilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yourname.moneypilot.util.SecurityPreferences
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun PinAuthenticationScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PinAuthViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val verifyAndProceed = {
        if (pin.length == 4 || pin.length == 6) {
            viewModel.verifyPin(pin) { success ->
                if (success) onSuccess() else error = "Incorrect PIN"
            }
        } else {
            error = "PIN must be 4 or 6 digits"
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter PIN", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6) pin = it 
                        if (error != null) error = null // Clear error on change
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { verifyAndProceed() }
                    ),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { verifyAndProceed() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}

@HiltViewModel
class PinAuthViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences
) : ViewModel() {
    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isValid = securityPreferences.verifyPin(pin)
            onResult(isValid)
        }
    }
}
