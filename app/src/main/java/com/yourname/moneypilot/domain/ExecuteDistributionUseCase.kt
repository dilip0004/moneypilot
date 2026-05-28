package com.yourname.moneypilot.domain

import androidx.room.withTransaction
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.DistributionRuleRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber

/**
 * Enterprise Architecture v5.0 Compliance:
 * Implements the "Automated Surplus Distribution" logic.
 * Ensures all distributions are ledger-backed and atomic.
 */
class ExecuteDistributionUseCase @Inject constructor(
    private val database: MoneyPilotDatabase,
    private val transactionRepository: TransactionRepository,
    private val distributionRuleRepository: DistributionRuleRepository
) {

    suspend operator fun invoke(yearMonth: YearMonth) {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)

        // 1. Calculate the real surplus from the ledger
        val income = transactionRepository.getTotalSumByType(TransactionType.Income, startOfMonth, endOfMonth) ?: 0.0
        val expense = transactionRepository.getTotalSumByType(TransactionType.Expense, startOfMonth, endOfMonth) ?: 0.0

        var availableSurplus = income - expense

        if (availableSurplus <= 0) {
            Timber.i("Distribution: No surplus detected for $yearMonth ($availableSurplus). Skipping.")
            return
        }

        // 2. Fetch rules ordered by priority
        val rules = distributionRuleRepository.getAllRules().first().sortedBy { it.priority }
        if (rules.isEmpty()) return

        Timber.i("Distribution: Starting distribution of ₹$availableSurplus using ${rules.size} rules.")

        // 3. Execute batch transfers atomically
        database.withTransaction {
            for (rule in rules) {
                if (availableSurplus <= 0) break

                val amountToDistribute = when {
                    rule.fixedAmount != null && rule.fixedAmount > 0 -> 
                        rule.fixedAmount.coerceAtMost(availableSurplus)
                    rule.percentage != null && rule.percentage > 0 -> 
                        (availableSurplus * (rule.percentage / 100)).coerceAtMost(availableSurplus)
                    else -> 0.0
                }

                if (amountToDistribute > 1.0) { // Only distribute amounts > ₹1
                    val transfer = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        walletFromId = rule.sourceWalletId,
                        walletToId = rule.targetWalletId,
                        amount = amountToDistribute,
                        type = TransactionType.Transfer,
                        dateTime = LocalDateTime.now(),
                        transactionSourceType = "AUTOMATED_DISTRIBUTION",
                        note = "Surplus Distribution (Rule #${rule.id})"
                    )
                    
                    // This call is now atomic inside the transaction block
                    transactionRepository.createTransfer(transfer)
                    
                    availableSurplus -= amountToDistribute
                    Timber.d("Distribution: Transferred ₹$amountToDistribute from ${rule.sourceWalletId} to ${rule.targetWalletId}")
                }
            }
        }
        Timber.i("Distribution: Completed for $yearMonth.")
    }
}
