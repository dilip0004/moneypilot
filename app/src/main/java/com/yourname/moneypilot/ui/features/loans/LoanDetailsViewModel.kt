package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.domain.loan.LoanCalculator
import com.yourname.moneypilot.domain.loan.LoanSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LoanDetailsState(
    val loan: LoanEntity? = null,
    val events: List<LoanEventEntity> = emptyList(),
    val snapshot: LoanSnapshot? = null,
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _loanId = MutableStateFlow<Long?>(null)

    val state: StateFlow<LoanDetailsState> = _loanId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                loanRepository.getLoanByIdFlow(id),
                loanRepository.getEventsForLoan(id)
            ) { loan: LoanEntity?, events: List<LoanEventEntity> ->
                if (loan != null) {
                    val (snapshot, _) = LoanCalculator.computeSnapshot(loan, events)
                    LoanDetailsState(loan, events, snapshot, false)
                } else {
                    LoanDetailsState(loading = false)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoanDetailsState()
        )

    init {
        savedStateHandle.get<Long>("loanId")?.let { id ->
            if (id != -1L) {
                _loanId.value = id
            }
        }
    }

    fun load(id: Long) {
        _loanId.value = id
    }

    fun addPrepayment(amount: Double, date: LocalDate, note: String) {
        viewModelScope.launch {
            _loanId.value?.let { id ->
                val event = LoanEventEntity(
                    loanId = id,
                    eventType = "PREPAYMENT",
                    eventDate = date,
                    amount = amount,
                    note = note
                )
                loanRepository.insertLoanEvent(event)
                
                // Update loan balance
                state.value.loan?.let { loan ->
                    val newBalance = (loan.currentBalance - amount).coerceAtLeast(0.0)
                    loanRepository.updateLoan(loan.copy(currentBalance = newBalance))
                }
            }
        }
    }

    fun addRoiChange(newRate: Double, date: LocalDate, note: String) {
        viewModelScope.launch {
            _loanId.value?.let { id ->
                val event = LoanEventEntity(
                    loanId = id,
                    eventType = "RATE_CHANGE",
                    eventDate = date,
                    newInterestRate = newRate,
                    note = note
                )
                loanRepository.insertLoanEvent(event)
                
                // Update loan ROI
                state.value.loan?.let { loan ->
                    loanRepository.updateLoan(loan.copy(interestRate = newRate))
                }
            }
        }
    }
}
