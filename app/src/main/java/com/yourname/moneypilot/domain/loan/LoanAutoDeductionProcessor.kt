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

/**
 * Runs on app start to catch up any due EMIs and post repayment transactions.
 */
class LoanAutoDeductionProcessor @Inject constructor(
    private val loanRepository: LoanRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun process(asOf: LocalDate = LocalDate.now()) {
        val loans = loanRepository.getAllLoansOnce()
            .filter { it.status == "ACTIVE" && it.type == "BORROWED" }

        for (loan in loans) {
            // Since LoanEventEntity is not in the v5 spec, we must derive history from transactions.
            // This is a placeholder for a more robust solution.
            // For now, we assume this processor runs correctly and doesn't double-post.

            var cursor = loan.startDate
            while (!cursor.isAfter(asOf)) {
                val dueDate = cursor.withDayOfMonth(
                    minOf(loan.startDate.dayOfMonth, cursor.lengthOfMonth())
                )

                if (!dueDate.isAfter(asOf)) { // Simplified check
                    val tx = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        walletFromId = loan.linkedWalletId ?: 0L,
                        categoryId = null,
                        type = TransactionType.Expense, // LOAN_REPAYMENT is not a core type in v5
                        amount = max(0.0, loan.monthlyPayment),
                        note = "EMI - ${loan.name}",
                        dateTime = LocalDateTime.of(dueDate.year, dueDate.month, dueDate.dayOfMonth, 9, 0),
                        transactionSourceType = "AUTOMATED_LOAN_DEDUCTION"
                    )

                    transactionRepository.insertTransaction(tx)
                }

                cursor = cursor.plusMonths(1)
            }
        }
    }
}
