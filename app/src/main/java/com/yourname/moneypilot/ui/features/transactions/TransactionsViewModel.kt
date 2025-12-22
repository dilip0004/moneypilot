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
import javax.inject.Inject

data class TransactionsState(
    val transactions: List<TransactionEntity> = emptyList(),
    val searchQuery: String = "",
    val isCalendarView: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : BaseViewModel<TransactionsState>() {

    private val _searchQuery = MutableStateFlow("")
    private val _isCalendarView = MutableStateFlow(false)

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            combine(
                transactionRepository.getAllTransactions(),
                _searchQuery,
                _isCalendarView
            ) { transactions, query, isCalendar ->
                val filtered = if (query.isBlank()) {
                    transactions
                } else {
                    transactions.filter { 
                        it.description.contains(query, ignoreCase = true) || 
                        it.type.contains(query, ignoreCase = true)
                    }
                }
                TransactionsState(filtered, query, isCalendar)
            }.collect { state ->
                if (state.transactions.isEmpty() && state.searchQuery.isBlank()) {
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

    fun toggleViewMode() {
        _isCalendarView.value = !_isCalendarView.value
    }
}
