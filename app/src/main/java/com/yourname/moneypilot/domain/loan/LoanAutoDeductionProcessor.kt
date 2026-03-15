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
import kotlinx.coroutines.flow.first

/**
 * Modern Automation Engine for Loan Repayments.
 * Responsible for identifying due EMIs and posting split transactions (Principal + Interest).
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

            // 2. Check if a repayment for this month has already been posted
            // We look for transactions with the specific automated source type for this loan in the current month
            val startOfMonth = currentMonthDue.withDayOfMonth(1).atStartOfDay()
            val endOfMonth = currentMonthDue.withDayOfMonth(currentMonthDue.lengthOfMonth()).atTime(23, 59)
            
            val existingTxs = transactionRepository.getTransactionsForWallet(loan.linkedWalletId!!)
                .filter { it.loanId == loan.id && it.dateTime.isAfter(startOfMonth) && it.dateTime.isBefore(endOfMonth) }

            if (existingTxs.isNotEmpty()) continue

            // 3. Perform Split Deduction (Section 5.3)
            val annualRate = loan.interestRate
            val monthlyRate = annualRate / 12.0 / 100.0
            
            val interestAmount = loan.currentBalance * monthlyRate
            val principalAmount = max(0.0, loan.monthlyPayment - interestAmount)

            // A. Post Interest as an Expense (Money leaving the system)
            val interestTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = loan.linkedWalletId,
                categoryId = null, // Optionally link to a "Loan Interest" category
                loanId = null, // Interest does not reduce the principal balance
                type = TransactionType.Expense,
                amount = interestAmount,
                note = "Interest Payment: ${loan.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "AUTO_EMI_INTEREST"
            )

            // B. Post Principal as a Repayment (Reduces liability)
            val principalTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = loan.linkedWalletId,
                loanId = loan.id, // This triggers the balance reduction in Repository
                type = TransactionType.Expense,
                amount = principalAmount,
                note = "Principal Repayment: ${loan.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "AUTO_EMI_PRINCIPAL"
            )

            transactionRepository.insertTransaction(interestTx)
            transactionRepository.insertTransaction(principalTx)
        }
    }
}
