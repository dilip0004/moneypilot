package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val state: StateFlow<LoanDetailsState> = _state

    fun load(loanId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val loan = loanRepository.getLoanById(loanId)
            if (loan == null) {
                _state.update { it.copy(loading = false, loan = null) }
                return@launch
            }
            loanRepository.getLoanEvents(loanId).collect { events ->
                _state.update { it.copy(loading = false, loan = loan, events = events) }
            }
        }
    }

    fun addRateChange(loanId: Long, date: LocalDate, newRate: Double, note: String? = null) {
        viewModelScope.launch {
            loanRepository.addLoanEvent(
                LoanEventEntity(
                    loanId = loanId,
                    eventType = "RATE_CHANGE",
                    eventDate = date,
                    newInterestRate = newRate,
                    note = note
                )
            )
        }
    }

    fun addPrepayment(loanId: Long, date: LocalDate, amount: Double, note: String? = null) {
        viewModelScope.launch {
            loanRepository.addLoanEvent(
                LoanEventEntity(
                    loanId = loanId,
                    eventType = "PREPAYMENT",
                    eventDate = date,
                    amount = amount,
                    note = note
                )
            )
        }
    }

    fun deleteEvent(event: LoanEventEntity) {
        viewModelScope.launch {
            loanRepository.deleteLoanEvent(event)
        }
    }

    fun updateEvent(event: LoanEventEntity) {
        viewModelScope.launch {
            loanRepository.updateLoanEvent(event)
        }
    }

    fun addBackDeleted(event: LoanEventEntity) {
        viewModelScope.launch {
            // reinsert with same id (REPLACE behavior)
            loanRepository.addLoanEvent(event)
        }
    }
}
