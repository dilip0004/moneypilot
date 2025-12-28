package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.BudgetRepository
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
    val date: LocalDateTime = LocalDateTime.now(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val subcategories: List<SubcategoryEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList()
)

sealed class AddEditTransactionEvent {
    data class EnteredDescription(val value: String) : AddEditTransactionEvent()
    data class EnteredAmount(val value: String) : AddEditTransactionEvent()
    data class TypeChanged(val value: String) : AddEditTransactionEvent()
    data class AccountChanged(val value: Long) : AddEditTransactionEvent()
    data class CategoryChanged(val value: Long) : AddEditTransactionEvent()
    data class SubcategoryChanged(val value: Long?) : AddEditTransactionEvent()
    data class GoalChanged(val value: Long?) : AddEditTransactionEvent()
    data class DateChanged(val value: LocalDateTime) : AddEditTransactionEvent()
    data class PasteSms(val text: String) : AddEditTransactionEvent()
    object SaveTransaction : AddEditTransactionEvent()
}

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditTransactionState())
    val state: StateFlow<AddEditTransactionState> = _state.asStateFlow()

    private val _typeFlow = MutableStateFlow("EXPENSE")

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
            categoryRepository.getCategoriesByType(type)
        }.onEach { categories ->
            _state.update { state ->
                val currentId = state.categoryId
                val newId = if (categories.any { it.id == currentId }) currentId else categories.firstOrNull()?.id
                state.copy(
                    categories = categories,
                    categoryId = newId
                )
            }
            _state.value.categoryId?.let { loadSubcategories(it) }
        }.launchIn(viewModelScope)

        goalRepository.getAllGoals().onEach { goals ->
            _state.update { it.copy(
                goals = goals.filter { goal -> goal.status == "ACTIVE" }
            ) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.EnteredDescription -> _state.update { it.copy(description = event.value) }
            is AddEditTransactionEvent.EnteredAmount -> _state.update { it.copy(amount = event.value) }
            is AddEditTransactionEvent.TypeChanged -> {
                _state.update { it.copy(type = event.value) }
                _typeFlow.value = event.value
            }
            is AddEditTransactionEvent.AccountChanged -> _state.update { it.copy(accountId = event.value) }
            is AddEditTransactionEvent.CategoryChanged -> {
                _state.update { it.copy(categoryId = event.value, subcategoryId = null) }
                loadSubcategories(event.value)
            }
            is AddEditTransactionEvent.SubcategoryChanged -> _state.update { it.copy(subcategoryId = event.value) }
            is AddEditTransactionEvent.GoalChanged -> _state.update { it.copy(goalId = event.value) }
            is AddEditTransactionEvent.DateChanged -> _state.update { it.copy(date = event.value) }
            is AddEditTransactionEvent.PasteSms -> processSms(event.text)
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
            date = parsed.date
        ) }
        _typeFlow.value = parsed.type
        parsed.accountSuffix?.let { suffix ->
            _state.value.accounts.find { acc -> acc.name.contains(suffix) }?.let { matched ->
                _state.update { it.copy(accountId = matched.id) }
            }
        }
    }

    private fun saveTransaction() {
        viewModelScope.launch {
            try {
                val amountValue = _state.value.amount.toDoubleOrNull() ?: 0.0
                if (amountValue <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid amount"))
                    return@launch
                }
                if (_state.value.accountId == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select an account"))
                    return@launch
                }

                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = _state.value.accountId!!,
                        categoryId = if (_state.value.type == "GOAL_CONTRIBUTION") null else _state.value.categoryId,
                        subcategoryId = _state.value.subcategoryId,
                        goalId = _state.value.goalId,
                        type = _state.value.type,
                        amount = amountValue,
                        description = _state.value.description,
                        date = _state.value.date
                    )
                )

                val balanceChange = when (_state.value.type) {
                    "INCOME" -> amountValue
                    "EXPENSE", "GOAL_CONTRIBUTION" -> -amountValue
                    else -> 0.0
                }
                accountRepository.updateBalance(_state.value.accountId!!, balanceChange)

                if (_state.value.type == "GOAL_CONTRIBUTION" && _state.value.goalId != null) {
                    goalRepository.incrementCurrentAmount(_state.value.goalId!!, amountValue)
                }

                if (_state.value.type == "EXPENSE" && _state.value.categoryId != null) {
                    updateBudgetSpent(_state.value.categoryId!!)
                }

                _eventFlow.emit(UiEvent.SaveTransaction)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Save failed: ${e.message}"))
            }
        }
    }

    private suspend fun updateBudgetSpent(categoryId: Long) {
        val startOfMonth = _state.value.date.withDayOfMonth(1).with(LocalTime.MIN)
        val endOfMonth = _state.value.date.withDayOfMonth(_state.value.date.toLocalDate().lengthOfMonth()).with(LocalTime.MAX)
        
        val totalSpent = transactionRepository.getCategoryExpenseSum(categoryId, startOfMonth, endOfMonth)
        
        budgetRepository.getActiveBudgets(_state.value.date.toLocalDate()).first().find { b -> b.categoryId == categoryId }?.let { budget ->
            budgetRepository.updateSpentAmount(budget.id, totalSpent)
        }
    }
}
