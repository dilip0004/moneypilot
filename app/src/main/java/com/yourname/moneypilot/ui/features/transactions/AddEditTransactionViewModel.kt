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
    val goalId: Long? = null,
    val investmentId: Long? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val isEditing: Boolean = false
)

sealed class AddEditTransactionEvent {
    data class EnteredDescription(val value: String) : AddEditTransactionEvent()
    data class EnteredAmount(val value: String) : AddEditTransactionEvent()
    data class TypeChanged(val value: TransactionType) : AddEditTransactionEvent()
    data class WalletChanged(val value: Long) : AddEditTransactionEvent()
    data class CategoryChanged(val value: Long) : AddEditTransactionEvent()
    data class SubcategoryChanged(val value: Long?) : AddEditTransactionEvent()
    data class LoanChanged(val value: Long?) : AddEditTransactionEvent()
    data class GoalChanged(val value: Long?) : AddEditTransactionEvent()
    data class InvestmentChanged(val value: Long?) : AddEditTransactionEvent()
    data class DateChanged(val value: LocalDateTime) : AddEditTransactionEvent()
    object SaveTransaction : AddEditTransactionEvent()
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val loanRepository: LoanRepository,
    private val goalRepository: GoalRepository,
    private val investmentRepository: InvestmentRepository,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state: StateFlow<AddEditTransactionState> = _state.asStateFlow()

    private val _typeFlow = MutableStateFlow<TransactionType?>(null)
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
            val transactionId = savedStateHandle.get<String>("transactionId")
            
            // 1. Load basic dependencies
            loadStaticData()

            // 2. Load existing transaction if editing
            if (transactionId != null) {
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
                        date = transaction.dateTime,
                        isEditing = true
                    ) }
                    _typeFlow.value = transaction.type
                    transaction.categoryId?.let { loadSubcategories(it) }
                }
            } else {
                _typeFlow.value = TransactionType.Expense
            }

            // 3. Start reactive category flow AFTER initial load
            observeCategories()
        }
    }

    private fun loadStaticData() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.update { it.copy(
                wallets = wallets,
                walletFromId = if (it.isEditing) it.walletFromId else (wallets.find { w -> w.isPrimary }?.id ?: wallets.firstOrNull()?.id)
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
    }

    private fun observeCategories() {
        viewModelScope.launch {
            _typeFlow.filterNotNull().flatMapLatest { type ->
                categoryRepository.getCategoriesByType(if (type == TransactionType.Transfer) "EXPENSE" else type.name.uppercase())
            }.collect { categories ->
                _state.update { s ->
                    // If we just changed type manually, the current categoryId might not belong to this new type
                    val categoryStillValid = categories.any { it.id == s.categoryId }
                    val newId = if (categoryStillValid) s.categoryId else categories.firstOrNull()?.id
                    
                    s.copy(
                        categories = categories,
                        categoryId = newId
                    ).also { 
                        if (newId != null && newId != s.categoryId) loadSubcategories(newId)
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.EnteredDescription -> _state.update { it.copy(description = event.value) }
            is AddEditTransactionEvent.EnteredAmount -> _state.update { it.copy(amount = event.value) }
            is AddEditTransactionEvent.TypeChanged -> {
                _typeFlow.value = event.value
                _state.update { it.copy(type = event.value, subcategoryId = null, goalId = null, investmentId = null) }
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
                    if (currentState.categoryId == null && currentState.goalId == null) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Please select a category or link a goal."))
                        return@launch
                    }
                }

                val finalType = if (currentState.goalId != null) TransactionType.Expense else currentState.type

                val transaction = TransactionEntity(
                    id = currentTransactionId ?: UUID.randomUUID().toString(),
                    walletFromId = walletId,
                    categoryId = currentState.categoryId,
                    subcategoryId = currentState.subcategoryId,
                    loanId = currentState.loanId,
                    goalId = currentState.goalId,
                    investmentId = currentState.investmentId,
                    type = finalType,
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
