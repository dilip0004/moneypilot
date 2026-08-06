package com.yourname.moneypilot.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton
import java.text.NumberFormat
import java.util.Locale

@Singleton
class CurrencyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferencesRepository = UserPreferencesRepository(context)

    suspend fun getCurrencySymbol(): String {
        val currency = preferencesRepository.userPreferencesFlow.first().currency
        return when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> "₹"
        }
    }

    fun getCurrencySymbolBlocking(): String {
        return runBlocking { getCurrencySymbol() }
    }
}

@Composable
fun rememberCurrencySymbol(): String {
    val currencyManager = hiltViewModel<CurrencyManagerViewModel>()
    val currency by currencyManager.currency.collectAsState()
    return currency
}

@HiltViewModel
class CurrencyManagerViewModel @Inject constructor(
    private val currencyManager: CurrencyManager
) : ViewModel() {
    private val _currency = MutableStateFlow("₹")
    val currency: StateFlow<String> = _currency

    init {
        viewModelScope.launch {
            _currency.value = currencyManager.getCurrencySymbol()
        }
    }
}

fun Double.formatCurrency(symbol: String, hideDecimals: Boolean = true): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val formatted = formatter.format(this).replace("₹", symbol)
    
    return if (hideDecimals && this % 1.0 == 0.0) {
        val parts = formatted.split(".")
        if (parts.size > 1) {
            parts[0]
        } else {
            formatted
        }
    } else {
        formatted
    }
}
