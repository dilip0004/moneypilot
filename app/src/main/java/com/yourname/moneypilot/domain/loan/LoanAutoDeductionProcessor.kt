package com.yourname.moneypilot.domain.loan

import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber

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

            if (asOf.isBefore(currentMonthDue)) continue

            // Section 16.0: Hardened Idempotency (TASK-40)
            // Use deterministic UUIDs based on the month/year to prevent double-counting
            val monthStr = YearMonth.from(currentMonthDue).toString()
            val baseId = "EMI_${loan.id}_$monthStr"
            val interestTxId = UUID.nameUUIDFromBytes("${baseId}_INT".toByteArray()).toString()
            val principalTxId = UUID.nameUUIDFromBytes("${baseId}_PRIN".toByteArray()).toString()

            // 1. Check if already exists (Optimization, but DB Primary Key is the real guard)
            val existing = transactionRepository.getTransactionById(interestTxId) ?: 
                           transactionRepository.getTransactionById(principalTxId)
            if (existing != null) continue

            // 2. Calculate Split
            val monthlyRate = loan.interestRate / 12.0 / 100.0
            val interestAmount = loan.currentBalance * monthlyRate
            val principalAmount = (loan.monthlyPayment - interestAmount).coerceIn(0.0, loan.currentBalance)

            // 3. Commit Transactions
            if (interestAmount > 0) {
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        id = interestTxId,
                        walletFromId = loan.linkedWalletId,
                        loanId = loan.id,
                        type = TransactionType.Expense,
                        amount = interestAmount,
                        note = "EMI Interest: ${loan.name} ($monthStr)",
                        dateTime = LocalDateTime.now(),
                        transactionSourceType = "AUTO_EMI_INTEREST"
                    )
                )
            }

            if (principalAmount > 0) {
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        id = principalTxId,
                        walletFromId = loan.linkedWalletId,
                        loanId = loan.id,
                        type = TransactionType.Expense,
                        amount = principalAmount,
                        note = "EMI Principal: ${loan.name} ($monthStr)",
                        dateTime = LocalDateTime.now(),
                        transactionSourceType = "AUTO_EMI_PRINCIPAL"
                    )
                )
            }

            // 4. Log audit event
            loanRepository.insertLoanEvent(
                LoanEventEntity(
                    loanId = loan.id,
                    eventType = "REPAYMENT_POSTED",
                    eventDate = LocalDate.now(),
                    amount = loan.monthlyPayment,
                    note = "Auto-Processed EMI for $monthStr"
                )
            )

            Timber.i("LoanAutoDeduction: Deterministic processing complete for ${loan.name} ($monthStr)")
        }
    }
}
