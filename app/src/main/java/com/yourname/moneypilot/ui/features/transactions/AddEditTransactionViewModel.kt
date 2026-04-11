package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.*
import com.yourname.moneypilot.domain.usecase.transaction.SaveTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class AddEditTransactionState(
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.Expense,
    val walletFromId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val loanId: Long? = null,
    val goalId: Long? = null, // Added for Goal Linking
    val investmentId: Long? = null, // Added for Investment Linking
    val date: LocalDateTime = LocalDateTime.now(),
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(), // Added
    val investments: List<InvestmentEntity> = emptyList() // Added
)

sealed class AddEditTransactionEvent {
    data class EnteredDescription(val value: String) : AddEditTransactionEvent()
    data class EnteredAmount(val value: String) : AddEditTransactionEvent()
    data class TypeChanged(val value: TransactionType) : AddEditTransactionEvent()
    data class WalletChanged(val value: Long) : AddEditTransactionEvent()
    data class CategoryChanged(val value: Long) : AddEditTransactionEvent()
    data class SubcategoryChanged(val value: Long?) : AddEditTransactionEvent()
    data class LoanChanged(val value: Long?) : AddEditTransactionEvent()
    data class GoalChanged(val value: Long?) : AddEditTransactionEvent() // Added
    data class InvestmentChanged(val value: Long?) : AddEditTransactionEvent() // Added
    data class DateChanged(val value: LocalDateTime) : AddEditTransactionEvent()
    object SaveTransaction : AddEditTransactionEvent()
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val loanRepository: LoanRepository,
    private val goalRepository: GoalRepository, // Added
    private val investmentRepository: InvestmentRepository, // Added
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state: StateFlow<AddEditTransactionState> = _state.asStateFlow()

    private val _typeFlow = MutableStateFlow(TransactionType.Expense)
    private var currentTransactionId: String? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveTransaction : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaults()
            loadData()
            checkExistingTransaction()
        }
    }

    private fun checkExistingTransaction() {
        val transactionId = savedStateHandle.get<String>("transactionId")
        if (transactionId != null) {
            viewModelScope.launch {
                transactionRepository.getTransactionById(transactionId)?.let { transaction ->
                    currentTransactionId = transaction.id
                    _state.update { it.copy(
                        description = transaction.note ?: "",
                        amount = transaction.amount.toString(),
                        type = transaction.type,
                        walletFromId = transaction.walletFromId,
                        categoryId = transaction.categoryId,
                        subcategoryId = transaction.subcategoryId,
                        loanId = transaction.loanId,
                        goalId = transaction.goalId,
                        investmentId = transaction.investmentId,
                        date = transaction.dateTime
                    ) }
                    _typeFlow.value = transaction.type
                    transaction.categoryId?.let { loadSubcategories(it) }
                }
            }
        }
    }

    private fun loadData() {
        walletRepository.getAllWallets().onEach { wallets ->
            val defaultWalletId = if (currentTransactionId != null) {
                _state.value.walletFromId
            } else {
                wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id
            }
            _state.update { it.copy(
                wallets = wallets,
                walletFromId = defaultWalletId
            ) }
        }.launchIn(viewModelScope)

        loanRepository.getAllLoans().onEach { loans ->
            _state.update { it.copy(loans = loans.filter { l -> l.status == "ACTIVE" }) }
        }.launchIn(viewModelScope)

        goalRepository.getAllGoals().onEach { goals ->
            _state.update { it.copy(goals = goals.filter { g -> g.status == "ACTIVE" }) }
        }.launchIn(viewModelScope)

        investmentRepository.getAllInvestments().onEach { investments ->
            _state.update { it.copy(investments = investments) }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            _typeFlow.flatMapLatest { type ->
                categoryRepository.getCategoriesByType(if (type == TransactionType.Transfer) "EXPENSE" else type.name.uppercase())
            }.collect { categories ->
                val newCategoryId = if (currentTransactionId != null) _state.value.categoryId else categories.firstOrNull()?.id
                _state.update { it.copy(categories = categories, categoryId = newCategoryId) }
                newCategoryId?.let { loadSubcategories(it) }
            }
        }
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.EnteredDescription -> _state.update { it.copy(description = event.value) }
            is AddEditTransactionEvent.EnteredAmount -> _state.update { it.copy(amount = event.value) }
            is AddEditTransactionEvent.TypeChanged -> {
                _state.update { it.copy(type = event.value, categoryId = null, subcategoryId = null) }
                _typeFlow.value = event.value
            }
            is AddEditTransactionEvent.WalletChanged -> _state.update { it.copy(walletFromId = event.value) }
            is AddEditTransactionEvent.CategoryChanged -> {
                _state.update { it.copy(categoryId = event.value, subcategoryId = null) }
                loadSubcategories(event.value)
            }
            is AddEditTransactionEvent.SubcategoryChanged -> _state.update { it.copy(subcategoryId = event.value) }
            is AddEditTransactionEvent.LoanChanged -> _state.update { it.copy(loanId = event.value) }
            is AddEditTransactionEvent.GoalChanged -> _state.update { it.copy(goalId = event.value) }
            is AddEditTransactionEvent.InvestmentChanged -> _state.update { it.copy(investmentId = event.value) }
            is AddEditTransactionEvent.DateChanged -> _state.update { it.copy(date = event.value) }
            is AddEditTransactionEvent.SaveTransaction -> saveTransaction()
        }
    }

    private fun loadSubcategories(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.getSubcategories(categoryId).firstOrNull()?.let { subList ->
                _state.update { it.copy(subcategories = subList) }
            }
        }
    }

    private fun saveTransaction() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                val amountValue = currentState.amount.toDoubleOrNull()

                if (amountValue == null || amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount."))
                    return@launch
                }
                
                val walletId = currentState.walletFromId
                if (walletId == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select a wallet."))
                    return@launch
                }

                if (currentState.type != TransactionType.Transfer) {
                    if (currentState.categoryId == null) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Please select a category."))
                        return@launch
                    }
                }

                val transaction = TransactionEntity(
                    id = currentTransactionId ?: UUID.randomUUID().toString(),
                    walletFromId = walletId,
                    categoryId = currentState.categoryId,
                    subcategoryId = currentState.subcategoryId,
                    loanId = currentState.loanId,
                    goalId = currentState.goalId,
                    investmentId = currentState.investmentId,
                    type = currentState.type,
                    amount = amountValue,
                    note = currentState.description,
                    dateTime = currentState.date,
                    transactionSourceType = when {
                        currentState.loanId != null -> "LOAN_REPAYMENT"
                        currentState.goalId != null -> "GOAL_CONTRIBUTION"
                        currentState.investmentId != null -> "INVESTMENT_BUY"
                        else -> "MANUAL"
                    }
                )
                
                saveTransactionUseCase(transaction, isEdit = currentTransactionId != null)
                _eventFlow.emit(UiEvent.SaveTransaction)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Save failed: ${e.message}"))
            }
        }
    }
}
