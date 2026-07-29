package com.yourname.moneypilot.ui.features.investments

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.InvestmentRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class InvestmentState(
    val investments: List<InvestmentEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val totalValue: Double = 0.0,
    val totalGain: Double = 0.0,
    val gainPercentage: Double = 0.0
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel<InvestmentState>() {

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            combine(
                investmentRepository.getAllInvestments(),
                walletRepository.getAllWallets()
            ) { investments, wallets ->
                val totalValue = investments.sumOf { it.quantity * it.currentPrice }
                val totalCost = investments.sumOf { it.quantity * it.averagePrice }
                val totalGain = totalValue - totalCost
                val gainPct = if (totalCost > 0) (totalGain / totalCost) * 100 else 0.0
                
                InvestmentState(
                    investments = investments,
                    wallets = wallets,
                    totalValue = totalValue,
                    totalGain = totalGain,
                    gainPercentage = gainPct
                )
            }.collectLatest { state ->
                _uiState.value = ScreenState.Success(state)
            }
        }
    }

    fun buyAsset(investmentId: Long, amount: Double, walletId: Long) {
        viewModelScope.launch {
            val investment = investmentRepository.getInvestmentById(investmentId) ?: return@launch
            
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = walletId,
                investmentId = investmentId,
                type = TransactionType.Expense,
                amount = amount,
                note = "Bought more ${investment.name} (${investment.symbol})",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "INVESTMENT_BUY"
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun sellAsset(investmentId: Long, amount: Double, walletId: Long) {
        viewModelScope.launch {
            val investment = investmentRepository.getInvestmentById(investmentId) ?: return@launch
            
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = walletId, // Acts as target wallet
                investmentId = investmentId,
                type = TransactionType.Income,
                amount = amount,
                note = "Sold ${investment.name} (${investment.symbol})",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "INVESTMENT_SELL"
            )
            transactionRepository.insertTransaction(transaction)
        }
    }
}
