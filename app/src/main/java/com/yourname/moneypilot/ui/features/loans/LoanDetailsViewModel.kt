package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanDetailsState(
    val loading: Boolean = true,
    val loan: LoanEntity? = null
)

@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanDetailsState())
    val state: StateFlow<LoanDetailsState> = _state

    fun load(loanId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val loan = loanRepository.getLoanById(loanId)
            _state.update { it.copy(loading = false, loan = loan) }
        }
    }
}
