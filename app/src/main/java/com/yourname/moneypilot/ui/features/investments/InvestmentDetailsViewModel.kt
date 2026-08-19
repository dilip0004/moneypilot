package com.yourname.moneypilot.ui.features.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.data.repository.InvestmentRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

data class InvestmentDetailsState(
    val investment: InvestmentEntity? = null,
    val history: List<TransactionWithDetails> = emptyList(),
    val totalInvested: Double = 0.0,
    val currentValue: Double = 0.0
)

@HiltViewModel
class InvestmentDetailsViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<InvestmentDetailsState>() {

    private val investmentId: Long = savedStateHandle.get<Long>("investmentId") ?: 0L

    init {
        loadDetails()
    }

    private fun loadDetails() {
        if (investmentId == 0L) return
        
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            combine(
                flow { emit(investmentRepository.getInvestmentById(investmentId)) },
                transactionRepository.getTransactionsForInvestment(investmentId)
            ) { investment, history ->
                if (investment != null) {
                    val effectiveQty = if (investment.quantity == 0.0 && investment.type != "SIP") {
                        try {
                            val extra = investment.extraData?.let { Json.decodeFromString<InvestmentExtraData>(it) }
                            extra?.currentBalance ?: extra?.purchaseValue ?: 0.0
                        } catch (e: Exception) { 0.0 }
                    } else investment.quantity

                    val totalInvested = effectiveQty * investment.averagePrice
                    val currentValue = effectiveQty * investment.currentPrice
                    
                    InvestmentDetailsState(
                        investment = investment,
                        history = history,
                        totalInvested = totalInvested,
                        currentValue = currentValue
                    )
                } else {
                    null
                }
            }.collectLatest { state ->
                if (state != null) {
                    _uiState.value = ScreenState.Success(state)
                } else {
                    _uiState.value = ScreenState.Empty
                }
            }
        }
    }
}
