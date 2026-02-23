package com.yourname.moneypilot.ui.features.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// use MaterialTheme.colorScheme.income / expense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onPopBackStack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onPopBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runAllTests() },
                        enabled = !state.isRunning
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run All")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "System Integrity Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Run end-to-end tests to verify financial logic and data safety.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(state.testCases) { testCase ->
                    TestCaseItem(testCase)
                }
            }
        }
    }
}

@Composable
fun TestCaseItem(test: TestCase) {
    val statusColor by animateColorAsState(
        targetValue = when (test.status) {
            TestStatus.PASSED -> MaterialTheme.colorScheme.income
            TestStatus.FAILED -> MaterialTheme.colorScheme.expense
            TestStatus.RUNNING -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        },
        label = "status_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = test.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = test.description,
                    style = MaterialTheme.typography.bodySmall
                )
                if (test.status == TestStatus.FAILED && test.errorMessage != null) {
                        Text(
                        text = "Error: ${test.errorMessage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.expense,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            when (test.status) {
                TestStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                TestStatus.PASSED -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.income)
                TestStatus.FAILED -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.expense)
                else -> Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}
