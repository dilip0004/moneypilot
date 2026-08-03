package com.yourname.moneypilot.ui.features.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.BankImportRepository
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.domain.model.CsvColumnMapping
import com.yourname.moneypilot.domain.model.ImportedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

data class BankImportState(
    val isLoading: Boolean = false,
    val isFileSelected: Boolean = false,
    val importedTransactions: List<ImportedTransaction> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedWalletId: Long? = null,
    val mapping: CsvColumnMapping = CsvColumnMapping(),
    val error: String? = null,
    val importSuccessCount: Int? = null
)

sealed class BankImportEvent {
    data class FileSelected(val uri: Uri) : BankImportEvent()
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

    private var parsedLines: List<List<String>> = emptyList()
    private var parseJob: Job? = null

    init {
        // Safe initialization
        viewModelScope.launch {
            try {
                loadInitialData()
            } catch (e: Throwable) {
                Timber.e(e, "BankImportViewModel: Init failed critical")
            }
        }
    }

    private suspend fun loadInitialData() {
        // Fetch wallets
        walletRepository.getAllWallets()
            .catch { e -> Timber.e(e, "Wallets flow error") }
            .collectLatest { wallets ->
                _state.update { currentState ->
                    currentState.copy(
                        wallets = wallets,
                        selectedWalletId = currentState.selectedWalletId ?: wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id
                    )
                }
            }
    }

    // Separate launch for categories to avoid blocking
    fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesByType("EXPENSE")
                .catch { e -> Timber.e(e, "Categories flow error") }
                .collectLatest { categories ->
                    _state.update { it.copy(categories = categories) }
                }
        }
    }

    fun onEvent(event: BankImportEvent, context: Context? = null) {
        when (event) {
            is BankImportEvent.FileSelected -> {
                context?.let { loadFile(event.uri, it) }
            }
            is BankImportEvent.ColumnMappingChanged -> {
                _state.update { it.copy(mapping = event.mapping) }
                updatePreview()
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

    private fun loadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _state.update { it.copy(isLoading = false, error = "Could not open file") }
                    return@launch
                }

                inputStream.use { stream ->
                    parsedLines = com.yourname.moneypilot.util.CsvParser.parse(stream)
                    if (parsedLines.isEmpty()) {
                        _state.update { it.copy(isLoading = false, error = "File is empty or invalid") }
                    } else {
                        _state.update { it.copy(isFileSelected = true, isLoading = false) }
                        updatePreview()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "BankImportViewModel: File load failed")
                _state.update { it.copy(isLoading = false, error = "Failed to load file: ${e.message}") }
            }
        }
    }

    private fun updatePreview() {
        if (parsedLines.isEmpty()) return
        val mapping = _state.value.mapping
        
        if (mapping.dateIndex == -1 || mapping.amountIndex == -1 || mapping.descriptionIndex == -1) {
            _state.update { it.copy(importedTransactions = emptyList()) }
            return
        }

        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // We use the already parsed lines to avoid re-opening the stream and permission issues
                val transactions = importRepository.convertToTransactions(parsedLines, mapping)
                val deduped = importRepository.detectDuplicates(transactions)
                
                _state.update { it.copy(
                    importedTransactions = deduped.map { it.copy(walletId = _state.value.selectedWalletId) },
                    isLoading = false
                ) }
            } catch (e: Exception) {
                Timber.e(e, "BankImportViewModel: Preview update failed")
                _state.update { it.copy(isLoading = false, error = "Preview failed: ${e.message}") }
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
