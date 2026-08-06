package com.yourname.moneypilot.ui.features.loans

import androidx.compose.foundation.ExperimentalFoundationApi
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import com.yourname.moneypilot.ui.theme.motion.motionTween
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.ui.MainViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.util.formatCurrency
import com.yourname.moneypilot.util.rememberCurrencySymbol
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoansScreen(
    onLoanClick: (Long) -> Unit,
    searchQuery: String = "",
    viewModel: LoansViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by mainViewModel.userPreferences.collectAsState()
    val isPrivacyMode = preferences?.isPrivacyModeEnabled ?: false
    val currencySymbol = rememberCurrencySymbol()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ScreenState.Success -> {
                val filteredLoans = remember(state.data.loans, searchQuery) {
                    state.data.loans.filter { it.name.contains(searchQuery, ignoreCase = true) || it.lender.contains(searchQuery, ignoreCase = true) }
                }

                if (filteredLoans.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No loans matching '$searchQuery'")
                    }
                } else if (filteredLoans.isEmpty()) {
                    LoansEmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(
                            items = filteredLoans,
                            key = { it.id }
                        ) { loan ->
                            val index = filteredLoans.indexOf(loan)
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(index * MotionConstants.StaggerDelay.toLong())
                                visible = true
                            }

                            AnimatedVisibility(
                                visible = visible,
                                enter = slideInVertically(animationSpec = motionTween()) { 20 } + fadeIn(animationSpec = motionTween()),
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                var isExpanded by remember { mutableStateOf(false) }

                                CompactLoanItem(
                                    loan = loan,
                                    isExpanded = isExpanded,
                                    isPrivacyMode = isPrivacyMode,
                                    onClick = { isExpanded = !isExpanded },
                                    onEdit = { onLoanClick(loan.id) },
                                    repayments = emptyList(),
                                    currencySymbol = currencySymbol
                                )
                            }
                        }
                    }
                }
            }
            is ScreenState.Empty -> LoansEmptyState()
            else -> {}
        }
    }
}

@Composable
fun CompactLoanItem(
    loan: LoanEntity,
    isExpanded: Boolean,
    isPrivacyMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    repayments: List<TransactionEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val progress = ((loan.totalAmount - loan.currentBalance) / loan.totalAmount).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                }
                
                Spacer(Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = loan.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = loan.type,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                }

                // Balance
                val displayRemaining = if (isPrivacyMode) "••••" else loan.currentBalance.formatCurrency(currencySymbol)
                Text(
                    text = displayRemaining,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (loan.type == "BORROWED") MaterialTheme.colorScheme.error else Color(0xFF00C853),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color = if (loan.type == "BORROWED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val displayMonthly = if (isPrivacyMode) "••••" else loan.monthlyPayment.formatCurrency(currencySymbol)
                Text("EMI: $displayMonthly", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lender: ${loan.lender}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LoansEmptyState() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(animationSpec = motionTween()) + fadeIn(animationSpec = motionTween())
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = motionTween()) { 20 } + fadeIn(animationSpec = motionTween())
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No active loans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Add personal or bank loans to track repayments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
