package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanDetailsState(
    val loading: Boolean = true,
    val loan: LoanEntity? = null,
    val events: List<LoanEventEntity> = emptyList()
)

@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoanDetailsState())
    val state: StateFlow<LoanDetailsState> = _state.asStateFlow()

    fun load(loanId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            
            // Combine loan metadata and event history
            val loanFlow = flow { emit(loanRepository.getLoanById(loanId)) }
            val eventsFlow = loanRepository.getEventsForLoan(loanId)

            combine(loanFlow, eventsFlow) { loan, events ->
                LoanDetailsState(
                    loading = false,
                    loan = loan,
                    events = events
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }
}
