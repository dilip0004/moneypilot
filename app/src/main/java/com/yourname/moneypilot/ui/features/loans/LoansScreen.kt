package com.yourname.moneypilot.ui.features.loans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.ui.common.ScreenState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    onLoanClick: (Long) -> Unit,
    onAddLoan: () -> Unit,
    viewModel: LoansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLoan) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        item {
                            LoanSummaryHeader(
                                borrowed = state.data.totalBorrowed,
                                lent = state.data.totalLent
                            )
                        }
                        
                        items(state.data.loans) { loan ->
                            var isExpanded by remember { mutableStateOf(false) }
                            
                            LoanItem(
                                loan = loan,
                                isExpanded = isExpanded,
                                onClick = { isExpanded = !isExpanded },
                                repayments = emptyList() 
                            )
                        }
                    }
                }
                is ScreenState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active loans or debts found.")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun LoanSummaryHeader(borrowed: Double, lent: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            // colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.expense.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Text("Borrowed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.expense)
                // Text("₹ $borrowed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.expense)
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            // colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.income.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Text("Lent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.income)
                // Text("₹ $lent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.income)
            }
        }
    }
}

@Composable
fun LoanItem(
    loan: LoanEntity,
    isExpanded: Boolean,
    onClick: () -> Unit,
    repayments: List<TransactionEntity>
) {
    val progress = ((loan.totalAmount - loan.currentBalance) / loan.totalAmount).toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = loan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "from ${loan.lender}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(
                    // containerColor = if (loan.type == "BORROWED") MaterialTheme.colorScheme.expense.copy(alpha = 0.2f) else MaterialTheme.colorScheme.income.copy(alpha = 0.2f),
                    // contentColor = if (loan.type == "BORROWED") MaterialTheme.colorScheme.expense else MaterialTheme.colorScheme.income
                ) {
                    Text(loan.type, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
                LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                // color = if (loan.type == "BORROWED") MaterialTheme.colorScheme.expense else MaterialTheme.colorScheme.income,
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹ ${loan.currentBalance}", fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Monthly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹ ${loan.monthlyPayment}", fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repayment History", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    
                    if (repayments.isEmpty()) {
                        Text(
                            "No repayment records found for this loan.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        repayments.forEach { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = record.dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "₹ ${record.amount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    // color = MaterialTheme.colorScheme.income
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
