package com.yourname.moneypilot.ui.features.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.repository.BankImportRepository
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.domain.model.CsvColumnMapping
import com.yourname.moneypilot.domain.model.ImportedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject
import timber.log.Timber

data class BankImportState(
    val isLoading: Boolean = false,
    val importedTransactions: List<ImportedTransaction> = emptyList(),
    val wallets: List<com.yourname.moneypilot.data.local.database.entities.WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedWalletId: Long? = null,
    val mapping: CsvColumnMapping = CsvColumnMapping(),
    val error: String? = null,
    val importSuccessCount: Int? = null
)

sealed class BankImportEvent {
    data class FileSelected(val inputStream: InputStream) : BankImportEvent()
    data class ColumnMappingChanged(val mapping: CsvColumnMapping) : BankImportEvent()
    data class WalletSelected(val walletId: Long) : BankImportEvent()
    data class CategorySelected(val index: Int, val categoryId: Long?) : BankImportEvent()
    data class ToggleTransactionSelection(val index: Int) : BankImportEvent()
    object StartImport : BankImportEvent()
}

@HiltViewModel
class BankImportViewModel @Inject constructor(
    private val importRepository: BankImportRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BankImportState())
    val state: StateFlow<BankImportState> = _state.asStateFlow()

    private var currentInputStream: InputStream? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Combine data loading into a single stream to prevent race conditions on emulator init
                combine(
                    walletRepository.getAllWallets(),
                    categoryRepository.getCategoriesByType("EXPENSE")
                ) { wallets, categories ->
                    _state.update { it.copy(
                        wallets = wallets,
                        categories = categories,
                        selectedWalletId = it.selectedWalletId ?: wallets.find { w -> w.isPrimary }?.id ?: wallets.firstOrNull()?.id
                    ) }
                }.collect()
            } catch (e: Exception) {
                Timber.e(e, "BankImportViewModel: Initial data load failed")
            }
        }
    }

    fun onEvent(event: BankImportEvent) {
        when (event) {
            is BankImportEvent.FileSelected -> {
                currentInputStream = event.inputStream
                parsePreview()
            }
            is BankImportEvent.ColumnMappingChanged -> {
                _state.update { it.copy(mapping = event.mapping) }
                parsePreview()
            }
            is BankImportEvent.WalletSelected -> {
                _state.update { it.copy(selectedWalletId = event.walletId) }
            }
            is BankImportEvent.CategorySelected -> {
                _state.update { s ->
                    val newList = s.importedTransactions.toMutableList()
                    if (event.index in newList.indices) {
                        val item = newList[event.index]
                        newList[event.index] = item.copy(categoryId = event.categoryId)
                    }
                    s.copy(importedTransactions = newList)
                }
            }
            is BankImportEvent.ToggleTransactionSelection -> {
                _state.update { s ->
                    val newList = s.importedTransactions.toMutableList()
                    if (event.index in newList.indices) {
                        val item = newList[event.index]
                        newList[event.index] = item.copy(isSelected = !item.isSelected)
                    }
                    s.copy(importedTransactions = newList)
                }
            }
            is BankImportEvent.StartImport -> {
                commitImport()
            }
        }
    }

    private fun parsePreview() {
        val inputStream = currentInputStream ?: return
        val mapping = _state.value.mapping
        
        if (mapping.dateIndex == -1 || mapping.amountIndex == -1 || mapping.descriptionIndex == -1) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val transactions = importRepository.parseCsv(inputStream, mapping)
                val deduped = importRepository.detectDuplicates(transactions)
                
                _state.update { it.copy(
                    importedTransactions = deduped.map { it.copy(walletId = _state.value.selectedWalletId) },
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to parse: ${e.message}") }
            }
        }
    }

    private fun commitImport() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val transactionsToCommit = _state.value.importedTransactions.map { 
                    it.copy(walletId = _state.value.selectedWalletId) 
                }
                val result = importRepository.commitImports(transactionsToCommit)
                result.onSuccess { count ->
                    _state.update { it.copy(isLoading = false, importSuccessCount = count) }
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = "Import failed: ${e.message}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Critical system failure during commit") }
            }
        }
    }
}
