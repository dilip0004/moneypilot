package com.yourname.moneypilot.domain.monthly

import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
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
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    suspend fun process(asOf: LocalDate = LocalDate.now()) {
        // This processor's original purpose (creating snapshots) is deprecated as per v5 architecture.
        // The logic is being stubbed out. Future tasks will redefine its purpose, likely for budget rollovers.
        val lastMonth = YearMonth.from(asOf.minusMonths(1))
        ensureMonth(lastMonth, openingOnly = false)

        val current = YearMonth.from(asOf)
        ensureMonth(current, openingOnly = true)
    }

    private suspend fun ensureMonth(month: YearMonth, openingOnly: Boolean) {
        // Stubbed out. No-op.
    }
}
