package com.yourname.moneypilot.ui.features.transactions

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class AddEditTransactionState(
    val description: String = "",
    val amount: String = "",
    val type: String = "EXPENSE",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = mutableStateOf(AddEditTransactionState())
    val state: State<AddEditTransactionState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveTransaction : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        accountRepository.getAllAccounts().onEach { accounts ->
            _state.value = _state.value.copy(
                accounts = accounts,
                accountId = _state.value.accountId ?: accounts.firstOrNull()?.id
            )
        }.launchIn(viewModelScope)

        categoryRepository.getAllCategories().onEach { categories ->
            _state.value = _state.value.copy(
                categories = categories,
                categoryId = _state.value.categoryId ?: categories.firstOrNull()?.id
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
            }
            is AddEditTransactionEvent.AccountChanged -> {
                _state.value = _state.value.copy(accountId = event.value)
            }
            is AddEditTransactionEvent.CategoryChanged -> {
                _state.value = _state.value.copy(categoryId = event.value)
            }
            is AddEditTransactionEvent.SaveTransaction -> {
                saveTransaction()
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
                
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = _state.value.accountId!!,
                        categoryId = _state.value.categoryId,
                        type = _state.value.type,
                        amount = amount,
                        description = _state.value.description,
                        date = _state.value.date
                    )
                )
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
    object SaveTransaction : AddEditTransactionEvent()
}
