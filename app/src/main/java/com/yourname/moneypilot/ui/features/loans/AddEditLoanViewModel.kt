package com.yourname.moneypilot.ui.features.loans

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import timber.log.Timber

data class AddEditLoanState(
    val name: String = "",
    val lender: String = "",
    val amount: String = "",
    val interestRate: String = "0",
    val monthlyPayment: String = "",
    val durationMonths: String = "12",
    val type: String = "BORROWED",
    val startDate: LocalDate = LocalDate.now(),
    val repaymentDayOfMonth: Int = 1,
    val linkedWalletId: Long? = null,
    val wallets: List<WalletEntity> = emptyList()
)

sealed class AddEditLoanEvent {
    data class EnteredName(val value: String) : AddEditLoanEvent()
    data class EnteredLender(val value: String) : AddEditLoanEvent()
    data class EnteredAmount(val value: String) : AddEditLoanEvent()
    data class EnteredInterest(val value: String) : AddEditLoanEvent()
    data class EnteredMonthlyPayment(val value: String) : AddEditLoanEvent()
    data class EnteredDuration(val value: String) : AddEditLoanEvent()
    data class TypeChanged(val value: String) : AddEditLoanEvent()
    data class RepaymentDayChanged(val value: Int) : AddEditLoanEvent()
    data class WalletLinked(val value: Long?) : AddEditLoanEvent()
    object SaveLoan : AddEditLoanEvent()
}

@HiltViewModel
class AddEditLoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    private val walletRepository: WalletRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditLoanState())
    val state: StateFlow<AddEditLoanState> = _state.asStateFlow()

    private var currentLoanId: Long? = null
    private var originalLoan: LoanEntity? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveLoan : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadData()
    }

    private fun loadData() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.update { it.copy(wallets = wallets) }
        }.launchIn(viewModelScope)

        val loanId = savedStateHandle.get<Long>("loanId")
        if (loanId != null && loanId != -1L) {
            viewModelScope.launch {
                loanRepository.getLoanById(loanId)?.also { loan ->
                    currentLoanId = loan.id
                    originalLoan = loan
                    _state.update { it.copy(
                        name = loan.name,
                        lender = loan.lender,
                        amount = loan.totalAmount.toString(),
                        interestRate = loan.interestRate.toString(),
                        monthlyPayment = loan.monthlyPayment.toString(),
                        durationMonths = loan.durationMonths.toString(),
                        type = loan.type,
                        startDate = loan.startDate,
                        repaymentDayOfMonth = loan.repaymentDayOfMonth,
                        linkedWalletId = loan.linkedWalletId
                    ) }
                }
            }
        }
    }

    fun onEvent(event: AddEditLoanEvent) {
        when (event) {
            is AddEditLoanEvent.EnteredName -> _state.update { it.copy(name = event.value) }
            is AddEditLoanEvent.EnteredLender -> _state.update { it.copy(lender = event.value) }
            is AddEditLoanEvent.EnteredAmount -> _state.update { it.copy(amount = event.value) }
            is AddEditLoanEvent.EnteredInterest -> _state.update { it.copy(interestRate = event.value) }
            is AddEditLoanEvent.EnteredMonthlyPayment -> _state.update { it.copy(monthlyPayment = event.value) }
            is AddEditLoanEvent.EnteredDuration -> _state.update { it.copy(durationMonths = event.value) }
            is AddEditLoanEvent.TypeChanged -> _state.update { it.copy(type = event.value) }
            is AddEditLoanEvent.RepaymentDayChanged -> _state.update { it.copy(repaymentDayOfMonth = event.value) }
            is AddEditLoanEvent.WalletLinked -> _state.update { it.copy(linkedWalletId = event.value) }
            is AddEditLoanEvent.SaveLoan -> saveLoan()
        }
    }

    private fun saveLoan() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                
                // 1. Precise Validation with Trimming
                val name = currentState.name.trim()
                val lender = currentState.lender.trim()
                val amountStr = currentState.amount.replace(",", "").trim()
                val emiStr = currentState.monthlyPayment.replace(",", "").trim()
                
                val amountValue = amountStr.toDoubleOrNull() ?: 0.0
                val emiValue = emiStr.toDoubleOrNull() ?: 0.0
                val rateValue = currentState.interestRate.toDoubleOrNull() ?: 0.0
                val durationValue = currentState.durationMonths.toIntOrNull() ?: 0

                if (name.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Validation Error: Please enter a name for the loan."))
                    return@launch
                }
                if (amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Validation Error: Principal amount must be greater than zero."))
                    return@launch
                }
                if (emiValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Validation Error: Monthly EMI must be greater than zero."))
                    return@launch
                }
                if (durationValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Validation Error: Loan tenure must be at least 1 month."))
                    return@launch
                }

                val loan = LoanEntity(
                    id = currentLoanId ?: 0L,
                    name = name,
                    lender = lender,
                    totalAmount = amountValue,
                    interestRate = rateValue,
                    startDate = currentState.startDate,
                    durationMonths = durationValue,
                    currentBalance = if (currentLoanId == null) amountValue else {
                        loanRepository.getLoanById(currentLoanId!!)?.currentBalance ?: amountValue
                    },
                    monthlyPayment = emiValue,
                    type = currentState.type,
                    linkedWalletId = currentState.linkedWalletId,
                    repaymentDayOfMonth = currentState.repaymentDayOfMonth
                )

                if (currentLoanId == null) {
                    val id = loanRepository.insertLoan(loan)
                    loanRepository.insertLoanEvent(LoanEventEntity(
                        loanId = id,
                        eventType = "INITIAL_LOAN",
                        eventDate = currentState.startDate,
                        amount = amountValue,
                        note = "Loan initiated"
                    ))
                } else {
                    loanRepository.updateLoan(loan)
                    originalLoan?.let { original ->
                        if (original.interestRate != rateValue) {
                            loanRepository.insertLoanEvent(LoanEventEntity(
                                loanId = original.id,
                                eventType = "RATE_CHANGE",
                                eventDate = LocalDate.now(),
                                newInterestRate = rateValue,
                                note = "ROI updated to $rateValue%"
                            ))
                        }
                        if (original.monthlyPayment != emiValue) {
                            loanRepository.insertLoanEvent(LoanEventEntity(
                                loanId = original.id,
                                eventType = "EMI_CHANGE",
                                eventDate = LocalDate.now(),
                                newMonthlyPayment = emiValue,
                                note = "EMI updated to $emiValue"
                            ))
                        }
                    }
                }
                
                _eventFlow.emit(UiEvent.SaveLoan)
            } catch (e: Exception) {
                Timber.e(e, "Save Loan Failed")
                _eventFlow.emit(UiEvent.ShowSnackbar("System Error: ${e.localizedMessage}"))
            }
        }
    }
}
