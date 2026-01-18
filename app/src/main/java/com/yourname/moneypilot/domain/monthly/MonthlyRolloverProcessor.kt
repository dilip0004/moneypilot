package com.yourname.moneypilot.domain.monthly

import com.yourname.moneypilot.data.local.database.entities.MonthlyAccountSnapshotEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.MonthlySnapshotRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Creates monthly account snapshots.
 *
 * Uses repositories only (no direct DAO injection) so Hilt can resolve dependencies cleanly.
 * Idempotent via upsert (REPLACE).
 */
class MonthlyRolloverProcessor @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val snapshotRepository: MonthlySnapshotRepository
) {

    suspend fun process(asOf: LocalDate = LocalDate.now()) {
        // Always ensure last month snapshot exists, and create current month opening snapshot.
        val lastMonth = YearMonth.from(asOf.minusMonths(1))
        ensureMonth(lastMonth, openingOnly = false)

        val current = YearMonth.from(asOf)
        ensureMonth(current, openingOnly = true)
    }

    private suspend fun ensureMonth(month: YearMonth, openingOnly: Boolean) {
        val monthKey = month.toString() // yyyy-MM
        val accounts = accountRepository.getAllAccountsOnce()

        val start = month.atDay(1).atStartOfDay()
        val end = month.atEndOfMonth().atTime(23, 59, 59)

        for (acc in accounts) {
            val openingAsOf = start.minusSeconds(1)
            val opening = transactionRepository.getAccountBalanceAt(acc.id, openingAsOf)

            if (openingOnly) {
                snapshotRepository.upsert(
                    MonthlyAccountSnapshotEntity(
                        accountId = acc.id,
                        month = monthKey,
                        openingBalance = opening,
                        closingBalance = opening,
                        incomeTotal = 0.0,
                        expenseTotal = 0.0,
                        netChange = 0.0
                    )
                )
            } else {
                val closing = transactionRepository.getAccountBalanceAt(acc.id, end)
                val income = transactionRepository.getAccountIncomeInRange(acc.id, start, end)
                val expense = transactionRepository.getAccountExpenseInRange(acc.id, start, end)

                snapshotRepository.upsert(
                    MonthlyAccountSnapshotEntity(
                        accountId = acc.id,
                        month = monthKey,
                        openingBalance = opening,
                        closingBalance = closing,
                        incomeTotal = income,
                        expenseTotal = expense,
                        netChange = closing - opening
                    )
                )
            }
        }
    }
}
