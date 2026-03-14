package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class WalletStatementState(
    val wallet: WalletEntity? = null,
    val transactions: List<TransactionWithDetails> = emptyList(),
    val openingBalance: Double = 0.0,
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WalletStatementViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val walletId: Long = checkNotNull(savedStateHandle["walletId"])
    
    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _endDate = MutableStateFlow(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))

    private val _state = MutableStateFlow(WalletStatementState())
    val state: StateFlow<WalletStatementState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val wallet = walletRepository.getWalletById(walletId)
            _state.update { it.copy(wallet = wallet) }
            
            combine(_startDate, _endDate) { start, end ->
                Pair(start, end)
            }.flatMapLatest { (start, end) ->
                _state.update { it.copy(isLoading = true, startDate = start, endDate = end) }
                
                // Opening Balance = Initial Balance + Sum of all transactions before start date
                val sumBefore = transactionRepository.getSumBeforeDate(walletId, start.atStartOfDay())
                val opening = (wallet?.initialBalance ?: 0.0) + sumBefore
                _state.update { it.copy(openingBalance = opening) }

                transactionRepository.getTransactionsWithDetailsForWallet(
                    walletId, 
                    start.atStartOfDay(), 
                    end.atTime(LocalTime.MAX)
                )
            }.collect { txs ->
                _state.update { it.copy(transactions = txs, isLoading = false) }
            }
        }
    }

    fun onDateRangeChanged(start: LocalDate, end: LocalDate) {
        _startDate.value = start
        _endDate.value = end
    }
}
