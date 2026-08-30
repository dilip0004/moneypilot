package com.yourname.moneypilot.ui.features.transactions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.ui.theme.motion.MotionConstants
import androidx.compose.animation.core.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TransactionIntentCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(MotionConstants.DurationButton),
        label = "intent_card_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun TransactionDateTimeField(
    dateTime: LocalDateTime,
    isConfirmed: Boolean,
    onClick: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (!isConfirmed) Color.Red.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f)
    val borderColor = if (!isConfirmed) Color.Red else Color.White.copy(alpha = 0.1f)
    
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth, 
                contentDescription = null, 
                tint = if (!isConfirmed) Color.Red else Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Date & Time ${if(!isConfirmed) "(Action Required)" else ""}", 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!isConfirmed) Color.Red else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateTime.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy - HH:mm")),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            if (!isConfirmed) {
                Button(
                    onClick = onConfirm,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDropdownSelector(
    selectedWalletId: Long?,
    wallets: List<WalletEntity>,
    onWalletSelected: (Long) -> Unit,
    label: String = "Account / Wallet",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedWallet = wallets.find { it.id == selectedWalletId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedWallet?.name ?: "Select Wallet",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = glassTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            wallets.forEach { wallet ->
                DropdownMenuItem(
                    text = { Text(wallet.name) },
                    onClick = {
                        onWalletSelected(wallet.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdownSelector(
    selectedCategoryId: Long?,
    categories: List<CategoryEntity>,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "Select Category",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = selectedCategoryId == null,
            colors = glassTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.icon} ${category.name}") },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun AmountField(
    amount: String,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = amount,
            onValueChange = { },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            prefix = { Text("$currencySymbol ") },
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            ),
            colors = glassTextFieldColors()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryDropdownSelector(
    selectedSubcategoryId: Long?,
    subcategories: List<SubcategoryEntity>,
    onSubcategorySelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSub = subcategories.find { it.id == selectedSubcategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSub?.name ?: "None",
            onValueChange = {},
            readOnly = true,
            label = { Text("Subcategory (Optional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = glassTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSubcategorySelected(null); expanded = false })
            subcategories.forEach { sub ->
                DropdownMenuItem(
                    text = { Text(sub.name) },
                    onClick = {
                        onSubcategorySelected(sub.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Description / Note") },
        modifier = modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences
        ),
        shape = RoundedCornerShape(12.dp),
        colors = glassTextFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDropdownSelector(
    selectedGoalId: Long?,
    goals: List<GoalEntity>,
    onGoalSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGoal = goals.find { it.id == selectedGoalId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedGoal?.let { "${it.icon} ${it.name}" } ?: "Link to Goal (Optional)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Savings Goal") },
            leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color.White.copy(alpha = 0.6f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = glassTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onGoalSelected(null); expanded = false })
            goals.forEach { goal ->
                DropdownMenuItem(
                    text = { Text("${goal.icon} ${goal.name}") },
                    onClick = {
                        onGoalSelected(goal.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDropdownSelector(
    selectedLoanId: Long?,
    loans: List<LoanEntity>,
    onLoanSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLoan = loans.find { it.id == selectedLoanId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLoan?.name ?: "Link to Loan / Debt",
            onValueChange = {},
            readOnly = true,
            label = { Text("Liability / Loan") },
            leadingIcon = { Icon(Icons.Default.CreditScore, null, tint = Color.White.copy(alpha = 0.6f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = glassTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onLoanSelected(null); expanded = false })
            loans.forEach { loan ->
                DropdownMenuItem(
                    text = { Text(loan.name) },
                    onClick = {
                        onLoanSelected(loan.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDropdownSelector(
    selectedInvestmentId: Long?,
    investments: List<InvestmentEntity>,
    onInvestmentSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedInv = investments.find { it.id == selectedInvestmentId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedInv?.name ?: "Link to Investment Asset",
            onValueChange = {},
            readOnly = true,
            label = { Text("Asset / Investment") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White.copy(alpha = 0.6f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = glassTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onInvestmentSelected(null); expanded = false })
            investments.forEach { inv ->
                DropdownMenuItem(
                    text = { Text(inv.name) },
                    onClick = {
                        onInvestmentSelected(inv.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
    focusedContainerColor = Color.Black.copy(alpha = 0.4f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedTextColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedLabelColor = Color.White,
    cursorColor = Color.White
)
