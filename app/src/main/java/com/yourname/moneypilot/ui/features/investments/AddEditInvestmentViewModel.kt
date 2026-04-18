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
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@Serializable
data class InvestmentExtraData(
    val symbol: String? = null,
    val buyDate: String? = null,
    val weight: Double? = null,
    val purchaseDate: String? = null,
    val depositAmount: Double? = null,
    val monthlyDeposit: Double? = null,
    val interestRate: Double? = null,
    val startDate: String? = null,
    val maturityDate: String? = null,
    val tenure: Int? = null,
    val monthlyAmount: Double? = null,
    val fundName: String? = null,
    val expectedReturn: Double? = null,
    val propertyName: String? = null,
    val purchasePrice: Double? = null,
    val currentValue: Double? = null,
    val employeeContribution: Double? = null,
    val employerContribution: Double? = null,
    val currentBalance: Double? = null,
    val contribution: Double? = null,
    val pensionEquitySplit: String? = null
)

data class AddEditInvestmentState(
    val name: String = "",
    val type: String = "STOCKS",
    val symbol: String = "",
    val quantity: String = "",
    val averagePrice: String = "",
    val currentPrice: String = "",
    val currency: String = "INR",
    val linkedWalletId: Long? = null,
    val wallets: List<WalletEntity> = emptyList(),
    val buyDate: String = "",
    val weight: String = "",
    val purchaseDate: String = "",
    val depositAmount: String = "",
    val monthlyDeposit: String = "",
    val interestRate: String = "",
    val startDate: String = "",
    val maturityDate: String = "",
    val tenure: String = "",
    val monthlyAmount: String = "",
    val fundName: String = "",
    val expectedReturn: String = "",
    val propertyName: String = "",
    val purchasePrice: String = "",
    val currentValue: String = "",
    val employeeContribution: String = "",
    val employerContribution: String = "",
    val currentBalance: String = "",
    val contribution: String = "",
    val pensionEquitySplit: String = "",
    val principal: String = ""
)

sealed class AddEditInvestmentEvent {
    data class EnteredName(val value: String) : AddEditInvestmentEvent()
    data class EnteredSymbol(val value: String) : AddEditInvestmentEvent()
    data class EnteredQuantity(val value: String) : AddEditInvestmentEvent()
    data class EnteredAvgPrice(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentPrice(val value: String) : AddEditInvestmentEvent()
    data class TypeChanged(val value: String) : AddEditInvestmentEvent()
    data class WalletLinked(val value: Long?) : AddEditInvestmentEvent()
    data class EnteredBuyDate(val value: String) : AddEditInvestmentEvent()
    data class EnteredWeight(val value: String) : AddEditInvestmentEvent()
    data class EnteredPurchaseDate(val value: String) : AddEditInvestmentEvent()
    data class EnteredDepositAmount(val value: String) : AddEditInvestmentEvent()
    data class EnteredMonthlyDeposit(val value: String) : AddEditInvestmentEvent()
    data class EnteredInterestRate(val value: String) : AddEditInvestmentEvent()
    data class EnteredStartDate(val value: String) : AddEditInvestmentEvent()
    data class EnteredMaturityDate(val value: String) : AddEditInvestmentEvent()
    data class EnteredTenure(val value: String) : AddEditInvestmentEvent()
    data class EnteredMonthlyAmount(val value: String) : AddEditInvestmentEvent()
    data class EnteredFundName(val value: String) : AddEditInvestmentEvent()
    data class EnteredExpectedReturn(val value: String) : AddEditInvestmentEvent()
    data class EnteredPropertyName(val value: String) : AddEditInvestmentEvent()
    data class EnteredPurchasePrice(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentValue(val value: String) : AddEditInvestmentEvent()
    data class EnteredEmployeeContribution(val value: String) : AddEditInvestmentEvent()
    data class EnteredEmployerContribution(val value: String) : AddEditInvestmentEvent()
    data class EnteredCurrentBalance(val value: String) : AddEditInvestmentEvent()
    data class EnteredContribution(val value: String) : AddEditInvestmentEvent()
    data class EnteredPensionEquitySplit(val value: String) : AddEditInvestmentEvent()
    data class EnteredPrincipal(val value: String) : AddEditInvestmentEvent()
    object SaveInvestment : AddEditInvestmentEvent()
}

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
                        linkedWalletId = investment.linkedWalletId
                    )
                    investment.extraData?.let {
                        val extra = Json.decodeFromString<InvestmentExtraData>(it)
                        _state.value = _state.value.copy(
                            buyDate = extra.buyDate ?: "",
                            weight = extra.weight?.toString() ?: "",
                            purchaseDate = extra.purchaseDate ?: "",
                            depositAmount = extra.depositAmount?.toString() ?: "",
                            monthlyDeposit = extra.monthlyDeposit?.toString() ?: "",
                            interestRate = extra.interestRate?.toString() ?: "",
                            startDate = extra.startDate ?: "",
                            maturityDate = extra.maturityDate ?: "",
                            tenure = extra.tenure?.toString() ?: "",
                            monthlyAmount = extra.monthlyAmount?.toString() ?: "",
                            fundName = extra.fundName ?: "",
                            expectedReturn = extra.expectedReturn?.toString() ?: "",
                            propertyName = extra.propertyName ?: "",
                            purchasePrice = extra.purchasePrice?.toString() ?: "",
                            currentValue = extra.currentValue?.toString() ?: "",
                            employeeContribution = extra.employeeContribution?.toString() ?: "",
                            employerContribution = extra.employerContribution?.toString() ?: "",
                            currentBalance = extra.currentBalance?.toString() ?: "",
                            contribution = extra.contribution?.toString() ?: "",
                            pensionEquitySplit = extra.pensionEquitySplit ?: "",
                            principal = extra.depositAmount?.toString() ?: ""
                        )
                    }
                }
            }
        }
    }

    private fun loadWallets() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.value = _state.value.copy(wallets = wallets)
        }.launchIn(viewModelScope)
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
            is AddEditInvestmentEvent.EnteredBuyDate -> _state.value = _state.value.copy(buyDate = event.value)
            is AddEditInvestmentEvent.EnteredWeight -> _state.value = _state.value.copy(weight = event.value)
            is AddEditInvestmentEvent.EnteredPurchaseDate -> _state.value = _state.value.copy(purchaseDate = event.value)
            is AddEditInvestmentEvent.EnteredDepositAmount -> _state.value = _state.value.copy(depositAmount = event.value)
            is AddEditInvestmentEvent.EnteredMonthlyDeposit -> _state.value = _state.value.copy(monthlyDeposit = event.value)
            is AddEditInvestmentEvent.EnteredInterestRate -> _state.value = _state.value.copy(interestRate = event.value)
            is AddEditInvestmentEvent.EnteredStartDate -> _state.value = _state.value.copy(startDate = event.value)
            is AddEditInvestmentEvent.EnteredMaturityDate -> _state.value = _state.value.copy(maturityDate = event.value)
            is AddEditInvestmentEvent.EnteredTenure -> _state.value = _state.value.copy(tenure = event.value)
            is AddEditInvestmentEvent.EnteredMonthlyAmount -> _state.value = _state.value.copy(monthlyAmount = event.value)
            is AddEditInvestmentEvent.EnteredFundName -> _state.value = _state.value.copy(fundName = event.value)
            is AddEditInvestmentEvent.EnteredExpectedReturn -> _state.value = _state.value.copy(expectedReturn = event.value)
            is AddEditInvestmentEvent.EnteredPropertyName -> _state.value = _state.value.copy(propertyName = event.value)
            is AddEditInvestmentEvent.EnteredPurchasePrice -> _state.value = _state.value.copy(purchasePrice = event.value)
            is AddEditInvestmentEvent.EnteredCurrentValue -> _state.value = _state.value.copy(currentValue = event.value)
            is AddEditInvestmentEvent.EnteredEmployeeContribution -> _state.value = _state.value.copy(employeeContribution = event.value)
            is AddEditInvestmentEvent.EnteredEmployerContribution -> _state.value = _state.value.copy(employerContribution = event.value)
            is AddEditInvestmentEvent.EnteredCurrentBalance -> _state.value = _state.value.copy(currentBalance = event.value)
            is AddEditInvestmentEvent.EnteredContribution -> _state.value = _state.value.copy(contribution = event.value)
            is AddEditInvestmentEvent.EnteredPensionEquitySplit -> _state.value = _state.value.copy(pensionEquitySplit = event.value)
            is AddEditInvestmentEvent.EnteredPrincipal -> _state.value = _state.value.copy(principal = event.value)
            is AddEditInvestmentEvent.SaveInvestment -> saveInvestment()
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
                    "STOCKS", "MUTUAL_FUNDS", "CRYPTO", "GOLD" -> qty * avgPrice
                    "FD", "PPF" -> s.depositAmount.toDoubleOrNull() ?: 0.0
                    "SIP" -> s.monthlyAmount.toDoubleOrNull() ?: 0.0
                    "RD" -> (s.monthlyDeposit.toDoubleOrNull() ?: 0.0) * (s.tenure.toIntOrNull() ?: 0)
                    "REAL_ESTATE" -> s.purchasePrice.toDoubleOrNull() ?: 0.0
                    "EPF", "NPS" -> s.contribution.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }

                if (s.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter an asset name."))
                    return@launch
                }

                val extra = InvestmentExtraData(
                    symbol = s.symbol.takeIf { it.isNotBlank() },
                    buyDate = s.buyDate.takeIf { it.isNotBlank() },
                    weight = s.weight.toDoubleOrNull(),
                    purchaseDate = s.purchaseDate.takeIf { it.isNotBlank() },
                    depositAmount = s.depositAmount.toDoubleOrNull() ?: s.principal.toDoubleOrNull(),
                    monthlyDeposit = s.monthlyDeposit.toDoubleOrNull(),
                    interestRate = s.interestRate.toDoubleOrNull(),
                    startDate = s.startDate.takeIf { it.isNotBlank() },
                    maturityDate = s.maturityDate.takeIf { it.isNotBlank() },
                    tenure = s.tenure.toIntOrNull(),
                    monthlyAmount = s.monthlyAmount.toDoubleOrNull(),
                    fundName = s.fundName.takeIf { it.isNotBlank() },
                    expectedReturn = s.expectedReturn.toDoubleOrNull(),
                    propertyName = s.propertyName.takeIf { it.isNotBlank() },
                    purchasePrice = s.purchasePrice.toDoubleOrNull(),
                    currentValue = s.currentValue.toDoubleOrNull(),
                    employeeContribution = s.employeeContribution.toDoubleOrNull(),
                    employerContribution = s.employerContribution.toDoubleOrNull(),
                    currentBalance = s.currentBalance.toDoubleOrNull(),
                    contribution = s.contribution.toDoubleOrNull(),
                    pensionEquitySplit = s.pensionEquitySplit.takeIf { it.isNotBlank() }
                )
                val extraJson = Json.encodeToString(extra)

                val investment = InvestmentEntity(
                    id = currentInvestmentId ?: 0L,
                    name = s.name,
                    type = s.type,
                    symbol = s.symbol,
                    quantity = qty,
                    averagePrice = avgPrice,
                    currentPrice = currentPrice,
                    currency = s.currency,
                    linkedWalletId = s.linkedWalletId,
                    extraData = extraJson,
                    lastUpdated = LocalDateTime.now()
                )

                val investmentId = investmentRepository.insertInvestment(investment)

                if (currentInvestmentId == null && s.linkedWalletId != null && investedAmount > 0) {
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