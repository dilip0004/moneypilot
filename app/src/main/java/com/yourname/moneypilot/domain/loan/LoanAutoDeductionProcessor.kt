package com.yourname.moneypilot.domain.loan

import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
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

            // Idempotency check: already processed this month?
            val startOfMonth = currentMonthDue.withDayOfMonth(1).atStartOfDay()
            val endOfMonth = currentMonthDue.withDayOfMonth(currentMonthDue.lengthOfMonth()).atTime(23, 59)
            val existingTxs = transactionRepository.getTransactionsForWallet(loan.linkedWalletId!!)
                .filter {
                    it.loanId == loan.id &&
                            it.transactionSourceType == "LOAN_REPAYMENT" &&
                            it.dateTime.isAfter(startOfMonth) &&
                            it.dateTime.isBefore(endOfMonth)
                }
            if (existingTxs.isNotEmpty()) continue

            // Single transaction for the full EMI
            transactionRepository.insertTransaction(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    walletFromId = loan.linkedWalletId,
                    loanId = loan.id,
                    type = TransactionType.Expense,
                    amount = loan.monthlyPayment,
                    note = "EMI: ${loan.name}",
                    dateTime = LocalDateTime.now(),
                    transactionSourceType = "LOAN_REPAYMENT"
                )
            )

            // Update loan balance
            val newBalance = (loan.currentBalance - loan.monthlyPayment).coerceAtLeast(0.0)
            loanRepository.updateLoan(loan.copy(currentBalance = newBalance))

            // Log audit event
            loanRepository.insertLoanEvent(
                LoanEventEntity(
                    loanId = loan.id,
                    eventType = "REPAYMENT_POSTED",
                    eventDate = LocalDate.now(),
                    amount = loan.monthlyPayment,
                    note = "Automated EMI processed"
                )
            )

            Timber.i("LoanAutoDeduction: Processed single EMI for ${loan.name}")
        }
    }
}