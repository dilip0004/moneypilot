package com.yourname.moneypilot.ui.features.transactions

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GroupedTransactions(
    val dateLabel: String,
    val date: LocalDate,
    val transactions: List<TransactionWithDetails>,
    val dailyTotal: Double
)

data class TransactionsState(
    val groupedTransactions: List<GroupedTransactions> = emptyList(),
    val searchQuery: String = "",
    val selectedWalletId: Long? = null,
    val wallets: List<com.yourname.moneypilot.data.local.database.entities.WalletEntity> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel<TransactionsState>() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedWalletId = MutableStateFlow<Long?>(null)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private var lastDeletedTransaction: TransactionEntity? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowUndoSnackbar(val message: String) : UiEvent()
    }

    init {
        loadWalletsAndTransactions()
    }

    private fun loadWalletsAndTransactions() {
        viewModelScope.launch {
            // Load wallets
            walletRepository.getAllWallets().collect { wallets ->
                _uiState.updateSuccess { it.copy(wallets = wallets) }
            }
        }

        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactionsWithDetails(),
                _searchQuery,
                _selectedWalletId,
                _currentMonth
            ) { transactions, query, walletId, month ->
                
                // 1. Filter by Month/Year
                val monthFiltered = transactions.filter {
                    val txDate = it.transaction.dateTime.toLocalDate()
                    txDate.year == month.year && txDate.month == month.month
                }

                // 2. Filter by wallet if selected
                val walletFiltered = if (walletId != null && walletId != -1L) {
                    monthFiltered.filter {
                        it.transaction.walletFromId == walletId ||
                                it.transaction.walletToId == walletId
                    }
                } else {
                    monthFiltered
                }

                // 3. Filter by Search Query
                val filtered = if (query.isBlank()) walletFiltered
                else walletFiltered.filter {
                    it.transaction.note?.contains(query, ignoreCase = true) == true ||
                            it.category?.name?.contains(query, ignoreCase = true) == true
                }

                val grouped = filtered.groupBy { it.transaction.dateTime.toLocalDate() }
                    .map { (date, items) ->
                        val total = items.sumOf {
                            when (it.transaction.type) {
                                TransactionType.Expense -> -it.transaction.amount
                                TransactionType.Income -> it.transaction.amount
                                TransactionType.Transfer -> 0.0 // Transfers don't change net total usually, or handle as needed
                            }
                        }
                        GroupedTransactions(
                            dateLabel = getRelativeDateLabel(date),
                            date = date,
                            transactions = items,
                            dailyTotal = total
                        )
                    }
                    .sortedByDescending { it.date }

                TransactionsState(
                    groupedTransactions = grouped,
                    searchQuery = query,
                    selectedWalletId = walletId,
                    wallets = (_uiState.value as? ScreenState.Success)?.data?.wallets ?: emptyList(),
                    currentMonth = month
                )
            }.collect { state ->
                _uiState.value = if (state.groupedTransactions.isEmpty() && state.searchQuery.isBlank()) {
                    ScreenState.Empty
                } else {
                    ScreenState.Success(state)
                }
            }
        }
    }

    private fun getRelativeDateLabel(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
        }
    }

    fun updateMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onWalletSelected(walletId: Long) {
        _selectedWalletId.value = walletId
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            lastDeletedTransaction = transaction
            transactionRepository.deleteTransaction(transaction)
            _eventFlow.emit(UiEvent.ShowUndoSnackbar("Transaction deleted"))
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            lastDeletedTransaction?.let {
                transactionRepository.insertTransaction(it)
                lastDeletedTransaction = null
            }
        }
    }
    
    // Helper to update Success state safely
    private fun MutableStateFlow<ScreenState<TransactionsState>>.updateSuccess(
        transform: (TransactionsState) -> TransactionsState
    ) {
        val current = value
        if (current is ScreenState.Success) {
            value = ScreenState.Success(transform(current.data))
        }
    }
}