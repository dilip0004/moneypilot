package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.*
import com.yourname.moneypilot.util.SmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class AddEditTransactionState(
    val description: String = "",
    val amount: String = "",
    val type: String = "EXPENSE",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val goalId: Long? = null,
    val loanId: Long? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val isTruthReviewed: Boolean = true // Flag for SMS truth review
)

sealed class AddEditTransactionEvent {
    data class EnteredDescription(val value: String) : AddEditTransactionEvent()
    data class EnteredAmount(val value: String) : AddEditTransactionEvent()
    data class TypeChanged(val value: String) : AddEditTransactionEvent()
    data class AccountChanged(val value: Long) : AddEditTransactionEvent()
    data class CategoryChanged(val value: Long) : AddEditTransactionEvent()
    data class SubcategoryChanged(val value: Long?) : AddEditTransactionEvent()
    data class GoalChanged(val value: Long?) : AddEditTransactionEvent()
    data class LoanChanged(val value: Long?) : AddEditTransactionEvent()
    data class DateChanged(val value: LocalDateTime) : AddEditTransactionEvent()
    data class PasteSms(val text: String) : AddEditTransactionEvent()
    object AcceptTruth : AddEditTransactionEvent()
    object SaveTransaction : AddEditTransactionEvent()
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state: StateFlow<AddEditTransactionState> = _state.asStateFlow()

    private val _typeFlow = MutableStateFlow("EXPENSE")
    private var currentTransactionId: Long? = null
    private var originalTransaction: TransactionEntity? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveTransaction : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        viewModelScope.launch {
            // Sequential initialization to ensure categories exist before loading
            categoryRepository.seedDefaults()
            loadData()
            checkExistingTransaction()
        }
    }

    private fun checkExistingTransaction() {
        val transactionId = savedStateHandle.get<Long>("transactionId")
        if (transactionId != null && transactionId != -1L) {
            viewModelScope.launch {
                transactionRepository.getTransactionById(transactionId)?.let { transaction ->
                    currentTransactionId = transaction.id
                    originalTransaction = transaction
                    _state.update { it.copy(
                        description = transaction.description,
                        amount = transaction.amount.toString(),
                        type = transaction.type,
                        accountId = transaction.accountId,
                        categoryId = transaction.categoryId,
                        subcategoryId = transaction.subcategoryId,
                        goalId = transaction.goalId,
                        loanId = transaction.loanId,
                        date = transaction.date
                    ) }
                    _typeFlow.value = transaction.type
                }
            }
        }
    }

    private fun loadData() {
        accountRepository.getAllAccounts().onEach { accounts ->
            _state.update { it.copy(
                accounts = accounts,
                accountId = it.accountId ?: accounts.find { acc -> acc.isPrimary }?.id ?: accounts.firstOrNull()?.id
            ) }
        }.launchIn(viewModelScope)

        _typeFlow.flatMapLatest { type ->
            categoryRepository.getCategoriesByType(if (type == "LOAN_REPAYMENT") "EXPENSE" else type)
        }.onEach { categories ->
            val firstId = categories.firstOrNull()?.id
            _state.update { state ->
                val currentId = state.categoryId
                val newId = if (categories.any { it.id == currentId }) currentId else firstId
                state.copy(
                    categories = categories,
                    categoryId = newId
                )
            }
            // Use the determined ID to load subcategories
            val idToLoad = if (categories.any { it.id == _state.value.categoryId }) _state.value.categoryId else firstId
            idToLoad?.let { loadSubcategories(it) }
        }.launchIn(viewModelScope)

        goalRepository.getAllGoals().onEach { goals ->
            _state.update { it.copy(
                goals = goals.filter { goal -> goal.status == "ACTIVE" }
            ) }
        }.launchIn(viewModelScope)

        loanRepository.getAllLoans().onEach { loans ->
            _state.update { it.copy(
                loans = loans.filter { loan -> loan.status == "ACTIVE" }
            ) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.EnteredDescription -> _state.update { it.copy(description = event.value) }
            is AddEditTransactionEvent.EnteredAmount -> _state.update { it.copy(amount = event.value) }
            is AddEditTransactionEvent.TypeChanged -> {
                _state.update { it.copy(type = event.value, categoryId = null, subcategoryId = null, goalId = null, loanId = null) }
                _typeFlow.value = event.value
            }
            is AddEditTransactionEvent.AccountChanged -> _state.update { it.copy(accountId = event.value) }
            is AddEditTransactionEvent.CategoryChanged -> {
                _state.update { it.copy(categoryId = event.value, subcategoryId = null) }
                loadSubcategories(event.value)
            }
            is AddEditTransactionEvent.SubcategoryChanged -> _state.update { it.copy(subcategoryId = event.value) }
            is AddEditTransactionEvent.GoalChanged -> _state.update { it.copy(goalId = event.value) }
            is AddEditTransactionEvent.LoanChanged -> _state.update { it.copy(loanId = event.value) }
            is AddEditTransactionEvent.DateChanged -> _state.update { it.copy(date = event.value) }
            is AddEditTransactionEvent.PasteSms -> processSms(event.text)
            is AddEditTransactionEvent.AcceptTruth -> _state.update { it.copy(isTruthReviewed = true) }
            is AddEditTransactionEvent.SaveTransaction -> saveTransaction()
        }
    }

    private fun loadSubcategories(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.getSubcategories(categoryId).collect { subList ->
                _state.update { it.copy(subcategories = subList) }
            }
        }
    }

    private fun processSms(text: String) {
        val parsed = SmsParser.parse(text)
        _state.update { it.copy(
            amount = parsed.amount?.toString() ?: it.amount,
            type = parsed.type,
            description = parsed.merchant ?: it.description,
            date = parsed.date,
            isTruthReviewed = false
        ) }
        _typeFlow.value = parsed.type
    }

    private fun saveTransaction() {
        viewModelScope.launch {
            try {
                val currentState = _state.value
                if (!currentState.isTruthReviewed) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please review and accept the parsed data."))
                    return@launch
                }

                val amountValue = currentState.amount.toDoubleOrNull() ?: 0.0
                if (amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount"))
                    return@launch
                }
                if (currentState.accountId == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select an account"))
                    return@launch
                }

                if (currentState.type in listOf("EXPENSE", "INCOME")) {
                    if (currentState.categoryId == null) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Please select a category."))
                        return@launch
                    }
                    if (currentState.subcategories.isNotEmpty() && currentState.subcategoryId == null) {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Please select a subcategory."))
                        return@launch
                    }
                }

                // IRREVERSIBILITY LOGIC: Reverse original impact if editing
                originalTransaction?.let { old ->
                    reverseImpact(old)
                }

                val transaction = TransactionEntity(
                    id = currentTransactionId ?: 0L,
                    accountId = currentState.accountId!!,
                    categoryId = if (currentState.type == "GOAL_CONTRIBUTION" || currentState.type == "LOAN_REPAYMENT") null else currentState.categoryId,
                    subcategoryId = if (currentState.type == "GOAL_CONTRIBUTION" || currentState.type == "LOAN_REPAYMENT") null else currentState.subcategoryId,
                    goalId = currentState.goalId,
                    loanId = currentState.loanId,
                    type = currentState.type,
                    amount = amountValue,
                    description = currentState.description,
                    date = currentState.date
                )

                transactionRepository.insertTransaction(transaction)
                applyImpact(transaction)

                _eventFlow.emit(UiEvent.SaveTransaction)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Save failed: ${e.message}"))
            }
        }
    }

    private suspend fun reverseImpact(transaction: TransactionEntity) {
        val reverseBalanceChange = when (transaction.type) {
            "INCOME" -> -transaction.amount
            "EXPENSE", "GOAL_CONTRIBUTION", "LOAN_REPAYMENT" -> transaction.amount
            else -> 0.0
        }
        accountRepository.updateBalance(transaction.accountId, reverseBalanceChange)

        if (transaction.type == "GOAL_CONTRIBUTION" && transaction.goalId != null) {
            goalRepository.incrementCurrentAmount(transaction.goalId!!, -transaction.amount)
        }

        if (transaction.type == "LOAN_REPAYMENT" && transaction.loanId != null) {
            loanRepository.getLoanById(transaction.loanId!!)?.let {
                loanRepository.updateLoan(it.copy(currentBalance = it.currentBalance + transaction.amount))
            }
        }
    }

    private suspend fun applyImpact(transaction: TransactionEntity) {
        val balanceChange = when (transaction.type) {
            "INCOME" -> transaction.amount
            "EXPENSE", "GOAL_CONTRIBUTION", "LOAN_REPAYMENT" -> -transaction.amount
            else -> 0.0
        }
        accountRepository.updateBalance(transaction.accountId, balanceChange)

        if (transaction.type == "GOAL_CONTRIBUTION" && transaction.goalId != null) {
            goalRepository.incrementCurrentAmount(transaction.goalId!!, transaction.amount)
        }

        if (transaction.type == "LOAN_REPAYMENT" && transaction.loanId != null) {
            loanRepository.getLoanById(transaction.loanId!!)?.let {
                val newBalance = (it.currentBalance - transaction.amount).coerceAtLeast(0.0)
                loanRepository.updateLoan(it.copy(currentBalance = newBalance))
            }
        }

        if (transaction.type == "EXPENSE" && transaction.categoryId != null) {
            updateBudgetSpent(transaction.categoryId!!, transaction.date)
        }
    }

    private suspend fun updateBudgetSpent(categoryId: Long, date: LocalDateTime) {
        val startOfMonth = date.withDayOfMonth(1).with(LocalTime.MIN)
        val endOfMonth = date.withDayOfMonth(date.toLocalDate().lengthOfMonth()).with(LocalTime.MAX)
        
        val totalSpent = transactionRepository.getCategoryExpenseSum(categoryId, startOfMonth, endOfMonth)
        
        budgetRepository.getActiveBudgets(date.toLocalDate()).first().find { b -> b.categoryId == categoryId }?.let { budget ->
            budgetRepository.updateSpentAmount(budget.id, totalSpent)
        }
    }
}