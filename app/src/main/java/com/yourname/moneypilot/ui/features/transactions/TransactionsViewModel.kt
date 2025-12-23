package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class GroupedTransactions(
    val date: LocalDate,
    val transactions: List<TransactionEntity>,
    val dailyTotal: Double
)

data class TransactionsState(
    val groupedTransactions: List<GroupedTransactions> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : BaseViewModel<TransactionsState>() {

    private val _searchQuery = MutableStateFlow("")

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            combine(
                transactionRepository.getAllTransactions(),
                _searchQuery
            ) { transactions, query ->
                val filtered = if (query.isBlank()) {
                    transactions
                } else {
                    transactions.filter { 
                        it.description.contains(query, ignoreCase = true)
                    }
                }

                val grouped = filtered.groupBy { it.date.toLocalDate() }
                    .map { (date, items) ->
                        val total = items.sumOf { 
                            if (it.type == "EXPENSE") -it.amount else it.amount 
                        }
                        GroupedTransactions(date, items, total)
                    }
                    .sortedByDescending { it.date }

                TransactionsState(grouped, query)
            }.collect { state ->
                if (state.groupedTransactions.isEmpty() && state.searchQuery.isBlank()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(state)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun duplicateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val duplicated = transaction.copy(
                id = 0,
                date = LocalDateTime.now(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            transactionRepository.insertTransaction(duplicated)
        }
    }
}
