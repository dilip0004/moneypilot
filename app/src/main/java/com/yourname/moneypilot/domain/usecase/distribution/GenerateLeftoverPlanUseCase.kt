package com.yourname.moneypilot.domain.usecase.distribution

import com.yourname.moneypilot.data.repository.DistributionRuleRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.domain.usecase.transaction.CalculateMonthlySummaryUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class DistributionAction(
    val sourceWalletId: Long,
    val targetWalletId: Long,
    val amount: Double,
    val sourceName: String,
    val targetName: String
)

class GenerateLeftoverPlanUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
    private val distributionRuleRepository: DistributionRuleRepository,
    private val calculateMonthlySummaryUseCase: CalculateMonthlySummaryUseCase
) {
    suspend operator fun invoke(month: Int, year: Int): List<DistributionAction> {
        val summary = calculateMonthlySummaryUseCase(month, year).first()
        val wallets = walletRepository.getAllWallets().first()
        val rules = distributionRuleRepository.getAllRules().first()
        
        val surplus = (summary.totalIncome - summary.totalExpense).coerceAtLeast(0.0)
        
        val primaryWallet = wallets.find { it.isPrimary } ?: wallets.firstOrNull() ?: return emptyList()
        
        val actions = mutableListOf<DistributionAction>()
        var remainingToDistribute = surplus
        
        rules.filter { it.fixedAmount != null }.sortedByDescending { it.priority }.forEach { rule ->
            val amount = requireNotNull(rule.fixedAmount)
            if (remainingToDistribute >= amount) {
                val targetWallet = wallets.find { it.id == rule.targetWalletId }
                if (targetWallet != null) {
                    actions.add(DistributionAction(
                        sourceWalletId = primaryWallet.id,
                        targetWalletId = targetWallet.id,
                        amount = amount,
                        sourceName = primaryWallet.name,
                        targetName = targetWallet.name
                    ))
                    remainingToDistribute -= amount
                }
            }
        }
        
        val totalPercentageSurplus = remainingToDistribute
        rules.filter { it.percentage != null }.sortedByDescending { it.priority }.forEach { rule ->
            val amount = (requireNotNull(rule.percentage) / 100.0) * totalPercentageSurplus
            val targetWallet = wallets.find { it.id == rule.targetWalletId }
            if (targetWallet != null && amount > 0) {
                actions.add(DistributionAction(
                    sourceWalletId = primaryWallet.id,
                    targetWalletId = targetWallet.id,
                    amount = amount,
                    sourceName = primaryWallet.name,
                    targetName = targetWallet.name
                ))
            }
        }
        
        return actions
    }
}
