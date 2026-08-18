package com.yourname.moneypilot.ui.features.investments

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

@Serializable
data class InvestmentExtraData(
    val interestRate: Double? = null,
    val maturityDate: String? = null,
    val tenureMonths: Int? = null,
    val monthlyInstallment: Double? = null,
    val annualContribution: Double? = null,
    val sipAmount: Double? = null,
    val frequency: String? = null, // MONTHLY, QUARTERLY, YEARLY
    val purchaseValue: Double? = null,
    val currentValuation: Double? = null,
    val currentBalance: Double? = null
)

data class AddEditInvestmentState(
    val name: String = "",
    val type: String = "STOCKS",
    val symbol: String = "",
    val quantity: String = "",
    val averagePrice: String = "",
    val currentPrice: String = "",
    val currency: String = "INR",
    val startDate: LocalDate = LocalDate.now(),
    val linkedWalletId: Long? = null,
    val wallets: List<WalletEntity> = emptyList(),
    val isEditMode: Boolean = false,
    // Type-specific fields
    val interestRate: String = "",
    val tenureMonths: String = "",
    val monthlyInstallment: String = "",
    val annualContribution: String = "",
    val sipAmount: String = "",
    val frequency: String = "MONTHLY",
    val purchaseValue: String = "",
    val currentValuation: String = "",
    val currentBalance: String = "",
    val principal: String = ""
)

@HiltViewModel
class AddEditInvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(AddEditInvestmentState())
    val state: State<AddEditInvestmentState> = _state

    private var currentInvestmentId: Long? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveInvestment : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadWallets()
        checkEditMode()
    }

    private fun loadWallets() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.value = _state.value.copy(
                wallets = wallets,
                linkedWalletId = _state.value.linkedWalletId ?: wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id
            )
        }.launchIn(viewModelScope)
    }

    private fun checkEditMode() {
        val id = savedStateHandle.get<Long>("investmentId")
        if (id != null && id != -1L) {
            viewModelScope.launch {
                investmentRepository.getInvestmentById(id)?.let { investment ->
                    currentInvestmentId = investment.id
                    _state.value = _state.value.copy(
                        name = investment.name,
                        type = investment.type,
                        symbol = investment.symbol,
                        quantity = investment.quantity.toString(),
                        averagePrice = investment.averagePrice.toString(),
                        currentPrice = investment.currentPrice.toString(),
                        currency = investment.currency,
                        startDate = investment.startDate ?: LocalDate.now(),
                        linkedWalletId = investment.linkedWalletId,
                        isEditMode = true
                    )
                    investment.extraData?.let {
                        try {
                            val extra = Json.decodeFromString<InvestmentExtraData>(it)
                            _state.value = _state.value.copy(
                                interestRate = extra.interestRate?.toString() ?: "",
                                tenureMonths = extra.tenureMonths?.toString() ?: "",
                                monthlyInstallment = extra.monthlyInstallment?.toString() ?: "",
                                annualContribution = extra.annualContribution?.toString() ?: "",
                                sipAmount = extra.sipAmount?.toString() ?: "",
                                frequency = extra.frequency ?: "MONTHLY",
                                purchaseValue = extra.purchaseValue?.toString() ?: "",
                                currentValuation = extra.currentValuation?.toString() ?: "",
                                currentBalance = extra.currentBalance?.toString() ?: "",
                                principal = extra.currentBalance?.toString() ?: ""
                            )
                        } catch (e: Exception) { /* Ignore parsing errors */ }
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditInvestmentEvent) {
        when (event) {
            is AddEditInvestmentEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditInvestmentEvent.EnteredSymbol -> _state.value = _state.value.copy(symbol = event.value)
            is AddEditInvestmentEvent.EnteredQuantity -> _state.value = _state.value.copy(quantity = event.value)
            is AddEditInvestmentEvent.EnteredAvgPrice -> _state.value = _state.value.copy(averagePrice = event.value)
            is AddEditInvestmentEvent.EnteredCurrentPrice -> _state.value = _state.value.copy(currentPrice = event.value)
            is AddEditInvestmentEvent.TypeChanged -> _state.value = _state.value.copy(type = event.value)
            is AddEditInvestmentEvent.WalletLinked -> _state.value = _state.value.copy(linkedWalletId = event.value)
            is AddEditInvestmentEvent.StartDateChanged -> _state.value = _state.value.copy(startDate = event.value)
            is AddEditInvestmentEvent.EnteredInterestRate -> _state.value = _state.value.copy(interestRate = event.value)
            is AddEditInvestmentEvent.EnteredTenureMonths -> _state.value = _state.value.copy(tenureMonths = event.value)
            is AddEditInvestmentEvent.EnteredMonthlyInstallment -> _state.value = _state.value.copy(monthlyInstallment = event.value)
            is AddEditInvestmentEvent.EnteredAnnualContribution -> _state.value = _state.value.copy(annualContribution = event.value)
            is AddEditInvestmentEvent.EnteredSipAmount -> _state.value = _state.value.copy(sipAmount = event.value)
            is AddEditInvestmentEvent.FrequencyChanged -> _state.value = _state.value.copy(frequency = event.value)
            is AddEditInvestmentEvent.EnteredPurchaseValue -> _state.value = _state.value.copy(purchaseValue = event.value)
            is AddEditInvestmentEvent.EnteredCurrentValuation -> _state.value = _state.value.copy(currentValuation = event.value)
            is AddEditInvestmentEvent.EnteredCurrentBalance -> _state.value = _state.value.copy(currentBalance = event.value)
            is AddEditInvestmentEvent.EnteredPrincipal -> _state.value = _state.value.copy(principal = event.value)
            is AddEditInvestmentEvent.SaveInvestment -> saveInvestment()
            else -> {}
        }
    }

    private fun saveInvestment() {
        viewModelScope.launch {
            try {
                val s = _state.value
                val qty = s.quantity.toDoubleOrNull() ?: 0.0
                val avgPrice = s.averagePrice.toDoubleOrNull() ?: 0.0
                val currentPrice = s.currentPrice.toDoubleOrNull() ?: avgPrice
                
                val investedAmount = when (s.type) {
                    "STOCKS", "CRYPTO", "GOLD" -> qty * avgPrice
                    "FD" -> s.principal.toDoubleOrNull() ?: 0.0
                    "RD" -> s.monthlyInstallment.toDoubleOrNull() ?: 0.0
                    "PPF" -> 0.0 // PPF often starts with 0 or a first contribution
                    "SIP" -> s.sipAmount.toDoubleOrNull() ?: 0.0
                    "REAL_ESTATE" -> s.purchaseValue.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }

                if (s.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter an asset name."))
                    return@launch
                }

                // Validation logic (Spec 9)
                try {
                    when (s.type) {
                        "STOCKS", "CRYPTO", "GOLD" -> {
                            if (qty <= 0) throw Exception("Quantity must be positive.")
                            if (avgPrice <= 0) throw Exception("Average price must be positive.")
                        }
                        "FD" -> {
                            if ((s.principal.toDoubleOrNull() ?: 0.0) <= 0) throw Exception("Principal must be positive.")
                            if ((s.interestRate.toDoubleOrNull() ?: 0.0) < 0) throw Exception("Invalid interest rate.")
                            if ((s.tenureMonths.toIntOrNull() ?: 0) <= 0) throw Exception("Tenure must be positive.")
                        }
                        "RD" -> {
                            if ((s.monthlyInstallment.toDoubleOrNull() ?: 0.0) <= 0) throw Exception("Installment must be positive.")
                            if ((s.interestRate.toDoubleOrNull() ?: 0.0) < 0) throw Exception("Invalid interest rate.")
                            if ((s.tenureMonths.toIntOrNull() ?: 0) <= 0) throw Exception("Tenure must be positive.")
                        }
                        "PPF" -> {
                            if ((s.currentBalance.toDoubleOrNull() ?: -1.0) < 0) throw Exception("Invalid current balance.")
                            if ((s.interestRate.toDoubleOrNull() ?: 0.0) < 0) throw Exception("Invalid interest rate.")
                        }
                        "SIP" -> {
                            if ((s.sipAmount.toDoubleOrNull() ?: 0.0) <= 0) throw Exception("SIP amount must be positive.")
                        }
                        "REAL_ESTATE" -> {
                            if ((s.purchaseValue.toDoubleOrNull() ?: 0.0) <= 0) throw Exception("Purchase price must be positive.")
                        }
                    }
                } catch (e: Exception) {
                    _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "Validation failed"))
                    return@launch
                }

                val maturityDate = when(s.type) {
                    "FD", "RD" -> {
                        val tenure = s.tenureMonths.toIntOrNull() ?: 0
                        if (tenure > 0) s.startDate.plusMonths(tenure.toLong()).toString() else null
                    }
                    else -> null
                }

                val extra = InvestmentExtraData(
                    interestRate = s.interestRate.toDoubleOrNull(),
                    maturityDate = maturityDate,
                    tenureMonths = s.tenureMonths.toIntOrNull(),
                    monthlyInstallment = s.monthlyInstallment.toDoubleOrNull(),
                    annualContribution = s.annualContribution.toDoubleOrNull(),
                    sipAmount = s.sipAmount.toDoubleOrNull(),
                    frequency = s.frequency,
                    purchaseValue = s.purchaseValue.toDoubleOrNull(),
                    currentValuation = s.currentValuation.toDoubleOrNull(),
                    currentBalance = s.currentBalance.toDoubleOrNull()
                )
                val extraJson = Json.encodeToString(extra)

                val investment = InvestmentEntity(
                    id = currentInvestmentId ?: 0L,
                    name = s.name,
                    type = s.type,
                    symbol = s.symbol,
                    quantity = if (s.isEditMode) investmentRepository.getInvestmentById(currentInvestmentId!!)?.quantity ?: 0.0 else qty,
                    averagePrice = if (s.isEditMode) investmentRepository.getInvestmentById(currentInvestmentId!!)?.averagePrice ?: 0.0 else avgPrice,
                    currentPrice = currentPrice,
                    currency = s.currency,
                    startDate = s.startDate,
                    extraData = extraJson,
                    lastUpdated = LocalDateTime.now()
                )

                val investmentId = investmentRepository.insertInvestment(investment)

                // 2. Ledger Integration (Only for new buys)
                if (!s.isEditMode && s.linkedWalletId != null && investedAmount > 0) {
                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            walletFromId = s.linkedWalletId,
                            investmentId = investmentId,
                            type = TransactionType.Expense,
                            amount = investedAmount,
                            note = "Investment: ${s.name} (${s.type})",
                            dateTime = LocalDateTime.now(),
                            transactionSourceType = "INVESTMENT_BUY"
                        )
                    )
                }

                _eventFlow.emit(UiEvent.SaveInvestment)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Save failed: ${e.message}"))
            }
        }
    }
}

sealed class AddEditInvestmentEvent {
    data class EnteredName(val value: String) : AddEditInvestmentEvent()
    data class EnteredSymbol(val value: String) : AddEditInvestmentEvent()
    data class EnteredQuantity(val value: String) : AddEditInvestmentEvent()
    data class EnteredAvgPrice(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentPrice(val value: String) : AddEditInvestmentEvent()
    data class TypeChanged(val value: String) : AddEditInvestmentEvent()
    data class WalletLinked(val value: Long?) : AddEditInvestmentEvent()
    data class StartDateChanged(val value: LocalDate) : AddEditInvestmentEvent()
    data class EnteredInterestRate(val value: String) : AddEditInvestmentEvent()
    data class EnteredTenureMonths(val value: String) : AddEditInvestmentEvent()
    data class EnteredMonthlyInstallment(val value: String) : AddEditInvestmentEvent()
    data class EnteredAnnualContribution(val value: String) : AddEditInvestmentEvent()
    data class EnteredSipAmount(val value: String) : AddEditInvestmentEvent()
    data class FrequencyChanged(val value: String) : AddEditInvestmentEvent()
    data class EnteredPurchaseValue(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentValuation(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentBalance(val value: String) : AddEditInvestmentEvent()
    data class EnteredPrincipal(val value: String) : AddEditInvestmentEvent()
    object SaveInvestment : AddEditInvestmentEvent()
}
