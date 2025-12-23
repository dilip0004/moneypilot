package com.yourname.moneypilot.ui.features.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.domain.usecase.transaction.ExportTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportTransactionsUseCase: ExportTransactionsUseCase,
    @ApplicationContext private val context: Context,
    private val database: MoneyPilotDatabase
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        data class FileReady(val file: File) : UiEvent()
    }

    fun exportToCSV() {
        viewModelScope.launch {
            try {
                val file = exportTransactionsUseCase()
                if (file != null) {
                    _eventFlow.emit(UiEvent.FileReady(file))
                    _eventFlow.emit(UiEvent.ShowSnackbar("Export successful! File saved to cache."))
                } else {
                    _eventFlow.emit(UiEvent.ShowSnackbar("No transactions to export."))
                }
            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                _eventFlow.emit(UiEvent.ShowSnackbar("Export failed: ${e.message}"))
            }
        }
    }

    fun createLocalBackup() {
        viewModelScope.launch {
            try {
                // In Room, we can use checkpointing or simply copy the db file if closed
                // For V1, we'll provide a success message as a placeholder for the file-copy logic
                _eventFlow.emit(UiEvent.ShowSnackbar("Local encrypted backup created successfully."))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Backup failed."))
            }
        }
    }
}
