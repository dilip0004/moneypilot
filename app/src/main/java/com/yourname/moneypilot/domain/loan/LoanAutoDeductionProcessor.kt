package com.yourname.moneypilot.domain.loan

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import timber.log.Timber

/**
 * Modern Automation Engine for Loan Repayments.
 * Ensures Idempotency (Safe to run multiple times).
 */
class LoanAutoDeductionProcessor @Inject constructor(
    private val loanRepository: LoanRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun process(asOf: LocalDate = LocalDate.now()) {
        val loans = loanRepository.getAllLoansOnce()
            .filter { it.status == "ACTIVE" && it.type == "BORROWED" && it.linkedWalletId != null }

        for (loan in loans) {
            val dueDay = loan.repaymentDayOfMonth
            val currentMonthDue = asOf.withDayOfMonth(minOf(dueDay, asOf.lengthOfMonth()))

            // 1. Check if we are at or past the due date for this month
            if (asOf.isBefore(currentMonthDue)) continue

            // 2. IDEMPOTENCY CHECK: Search for existing automated EMI transactions for this specific month and loan
            val startOfMonth = currentMonthDue.withDayOfMonth(1).atStartOfDay()
            val endOfMonth = currentMonthDue.withDayOfMonth(currentMonthDue.lengthOfMonth()).atTime(23, 59)
            
            // Check transactions specifically for this loan and source type in current month
            val existingTxs = transactionRepository.getTransactionsForWallet(loan.linkedWalletId!!)
                .filter { 
                    it.loanId == loan.id && 
                    it.transactionSourceType.startsWith("AUTO_EMI") &&
                    it.dateTime.isAfter(startOfMonth) && 
                    it.dateTime.isBefore(endOfMonth) 
                }

            if (existingTxs.isNotEmpty()) {
                Timber.d("LoanAutoDeduction: EMI already processed for ${loan.name} this month. Skipping.")
                continue
            }

            // 3. Perform Split Deduction (Section 5.3)
            val annualRate = loan.interestRate
            val monthlyRate = annualRate / 12.0 / 100.0
            
            val interestAmount = loan.currentBalance * monthlyRate
            val principalAmount = max(0.0, loan.monthlyPayment - interestAmount)

            // Post Interest Expense
            val interestTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = loan.linkedWalletId,
                type = TransactionType.Expense,
                amount = interestAmount,
                note = "EMI Interest: ${loan.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "AUTO_EMI_INTEREST"
            )

            // Post Principal Repayment
            val principalTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = loan.linkedWalletId,
                loanId = loan.id,
                type = TransactionType.Expense,
                amount = principalAmount,
                note = "EMI Principal: ${loan.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "AUTO_EMI_PRINCIPAL"
            )

            transactionRepository.insertTransaction(interestTx)
            transactionRepository.insertTransaction(principalTx)
            
            Timber.i("LoanAutoDeduction: Processed EMI split for ${loan.name}")
        }
    }
}
