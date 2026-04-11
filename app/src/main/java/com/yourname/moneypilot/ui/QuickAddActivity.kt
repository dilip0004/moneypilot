package com.yourname.moneypilot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionScreen
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionViewModel
import com.yourname.moneypilot.ui.theme.MoneyPilotTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyPilotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickAddWrapper(onFinish = { finish() })
                }
            }
        }
    }
}

@Composable
fun QuickAddWrapper(onFinish: () -> Unit) {
    val viewModel: AddEditTransactionViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            if (event is AddEditTransactionViewModel.UiEvent.SaveTransaction) {
                onFinish()
            }
        }
    }
    AddEditTransactionScreen(
        onPopBackStack = onFinish,
        onNavigateToTransfer = { /* No transfer needed in quick add */ }
    )
}