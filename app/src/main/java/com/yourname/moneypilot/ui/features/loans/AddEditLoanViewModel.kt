package com.yourname.moneypilot.ui.features.loans

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddEditLoanState(
    val name: String = "",
    val lender: String = "",
    val amount: String = "",
    val interestRate: String = "0",
    val monthlyPayment: String = "",
    val durationMonths: String = "12",
    val type: String = "BORROWED",
    val startDate: LocalDate = LocalDate.now()
)

@HiltViewModel
class AddEditLoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(AddEditLoanState())
    val state: State<AddEditLoanState> = _state

    private var currentLoanId: Long? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveLoan : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        savedStateHandle.get<Long>("loanId")?.let { loanId ->
            if (loanId != -1L) {
                viewModelScope.launch {
                    loanRepository.getLoanById(loanId)?.also { loan ->
                        currentLoanId = loan.id
                        _state.value = _state.value.copy(
                            name = loan.name,
                            lender = loan.lender,
                            amount = loan.totalAmount.toString(),
                            interestRate = loan.interestRate.toString(),
                            monthlyPayment = loan.monthlyPayment.toString(),
                            durationMonths = loan.durationMonths.toString(),
                            type = loan.type,
                            startDate = loan.startDate
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditLoanEvent) {
        when (event) {
            is AddEditLoanEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditLoanEvent.EnteredLender -> _state.value = _state.value.copy(lender = event.value)
            is AddEditLoanEvent.EnteredAmount -> _state.value = _state.value.copy(amount = event.value)
            is AddEditLoanEvent.EnteredInterest -> _state.value = _state.value.copy(interestRate = event.value)
            is AddEditLoanEvent.EnteredMonthlyPayment -> _state.value = _state.value.copy(monthlyPayment = event.value)
            is AddEditLoanEvent.TypeChanged -> _state.value = _state.value.copy(type = event.value)
            is AddEditLoanEvent.SaveLoan -> saveLoan()
        }
    }

    private fun saveLoan() {
        viewModelScope.launch {
            try {
                val amount = _state.value.amount.toDoubleOrNull() ?: 0.0
                if (_state.value.name.isBlank() || amount <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please fill in all required fields."))
                    return@launch
                }

                loanRepository.insertLoan(
                    LoanEntity(
                        id = currentLoanId ?: 0L,
                        name = _state.value.name,
                        lender = _state.value.lender,
                        totalAmount = amount,
                        interestRate = _state.value.interestRate.toDoubleOrNull() ?: 0.0,
                        startDate = _state.value.startDate,
                        durationMonths = _state.value.durationMonths.toIntOrNull() ?: 12,
                        currentBalance = amount, // Initial balance is the total amount
                        monthlyPayment = _state.value.monthlyPayment.toDoubleOrNull() ?: 0.0,
                        type = _state.value.type,
                        accountId = null // Can be linked later
                    )
                )
                _eventFlow.emit(UiEvent.SaveLoan)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save loan"))
            }
        }
    }
}

sealed class AddEditLoanEvent {
    data class EnteredName(val value: String) : AddEditLoanEvent()
    data class EnteredLender(val value: String) : AddEditLoanEvent()
    data class EnteredAmount(val value: String) : AddEditLoanEvent()
    data class EnteredInterest(val value: String) : AddEditLoanEvent()
    data class EnteredMonthlyPayment(val value: String) : AddEditLoanEvent()
    data class TypeChanged(val value: String) : AddEditLoanEvent()
    object SaveLoan : AddEditLoanEvent()
}
