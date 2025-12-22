package com.yourname.moneypilot.domain.usecase.distribution

import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.DistributionRepository
import com.yourname.moneypilot.domain.usecase.transaction.CalculateMonthlySummaryUseCase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject

data class DistributionAction(
    val sourceWalletId: Long,
    val targetWalletId: Long,
    val amount: Double,
    val sourceName: String,
    val targetName: String
)

class GenerateLeftoverPlanUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val distributionRepository: DistributionRepository,
    private val calculateMonthlySummaryUseCase: CalculateMonthlySummaryUseCase
) {
    suspend operator fun invoke(month: Int, year: Int): List<DistributionAction> {
        val summary = calculateMonthlySummaryUseCase(month, year).first()
        val accounts = accountRepository.getAllAccounts().first()
        val rules = distributionRepository.getAllRules().first()
        
        // Strategy A: Income - Expenses surplus
        val surplus = (summary.totalIncome - summary.totalExpense).coerceAtLeast(0.0)
        
        // Find primary wallet for source (Principle 6 says base on primary wallet or total surplus)
        val primaryWallet = accounts.find { it.isPrimary } ?: accounts.firstOrNull() ?: return emptyList()
        
        val actions = mutableListOf<DistributionAction>()
        var remainingToDistribute = surplus
        
        // Apply fixed amount rules first
        rules.filter { it.fixedAmount != null }.sortedByDescending { it.priority }.forEach { rule ->
            val amount = rule.fixedAmount!!
            if (remainingToDistribute >= amount) {
                val targetAccount = accounts.find { it.id == rule.targetWalletId }
                if (targetAccount != null) {
                    actions.add(DistributionAction(
                        sourceWalletId = primaryWallet.id,
                        targetWalletId = targetAccount.id,
                        amount = amount,
                        sourceName = primaryWallet.name,
                        targetName = targetAccount.name
                    ))
                    remainingToDistribute -= amount
                }
            }
        }
        
        // Apply percentage rules to what's left
        val totalPercentageSurplus = remainingToDistribute
        rules.filter { it.percentage != null }.sortedByDescending { it.priority }.forEach { rule ->
            val amount = (rule.percentage!! / 100.0) * totalPercentageSurplus
            val targetAccount = accounts.find { it.id == rule.targetWalletId }
            if (targetAccount != null && amount > 0) {
                actions.add(DistributionAction(
                    sourceWalletId = primaryWallet.id,
                    targetWalletId = targetAccount.id,
                    amount = amount,
                    sourceName = primaryWallet.name,
                    targetName = targetAccount.name
                ))
            }
        }
        
        return actions
    }
}
