package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
import com.yourname.moneypilot.util.SmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _state = mutableStateOf(AddEditTransactionState())
    val state: State<AddEditTransactionState> = _state

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
            _state.value = _state.value.copy(
                accounts = accounts,
                accountId = _state.value.accountId ?: accounts.find { it.isPrimary }?.id ?: accounts.firstOrNull()?.id
            )
        }.launchIn(viewModelScope)

        // Reactive category loading based on Transaction Type (Expense/Income)
        _typeFlow.flatMapLatest { type ->
            categoryRepository.getCategoriesByType(type)
        }.onEach { categories ->
            _state.value = _state.value.copy(
                categories = categories,
                categoryId = if (categories.any { it.id == _state.value.categoryId }) _state.value.categoryId else categories.firstOrNull()?.id
            )
            _state.value.categoryId?.let { loadSubcategories(it) }
        }.launchIn(viewModelScope)

        goalRepository.getAllGoals().onEach { goals ->
            _state.value = _state.value.copy(
                goals = goals.filter { it.status == "ACTIVE" }
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: AddEditTransactionEvent) {
        when (event) {
            is AddEditTransactionEvent.EnteredDescription -> {
                _state.value = _state.value.copy(description = event.value)
            }
            is AddEditTransactionEvent.EnteredAmount -> {
                _state.value = _state.value.copy(amount = event.value)
            }
            is AddEditTransactionEvent.TypeChanged -> {
                _state.value = _state.value.copy(type = event.value)
                _typeFlow.value = event.value
            }
            is AddEditTransactionEvent.AccountChanged -> {
                _state.value = _state.value.copy(accountId = event.value)
            }
            is AddEditTransactionEvent.CategoryChanged -> {
                _state.value = _state.value.copy(
                    categoryId = event.value,
                    subcategoryId = null
                )
                loadSubcategories(event.value)
            }
            is AddEditTransactionEvent.SubcategoryChanged -> {
                _state.value = _state.value.copy(subcategoryId = event.value)
            }
            is AddEditTransactionEvent.GoalChanged -> {
                _state.value = _state.value.copy(goalId = event.value)
            }
            is AddEditTransactionEvent.DateChanged -> {
                _state.value = _state.value.copy(date = event.value)
            }
            is AddEditTransactionEvent.PasteSms -> {
                processSms(event.text)
            }
            is AddEditTransactionEvent.SaveTransaction -> {
                saveTransaction()
            }
        }
    }

    private fun processSms(text: String) {
        val parsed = SmsParser.parse(text)
        _state.value = _state.value.copy(
            amount = parsed.amount?.toString() ?: _state.value.amount,
            type = parsed.type,
            description = parsed.merchant ?: _state.value.description,
            date = parsed.date
        )
        _typeFlow.value = parsed.type
        
        parsed.accountSuffix?.let { suffix ->
            val matchedAccount = _state.value.accounts.find { it.name.contains(suffix) }
            if (matchedAccount != null) {
                _state.value = _state.value.copy(accountId = matchedAccount.id)
            }
        }
    }

    private fun loadSubcategories(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.getSubcategories(categoryId).collect { subcategories ->
                _state.value = _state.value.copy(subcategories = subcategories)
            }
        }
    }

    private fun saveTransaction() {
        viewModelScope.launch {
            try {
                val amount = _state.value.amount.toDoubleOrNull() ?: 0.0
                if (_state.value.accountId == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select an account"))
                    return@launch
                }

                if (_state.value.type == "GOAL_CONTRIBUTION" && _state.value.goalId == null) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please select a target goal"))
                    return@launch
                }
                
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = _state.value.accountId!!,
                        categoryId = if (_state.value.type == "GOAL_CONTRIBUTION") null else _state.value.categoryId,
                        goalId = _state.value.goalId,
                        type = _state.value.type,
                        amount = amount,
                        description = _state.value.description,
                        date = _state.value.date
                    )
                )

                if (_state.value.type == "GOAL_CONTRIBUTION" && _state.value.goalId != null) {
                    goalRepository.incrementCurrentAmount(_state.value.goalId!!, amount)
                }

                _eventFlow.emit(UiEvent.SaveTransaction)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save transaction"))
            }
        }
    }
}

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
