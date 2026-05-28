package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.domain.loan.LoanCalculator
import com.yourname.moneypilot.domain.loan.LoanSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class LoanDetailsState(
    val loan: LoanEntity? = null,
    val events: List<LoanEventEntity> = emptyList(),
    val snapshot: LoanSnapshot? = null,
    val wallets: List<WalletEntity> = emptyList(),
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LoanDetailsViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _loanId = MutableStateFlow<Long?>(null)

    val state: StateFlow<LoanDetailsState> = _loanId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                loanRepository.getLoanByIdFlow(id),
                loanRepository.getEventsForLoan(id),
                walletRepository.getAllWallets()
            ) { loan: LoanEntity?, events: List<LoanEventEntity>, wallets: List<WalletEntity> ->
                if (loan != null) {
                    val (snapshot, _) = LoanCalculator.computeSnapshot(loan, events)
                    LoanDetailsState(
                        loan = loan,
                        events = events,
                        snapshot = snapshot,
                        wallets = wallets.filter { !it.isArchived },
                        loading = false
                    )
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

    /**
     * Adheres to Section 3.0 (No silent mutations).
     * Prepayments are now ledger-backed transactions.
     */
    fun addPrepayment(amount: Double, walletId: Long, date: LocalDate, note: String) {
        viewModelScope.launch {
            val currentLoan = state.value.loan ?: return@launch
            
            // 1. Create Ledger Transaction
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                dateTime = LocalDateTime.now(), // Store actual time of action
                amount = amount,
                type = TransactionType.Expense,
                walletFromId = walletId,
                loanId = currentLoan.id,
                transactionSourceType = "LOAN_PREPAYMENT",
                note = "Prepayment: $note".trim()
            )
            
            // This call is atomic (withTransaction) and handles balance updates
            transactionRepository.insertTransaction(transaction)

            // 2. Log audit event for timeline calculation
            val event = LoanEventEntity(
                loanId = currentLoan.id,
                eventType = "PREPAYMENT",
                eventDate = date,
                amount = amount,
                note = note
            )
            loanRepository.insertLoanEvent(event)
            
            // Balance update is now handled automatically by TransactionRepositoryImpl 
            // via applyFinancialImpact for the LOAN_PREPAYMENT source type.
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
                
                // ROI changes don't involve cash flow, so we update the entity directly
                state.value.loan?.let { loan ->
                    loanRepository.updateLoan(loan.copy(interestRate = newRate))
                }
            }
        }
    }
}
