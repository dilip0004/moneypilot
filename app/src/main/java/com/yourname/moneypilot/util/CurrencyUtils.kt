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
        return normalizeSymbol(currency)
    }

    fun getCurrencySymbolBlocking(): String {
        return runBlocking { getCurrencySymbol() }
    }
    
    private fun normalizeSymbol(currency: String): String {
        return when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "INR" -> "₹"
            else -> currency.take(1)
        }
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
    val displaySymbol = if (symbol == "INR") "₹" else symbol
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val formatted = formatter.format(this).replace("₹", displaySymbol)
    
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

/**
 * Formats large numbers into compact strings like 1.2L or 18K.
 */
fun Double.formatCompact(symbol: String): String {
    val displaySymbol = if (symbol == "INR") "₹" else symbol
    val absVal = kotlin.math.abs(this)
    val locale = Locale("en", "IN")
    return when {
        absVal >= 100000 -> "${if(this < 0) "-" else ""}$displaySymbol${String.format(locale, "%.1f", absVal / 100000)}L"
        absVal >= 1000 -> "${if(this < 0) "-" else ""}$displaySymbol${String.format(locale, "%.0f", absVal / 1000)}K"
        else -> this.formatCurrency(displaySymbol)
    }
}
