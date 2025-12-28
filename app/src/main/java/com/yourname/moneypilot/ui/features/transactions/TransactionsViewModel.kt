package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class GroupedTransactions(
    val date: LocalDate,
    val transactions: List<TransactionWithCategory>,
    val dailyTotal: Double
)

data class TransactionsState(
    val groupedTransactions: List<GroupedTransactions> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : BaseViewModel<TransactionsState>() {

    private val _searchQuery = MutableStateFlow("")

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            combine(
                transactionRepository.getAllTransactionsWithCategory(),
                _searchQuery
            ) { transactions, query ->
                val filtered = if (query.isBlank()) {
                    transactions
                } else {
                    transactions.filter { 
                        it.transaction.description.contains(query, ignoreCase = true) ||
                        it.category?.name?.contains(query, ignoreCase = true) == true
                    }
                }

                val grouped = filtered.groupBy { it.transaction.date.toLocalDate() }
                    .map { (date, items) ->
                        val total = items.sumOf { 
                            if (it.transaction.type == "EXPENSE") -it.transaction.amount else it.transaction.amount 
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
            try {
                // Reverse Sync: Update balance
                val balanceChange = when (transaction.type) {
                    "INCOME" -> -transaction.amount // Remove income
                    "EXPENSE", "GOAL_CONTRIBUTION" -> transaction.amount // Add back expense
                    else -> 0.0
                }
                accountRepository.updateBalance(transaction.accountId, balanceChange)

                // Reverse Sync: Update budget if needed
                if (transaction.type == "EXPENSE" && transaction.categoryId != null) {
                    recalculateBudget(transaction.categoryId!!, transaction.date)
                }

                transactionRepository.deleteTransaction(transaction)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun recalculateBudget(categoryId: Long, date: LocalDateTime) {
        val startOfMonth = date.withDayOfMonth(1).with(LocalTime.MIN)
        val endOfMonth = date.withDayOfMonth(date.toLocalDate().lengthOfMonth()).with(LocalTime.MAX)
        
        val totalSpent = transactionRepository.getCategoryExpenseSum(categoryId, startOfMonth, endOfMonth)
        
        budgetRepository.getActiveBudgets(date.toLocalDate()).first().find { it.categoryId == categoryId }?.let { budget ->
            budgetRepository.updateSpentAmount(budget.id, totalSpent)
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
            
            val balanceChange = when (duplicated.type) {
                "INCOME" -> duplicated.amount
                "EXPENSE", "GOAL_CONTRIBUTION" -> -duplicated.amount
                else -> 0.0
            }
            accountRepository.updateBalance(duplicated.accountId, balanceChange)
            
            if (duplicated.type == "EXPENSE" && duplicated.categoryId != null) {
                recalculateBudget(duplicated.categoryId!!, duplicated.date)
            }
        }
    }
}
