package com.yourname.moneypilot.ui.features.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.features.settings.SettingsItem
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onPopBackStack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showIntegrityDialog by remember { mutableStateOf<BackupViewModel.UiEvent.ShowIntegrityWarning?>(null) }
    var pendingJsonContent by remember { mutableStateOf<String?>(null) }
    var pendingFileName by remember { mutableStateOf<String?>(null) }

    // Launcher for Saving JSON (Export)
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let { targetUri ->
                pendingJsonContent?.let { content ->
                    try {
                        context.contentResolver.openOutputStream(targetUri)?.use { it.write(content.toByteArray()) }
                    } catch (e: Exception) {
                        // handled via UI or logging
                    }
                }
            }
            pendingJsonContent = null
            pendingFileName = null
        }
    )

    // Launcher for Reading JSON (Import/Restore)
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.restoreFromJson(it) }
        }
    )

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is BackupViewModel.UiEvent.FileReady -> {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", event.file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Transactions"))
                }
                is BackupViewModel.UiEvent.SaveJson -> {
                    pendingJsonContent = event.jsonContent
                    pendingFileName = event.fileName
                    saveFileLauncher.launch(event.fileName)
                }
                is BackupViewModel.UiEvent.ShowIntegrityWarning -> {
                    showIntegrityDialog = event
                }
                is BackupViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    if (showIntegrityDialog != null) {
        AlertDialog(
            onDismissRequest = { showIntegrityDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Integrity Mismatch Detected") },
            text = { Text("Your ledger has ${showIntegrityDialog!!.mismatchCount} inconsistencies (wallet balances don't match transaction history). Export anyway?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmBackupWithWarnings(showIntegrityDialog!!.jsonContent, showIntegrityDialog!!.fileName)
                        showIntegrityDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Export Regardless")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntegrityDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SettingsItem(
                title = "Export to CSV",
                subtitle = "Save transactions as a spreadsheet file",
                icon = Icons.Default.FileDownload,
                onClick = { viewModel.exportToCSV() }
            )
            SettingsItem(
                title = "Create JSON Backup",
                subtitle = "Export entire database for safe keeping",
                icon = Icons.Default.CloudDownload,
                onClick = { viewModel.exportToJson() }
            )
            SettingsItem(
                title = "Restore from JSON",
                subtitle = "Import data and rebuild your database",
                icon = Icons.Default.Restore,
                onClick = { openFileLauncher.launch(arrayOf("application/json")) }
            )
        }
    }
}
