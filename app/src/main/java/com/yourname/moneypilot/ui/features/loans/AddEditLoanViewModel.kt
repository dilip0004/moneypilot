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

                // Validation
                val name = currentState.name.trim()
                val lender = currentState.lender.trim()
                val amountStr = currentState.amount.replace(",", "").trim()
                val emiStr = currentState.monthlyPayment.replace(",", "").trim()
                val rateStr = currentState.interestRate.replace(",", "").trim()
                val durationStr = currentState.durationMonths.trim()

                if (name.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Loan name is required"))
                    return@launch
                }
                if (lender.isEmpty()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Lender name is required"))
                    return@launch
                }

                val amountValue = amountStr.toDoubleOrNull()
                if (amountValue == null || amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Principal amount must be greater than zero"))
                    return@launch
                }

                val emiValue = emiStr.toDoubleOrNull()
                if (emiValue == null || emiValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Monthly EMI must be greater than zero"))
                    return@launch
                }

                val rateValue = rateStr.toDoubleOrNull()
                if (rateValue == null || rateValue < 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Interest rate cannot be negative"))
                    return@launch
                }

                val durationValue = durationStr.toIntOrNull()
                if (durationValue == null || durationValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Tenure must be at least 1 month"))
                    return@launch
                }

                // FIX #28: When editing principal, adjust currentBalance proportionally
                val currentBalance = if (currentLoanId != null) {
                    val original = originalLoan ?: loanRepository.getLoanById(currentLoanId!!)
                    if (original != null && original.totalAmount != amountValue) {
                        // Calculate new current balance based on ratio
                        val ratio = amountValue / original.totalAmount
                        (original.currentBalance * ratio).coerceAtLeast(0.0)
                    } else {
                        original?.currentBalance ?: amountValue
                    }
                } else {
                    amountValue
                }

                val loan = LoanEntity(
                    id = currentLoanId ?: 0L,
                    name = name,
                    lender = lender,
                    totalAmount = amountValue,
                    interestRate = rateValue,
                    startDate = currentState.startDate,
                    durationMonths = durationValue,
                    currentBalance = currentBalance,
                    monthlyPayment = emiValue,
                    type = currentState.type,
                    linkedWalletId = currentState.linkedWalletId,
                    repaymentDayOfMonth = currentState.repaymentDayOfMonth
                )

                if (currentLoanId == null) {
                    val id = loanRepository.insertLoan(loan)
                    loanRepository.insertLoanEvent(
                        LoanEventEntity(
                            loanId = id,
                            eventType = "INITIAL_LOAN",
                            eventDate = currentState.startDate,
                            amount = amountValue,
                            note = "Loan initiated"
                        )
                    )
                    _eventFlow.emit(UiEvent.ShowSnackbar("Loan created successfully"))
                } else {
                    loanRepository.updateLoan(loan)
                    originalLoan?.let { original ->
                        if (original.interestRate != rateValue) {
                            loanRepository.insertLoanEvent(
                                LoanEventEntity(
                                    loanId = original.id,
                                    eventType = "RATE_CHANGE",
                                    eventDate = LocalDate.now(),
                                    newInterestRate = rateValue,
                                    note = "Interest rate changed from ${original.interestRate}% to $rateValue%"
                                )
                            )
                        }
                        if (original.monthlyPayment != emiValue) {
                            loanRepository.insertLoanEvent(
                                LoanEventEntity(
                                    loanId = original.id,
                                    eventType = "EMI_CHANGE",
                                    eventDate = LocalDate.now(),
                                    newMonthlyPayment = emiValue,
                                    note = "EMI changed from ${original.monthlyPayment} to $emiValue"
                                )
                            )
                        }
                        if (original.totalAmount != amountValue) {
                            loanRepository.insertLoanEvent(
                                LoanEventEntity(
                                    loanId = original.id,
                                    eventType = "AMOUNT_CHANGE",
                                    eventDate = LocalDate.now(),
                                    amount = amountValue,
                                    note = "Principal amount changed from ${original.totalAmount} to $amountValue"
                                )
                            )
                        }
                        if (original.durationMonths != durationValue) {
                            loanRepository.insertLoanEvent(
                                LoanEventEntity(
                                    loanId = original.id,
                                    eventType = "TENURE_CHANGE",
                                    eventDate = LocalDate.now(),
                                    newDurationMonths = durationValue,
                                    note = "Tenure changed from ${original.durationMonths} to $durationValue months"
                                )
                            )
                        }
                    }
                    _eventFlow.emit(UiEvent.ShowSnackbar("Loan updated successfully"))
                }

                _eventFlow.emit(UiEvent.SaveLoan)
            } catch (e: Exception) {
                Timber.e(e, "Save Loan Failed")
                _eventFlow.emit(UiEvent.ShowSnackbar("Error: ${e.localizedMessage}"))
            }
        }
    }
}