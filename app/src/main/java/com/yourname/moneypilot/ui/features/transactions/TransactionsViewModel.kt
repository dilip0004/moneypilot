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
    val wallets: List<com.yourname.moneypilot.data.local.database.entities.WalletEntity> = emptyList()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel<TransactionsState>() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedWalletId = MutableStateFlow<Long?>(null)
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
            // Load wallets first
            walletRepository.getAllWallets().collect { wallets ->
                _uiState.value = (_uiState.value as? ScreenState.Success)?.let {
                    it.copy(data = (it.data as TransactionsState).copy(wallets = wallets))
                } ?: ScreenState.Success(TransactionsState(wallets = wallets))

                // Auto-select primary wallet if none selected
                if (_selectedWalletId.value == null) {
                    val primary = wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id
                    primary?.let { onWalletSelected(it) }
                }
            }
        }

        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactionsWithDetails(),
                _searchQuery,
                _selectedWalletId
            ) { transactions, query, walletId ->
                // Filter by wallet if selected
                val walletFiltered = if (walletId != null) {
                    transactions.filter {
                        it.transaction.walletFromId == walletId ||
                                it.transaction.walletToId == walletId
                    }
                } else {
                    transactions
                }

                val filtered = if (query.isBlank()) walletFiltered
                else walletFiltered.filter {
                    it.transaction.note?.contains(query, ignoreCase = true) == true ||
                            it.category?.name?.contains(query, ignoreCase = true) == true
                }

                val grouped = filtered.groupBy { it.transaction.dateTime.toLocalDate() }
                    .map { (date, items) ->
                        val total = items.sumOf {
                            if (it.transaction.type == TransactionType.Expense) -it.transaction.amount else it.transaction.amount
                        }
                        GroupedTransactions(
                            dateLabel = getRelativeDateLabel(date),
                            date = date,
                            transactions = items,
                            dailyTotal = total
                        )
                    }
                    .sortedByDescending { it.date }

                TransactionsState(grouped, query, walletId, (_uiState.value as? ScreenState.Success)?.data?.wallets ?: emptyList())
            }.collect { state ->
                if (state.groupedTransactions.isEmpty() && state.searchQuery.isBlank()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(state)
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
}