package com.yourname.moneypilot.domain.loan

import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.LocalDateTime
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
            val events = loanRepository.getLoanEventsOnce(loan.id)

            // Avoid double posting
            val postedDates = events
                .filter { it.eventType == "REPAYMENT_POSTED" }
                .map { it.eventDate }
                .toSet()

            var cursor = loan.startDate
            while (!cursor.isAfter(asOf)) {
                val dueDate = cursor.withDayOfMonth(
                    minOf(loan.startDate.dayOfMonth, cursor.lengthOfMonth())
                )

                if (!dueDate.isAfter(asOf) && dueDate !in postedDates) {
                    val tx = TransactionEntity(
                        accountId = loan.accountId ?: 0L,
                        categoryId = null,
                        subcategoryId = null,
                        goalId = null,
                        loanId = loan.id,
                        type = "LOAN_REPAYMENT",
                        amount = max(0.0, loan.monthlyPayment),
                        description = "EMI - ${loan.name}",
                        date = LocalDateTime.of(dueDate.year, dueDate.month, dueDate.dayOfMonth, 9, 0),
                        isRecurring = false
                    )

                    // ✅ FIX: handle nullable return (Long?)
                    transactionRepository.insertTransaction(tx) ?: -1L

                    loanRepository.addLoanEvent(
                        LoanEventEntity(
                            loanId = loan.id,
                            eventType = "REPAYMENT_POSTED",
                            eventDate = dueDate,
                            amount = tx.amount,
                            note = "Auto-deducted EMI"
                        )
                    )
                }

                cursor = cursor.plusMonths(1)
            }
        }
    }
}
