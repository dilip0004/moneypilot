package com.yourname.moneypilot.ui.features.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.repository.BackupRepository
import com.yourname.moneypilot.domain.usecase.ledger.VerifyLedgerIntegrityUseCase
import com.yourname.moneypilot.domain.usecase.transaction.ExportToExcelUseCase
import com.yourname.moneypilot.domain.usecase.transaction.ExportTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    private val exportToExcelUseCase: ExportToExcelUseCase,
    private val backupRepository: BackupRepository,
    private val verifyLedgerIntegrityUseCase: VerifyLedgerIntegrityUseCase,
    @ApplicationContext private val context: Context,
    private val database: MoneyPilotDatabase
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class FileReady(val file: File, val mimeType: String) : UiEvent()
        data class SaveJson(val jsonContent: String, val fileName: String) : UiEvent()
        data class ShowIntegrityWarning(val mismatchCount: Int, val jsonContent: String, val fileName: String) : UiEvent()
    }

    fun exportToCSV() {
        viewModelScope.launch {
            try {
                val file = exportTransactionsUseCase()
                if (file != null) {
                    _eventFlow.emit(UiEvent.FileReady(file, "text/csv"))
                } else {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No transactions to export."))
                }
            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                _eventFlow.emit(UiEvent.ShowSnackbar("Export failed: ${e.message}"))
            }
        }
    }

    fun exportToExcel() {
        viewModelScope.launch {
            try {
                val file = exportToExcelUseCase()
                if (file != null) {
                    // Using application/vnd.ms-excel for XML-based SpreadsheetML compatibility
                    _eventFlow.emit(UiEvent.FileReady(file, "application/vnd.ms-excel"))
                } else {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No transactions to export."))
                }
            } catch (e: Exception) {
                Timber.e(e, "Excel export failed")
                _eventFlow.emit(UiEvent.ShowSnackbar("Excel export failed: ${e.message}"))
            }
        }
    }

    fun exportToJson() {
        viewModelScope.launch {
            try {
                val mismatches = verifyLedgerIntegrityUseCase()
                val json = backupRepository.createJsonBackup()
                val date = LocalDate.now().toString()
                val fileName = "MoneyPilot_Backup_$date.json"

                if (mismatches.isNotEmpty()) {
                    Timber.w("Backup initiated with ${mismatches.size} integrity mismatches.")
                    _eventFlow.emit(UiEvent.ShowIntegrityWarning(mismatches.size, json, fileName))
                } else {
                    _eventFlow.emit(UiEvent.SaveJson(json, fileName))
                }
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Backup failed: ${e.message}"))
            }
        }
    }

    fun confirmBackupWithWarnings(json: String, fileName: String) {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.SaveJson(json, fileName))
        }
    }

    fun restoreFromJson(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.restoreFromJson(uri)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Database restored successfully!"))
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar("Restore failed: ${result.exceptionOrNull()?.message}"))
            }
        }
    }
}
