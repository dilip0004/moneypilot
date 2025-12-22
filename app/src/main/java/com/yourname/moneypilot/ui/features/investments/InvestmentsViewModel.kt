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

data class InvestmentsState(
    val investments: List<InvestmentEntity> = emptyList(),
    val totalValue: Double = 0.0
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository
) : BaseViewModel<InvestmentsState>() {

    init {
        loadInvestments()
    }

    private fun loadInvestments() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            investmentRepository.getAllInvestments().collectLatest { list ->
                val total = list.sumOf { it.quantity * it.currentPrice }
                _uiState.value = ScreenState.Success(InvestmentsState(list, total))
            }
        }
    }

    fun addInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            investmentRepository.insertInvestment(investment)
        }
    }
}
