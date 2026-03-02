package com.yourname.moneypilot.domain

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.DistributionRuleRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

class ExecuteDistributionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val distributionRuleRepository: DistributionRuleRepository
) {

    suspend operator fun invoke(yearMonth: YearMonth) {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)

        val income = transactionRepository.getTotalSumByType(TransactionType.Income, startOfMonth, endOfMonth) ?: 0.0
        val expense = transactionRepository.getTotalSumByType(TransactionType.Expense, startOfMonth, endOfMonth) ?: 0.0

        var surplus = income - expense

        if (surplus <= 0) return

        val rules = distributionRuleRepository.getAllRules().first()

        for (rule in rules) {
            if (surplus <= 0) break

            val amountToDistribute = when {
                rule.fixedAmount != null -> rule.fixedAmount.coerceAtMost(surplus)
                rule.percentage != null -> (surplus * (rule.percentage / 100)).coerceAtMost(surplus)
                else -> 0.0
            }

            if (amountToDistribute > 0) {
                val transfer = TransactionEntity(
                    walletFromId = rule.sourceWalletId,
                    walletToId = rule.targetWalletId,
                    amount = amountToDistribute,
                    type = TransactionType.Transfer,
                    dateTime = LocalDateTime.now(),
                    transactionSourceType = "AUTOMATED_DISTRIBUTION",
                    note = "Surplus Distribution"
                )
                transactionRepository.createTransfer(transfer)
                surplus -= amountToDistribute
            }
        }
    }
}
