package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Encapsulates the logic for saving or updating a transaction.
 * Adheres to Section 2.1 (Flow Layer) and Section 5.3 (EMI Splitting) of the v5 architecture.
 */
class SaveTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val loanRepository: LoanRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity, isEdit: Boolean, tagIds: List<Long> = emptyList()) {
        // Section 5.3: EMI Splitting Logic
        if (transaction.loanId != null && transaction.transactionSourceType == "MANUAL" && transaction.type == TransactionType.Expense) {
            handleSplitLoanRepayment(transaction, isEdit, tagIds)
        } else {
            if (isEdit) {
                transactionRepository.updateTransaction(transaction, tagIds)
            } else {
                transactionRepository.insertTransaction(transaction, tagIds)
            }
        }
    }

    private suspend fun handleSplitLoanRepayment(transaction: TransactionEntity, isEdit: Boolean, tagIds: List<Long>) {
        val loan = loanRepository.getLoanById(transaction.loanId!!) ?: return

        // 1. Calculate the Interest component based on current balance
        val monthlyRate = loan.interestRate / 12.0 / 100.0
        val interestAmount = loan.currentBalance * monthlyRate
        
        // Ensure we don't interest-charge more than the payment itself
        val actualInterest = interestAmount.coerceAtMost(transaction.amount)
        val principalAmount = (transaction.amount - actualInterest).coerceAtLeast(0.0)

        // 2. Prepare the split entities
        val interestTx = transaction.copy(
            id = if (isEdit) transaction.id else UUID.randomUUID().toString(),
            amount = actualInterest,
            note = "EMI Interest: ${transaction.note ?: loan.name}",
            transactionSourceType = "AUTO_EMI_INTEREST"
        )

        val principalTx = transaction.copy(
            id = UUID.randomUUID().toString(),
            amount = principalAmount,
            note = "EMI Principal: ${transaction.note ?: loan.name}",
            transactionSourceType = "AUTO_EMI_PRINCIPAL"
        )

        // 3. Commit to repository
        if (isEdit) {
            val oldTx = transactionRepository.getTransactionById(transaction.id)
            if (oldTx != null) {
                transactionRepository.deleteTransaction(oldTx)
            }
        }

        // Insert both parts (share same tags if applicable)
        if (actualInterest > 0) {
            transactionRepository.insertTransaction(interestTx, tagIds)
        }
        if (principalAmount > 0) {
            transactionRepository.insertTransaction(principalTx, tagIds)
        }
    }
}
