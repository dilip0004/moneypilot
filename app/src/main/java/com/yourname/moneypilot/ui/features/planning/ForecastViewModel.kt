package com.yourname.moneypilot.ui.features.planning

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.repository.*
import com.yourname.moneypilot.domain.model.ForecastResult
import com.yourname.moneypilot.domain.usecase.analytics.CalculateKeyAnalyticsUseCase
import com.yourname.moneypilot.domain.usecase.analytics.CalculateWalletProjectionsUseCase
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class ForecastState(
    val primaryWalletName: String = "",
    val forecast: ForecastResult? = null,
    val dailyVelocity: Double = 0.0
)

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val bigBillRepository: BigBillRepository,
    private val transactionRepository: TransactionRepository,
    private val calculateProjectionsUseCase: CalculateWalletProjectionsUseCase,
    private val calculateKeyAnalyticsUseCase: CalculateKeyAnalyticsUseCase
) : BaseViewModel<ForecastState>() {

    init {
        loadForecast()
    }

    private fun loadForecast() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading

            // 1. Get Primary Wallet
            val wallets = walletRepository.getAllWallets().first()
            val primaryWallet = wallets.find { it.isPrimary } ?: wallets.firstOrNull()
            
            if (primaryWallet == null) {
                _uiState.value = ScreenState.Empty
                return@launch
            }

            // 2. Calculate Velocity (Last 30 days)
            val end = LocalDateTime.now()
            val start = end.minusDays(30)
            val recentTxs = transactionRepository.getTransactionsWithDetailsByDateRange(start, end).first()
            val analytics = calculateKeyAnalyticsUseCase(recentTxs, 30)
            val velocity = analytics.expenseVelocity ?: 0.0

            // 3. Get Upcoming Bills
            val unpaidBills = bigBillRepository.getUnpaidBigBills().first()

            // 4. Calculate Projections
            val result = calculateProjectionsUseCase(
                wallet = primaryWallet,
                unpaidBills = unpaidBills,
                avgDailyVelocity = velocity
            )

            _uiState.value = ScreenState.Success(
                ForecastState(
                    primaryWalletName = primaryWallet.name,
                    forecast = result,
                    dailyVelocity = velocity
                )
            )
        }
    }
}
