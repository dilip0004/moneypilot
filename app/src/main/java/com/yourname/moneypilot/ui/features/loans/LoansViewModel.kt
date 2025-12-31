package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoansState(
    val loans: List<LoanEntity> = emptyList(),
    val totalBorrowed: Double = 0.0,
    val totalLent: Double = 0.0
)

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : BaseViewModel<LoansState>() {

    init {
        loadLoans()
    }

    private fun loadLoans() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            combine(
                loanRepository.getAllLoans(),
                loanRepository.getTotalBorrowedAmount(),
                loanRepository.getTotalLentAmount()
            ) { loans, borrowed, lent ->
                LoansState(
                    loans = loans,
                    totalBorrowed = borrowed,
                    totalLent = lent
                )
            }.collectLatest { state ->
                if (state.loans.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(state)
                }
            }
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            loanRepository.deleteLoan(loan)
        }
    }
}
