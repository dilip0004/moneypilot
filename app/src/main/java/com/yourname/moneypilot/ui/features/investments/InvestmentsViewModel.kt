package com.yourname.moneypilot.ui.features.investments

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import com.yourname.moneypilot.data.repository.InvestmentRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvestmentState(
    val investments: List<InvestmentEntity> = emptyList(),
    val totalValue: Double = 0.0,
    val totalGain: Double = 0.0,
    val gainPercentage: Double = 0.0
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository
) : BaseViewModel<InvestmentState>() {

    init {
        loadInvestments()
    }

    private fun loadInvestments() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            investmentRepository.getAllInvestments().collectLatest { list ->
                val totalValue = list.sumOf { it.quantity * it.currentPrice }
                val totalCost = list.sumOf { it.quantity * it.averagePrice }
                val totalGain = totalValue - totalCost
                val gainPct = if (totalCost > 0) (totalGain / totalCost) * 100 else 0.0
                
                _uiState.value = ScreenState.Success(
                    InvestmentState(
                        investments = list,
                        totalValue = totalValue,
                        totalGain = totalGain,
                        gainPercentage = gainPct
                    )
                )
            }
        }
    }

    fun addInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            investmentRepository.insertInvestment(investment)
        }
    }
}
