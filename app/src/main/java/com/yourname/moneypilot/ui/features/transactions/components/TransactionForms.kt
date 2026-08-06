package com.yourname.moneypilot.ui.features.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionEvent
import com.yourname.moneypilot.ui.features.transactions.AddEditTransactionState

@Composable
fun ExpenseForm(
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TransactionDateTimeField(
            dateTime = state.date,
            isConfirmed = state.isDateConfirmed,
            onClick = { /* Handled by screen */ },
            onConfirm = { onEvent(AddEditTransactionEvent.ConfirmDate) }
        )

        WalletDropdownSelector(
            selectedWalletId = state.walletFromId,
            wallets = state.wallets,
            onWalletSelected = { onEvent(AddEditTransactionEvent.WalletChanged(it)) }
        )

        CategoryDropdownSelector(
            selectedCategoryId = state.categoryId,
            categories = state.categories,
            onCategorySelected = { onEvent(AddEditTransactionEvent.CategoryChanged(it)) }
        )

        if (state.subcategories.isNotEmpty()) {
            SubcategoryDropdownSelector(
                selectedSubcategoryId = state.subcategoryId,
                subcategories = state.subcategories,
                onSubcategorySelected = { onEvent(AddEditTransactionEvent.SubcategoryChanged(it)) }
            )
        }

        AmountField(
            amount = state.amount,
            currencySymbol = currencySymbol,
            onClick = onShowCalculator
        )

        DescriptionField(
            value = state.description,
            onValueChange = { onEvent(AddEditTransactionEvent.EnteredDescription(it)) }
        )
    }
}

@Composable
fun IncomeForm(
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TransactionDateTimeField(
            dateTime = state.date,
            isConfirmed = state.isDateConfirmed,
            onClick = { /* Handled by screen */ },
            onConfirm = { onEvent(AddEditTransactionEvent.ConfirmDate) }
        )

        WalletDropdownSelector(
            selectedWalletId = state.walletFromId,
            wallets = state.wallets,
            onWalletSelected = { onEvent(AddEditTransactionEvent.WalletChanged(it)) },
            label = "Deposit To"
        )

        CategoryDropdownSelector(
            selectedCategoryId = state.categoryId,
            categories = state.categories,
            onCategorySelected = { onEvent(AddEditTransactionEvent.CategoryChanged(it)) }
        )

        AmountField(
            amount = state.amount,
            currencySymbol = currencySymbol,
            onClick = onShowCalculator
        )

        DescriptionField(
            value = state.description,
            onValueChange = { onEvent(AddEditTransactionEvent.EnteredDescription(it)) }
        )
    }
}

@Composable
fun GoalForm(
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TransactionDateTimeField(
            dateTime = state.date,
            isConfirmed = state.isDateConfirmed,
            onClick = { /* Handled by screen */ },
            onConfirm = { onEvent(AddEditTransactionEvent.ConfirmDate) }
        )

        GoalDropdownSelector(
            selectedGoalId = state.goalId,
            goals = state.goals,
            onGoalSelected = { onEvent(AddEditTransactionEvent.GoalChanged(it)) }
        )

        WalletDropdownSelector(
            selectedWalletId = state.walletFromId,
            wallets = state.wallets,
            onWalletSelected = { onEvent(AddEditTransactionEvent.WalletChanged(it)) },
            label = "Funding Account"
        )

        AmountField(
            amount = state.amount,
            currencySymbol = currencySymbol,
            onClick = onShowCalculator
        )

        DescriptionField(
            value = state.description,
            onValueChange = { onEvent(AddEditTransactionEvent.EnteredDescription(it)) }
        )
    }
}

@Composable
fun LoanForm(
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TransactionDateTimeField(
            dateTime = state.date,
            isConfirmed = state.isDateConfirmed,
            onClick = { /* Handled by screen */ },
            onConfirm = { onEvent(AddEditTransactionEvent.ConfirmDate) }
        )

        LoanDropdownSelector(
            selectedLoanId = state.loanId,
            loans = state.loans,
            onLoanSelected = { onEvent(AddEditTransactionEvent.LoanChanged(it)) }
        )

        WalletDropdownSelector(
            selectedWalletId = state.walletFromId,
            wallets = state.wallets,
            onWalletSelected = { onEvent(AddEditTransactionEvent.WalletChanged(it)) },
            label = "Payment Account"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.isInterestPosting,
                onClick = { onEvent(AddEditTransactionEvent.ToggleInterestPosting(!state.isInterestPosting)) },
                label = { Text("Interest Only", fontSize = 11.sp) },
                leadingIcon = if (state.isInterestPosting) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        AmountField(
            amount = state.amount,
            currencySymbol = currencySymbol,
            onClick = onShowCalculator
        )

        DescriptionField(
            value = state.description,
            onValueChange = { onEvent(AddEditTransactionEvent.EnteredDescription(it)) }
        )
    }
}

@Composable
fun InvestmentForm(
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TransactionDateTimeField(
            dateTime = state.date,
            isConfirmed = state.isDateConfirmed,
            onClick = { /* Handled by screen */ },
            onConfirm = { onEvent(AddEditTransactionEvent.ConfirmDate) }
        )

        InvestmentDropdownSelector(
            selectedInvestmentId = state.investmentId,
            investments = state.investments,
            onInvestmentSelected = { onEvent(AddEditTransactionEvent.InvestmentChanged(it)) }
        )

        WalletDropdownSelector(
            selectedWalletId = state.walletFromId,
            wallets = state.wallets,
            onWalletSelected = { onEvent(AddEditTransactionEvent.WalletChanged(it)) }
        )

        AmountField(
            amount = state.amount,
            currencySymbol = currencySymbol,
            onClick = onShowCalculator
        )

        DescriptionField(
            value = state.description,
            onValueChange = { onEvent(AddEditTransactionEvent.EnteredDescription(it)) }
        )
    }
}

@Composable
fun RefundForm(
    state: AddEditTransactionState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onShowCalculator: () -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TransactionDateTimeField(
            dateTime = state.date,
            isConfirmed = state.isDateConfirmed,
            onClick = { /* Handled by screen */ },
            onConfirm = { onEvent(AddEditTransactionEvent.ConfirmDate) }
        )

        WalletDropdownSelector(
            selectedWalletId = state.walletFromId,
            wallets = state.wallets,
            onWalletSelected = { onEvent(AddEditTransactionEvent.WalletChanged(it)) },
            label = "Refund To"
        )

        CategoryDropdownSelector(
            selectedCategoryId = state.categoryId,
            categories = state.categories,
            onCategorySelected = { onEvent(AddEditTransactionEvent.CategoryChanged(it)) }
        )

        AmountField(
            amount = state.amount,
            currencySymbol = currencySymbol,
            onClick = onShowCalculator
        )

        DescriptionField(
            value = state.description,
            onValueChange = { onEvent(AddEditTransactionEvent.EnteredDescription(it)) }
        )
        
        Text(
            text = "Note: This will be recorded as an expense reversal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
