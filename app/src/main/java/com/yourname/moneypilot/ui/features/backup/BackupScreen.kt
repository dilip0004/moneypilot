package com.yourname.moneypilot.ui.features.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.ui.components.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onPopBackStack: () -> Unit,
    onNavigateToImport: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showIntegrityDialog by remember { mutableStateOf<BackupViewModel.UiEvent.ShowIntegrityWarning?>(null) }
    var pendingJsonContent by remember { mutableStateOf<String?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let { targetUri ->
                pendingJsonContent?.let { content ->
                    try {
                        context.contentResolver.openOutputStream(targetUri)?.use { it.write(content.toByteArray()) }
                    } catch (e: Exception) { }
                }
            }
            pendingJsonContent = null
        }
    )

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
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Ledger"))
                }
                is BackupViewModel.UiEvent.SaveJson -> {
                    pendingJsonContent = event.jsonContent
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
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Integrity Mismatch Detected") },
            text = { Text("Your ledger has ${showIntegrityDialog!!.mismatchCount} inconsistencies. Export anyway?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmBackupWithWarnings(showIntegrityDialog!!.jsonContent, showIntegrityDialog!!.fileName)
                        showIntegrityDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Export") }
            },
            dismissButton = { TextButton(onClick = { showIntegrityDialog = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassTopBar(
                title = { Text("Data Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SettingsHeader("Import Tools")
            SettingsGroupSurface {
                SettingsItemRow(
                    title = "Bank Statement Import",
                    subtitle = "Import transactions from bank CSV files",
                    icon = Icons.Default.AccountBalance,
                    onClick = onNavigateToImport
                )
            }
            
            SettingsHeader("Export & Backup")
            SettingsGroupSurface {
                SettingsItemRow(
                    title = "Export to CSV",
                    subtitle = "Save ledger as a comma-separated file",
                    icon = Icons.Default.FileDownload,
                    onClick = { viewModel.exportToCSV() }
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "Export to Excel",
                    subtitle = "Professional ledger format (.xml/xlsx)",
                    icon = Icons.Default.Description,
                    onClick = { viewModel.exportToExcel() }
                )
                SettingsDivider()
                SettingsItemRow(
                    title = "Create JSON Backup",
                    subtitle = "Full database snapshot for safe keeping",
                    icon = Icons.Default.CloudDownload,
                    onClick = { viewModel.exportToJson() }
                )
            }
            
            SettingsHeader("Recovery")
            SettingsGroupSurface {
                SettingsItemRow(
                    title = "Restore from JSON",
                    subtitle = "Rebuild database from a backup file",
                    icon = Icons.Default.Restore,
                    onClick = { openFileLauncher.launch(arrayOf("application/json")) }
                )
            }
        }
    }
}
