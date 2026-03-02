package com.yourname.moneypilot.domain.usecase.ledger

import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class IntegrityMismatch(
    val walletId: Long,
    val walletName: String,
    val storedBalance: Double,
    val calculatedBalance: Double
)

class VerifyLedgerIntegrityUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(): List<IntegrityMismatch> {
        val mismatches = mutableListOf<IntegrityMismatch>()
        val wallets = walletRepository.getAllWallets().first()

        for (wallet in wallets) {
            val transactions = transactionRepository.getTransactionsForWallet(wallet.id)
            
            var calculatedBalance = wallet.initialBalance
            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.Income -> {
                        if (tx.walletToId == wallet.id) calculatedBalance += tx.amount
                    }
                    TransactionType.Expense -> {
                        if (tx.walletFromId == wallet.id) calculatedBalance -= tx.amount
                    }
                    TransactionType.Transfer -> {
                        if (tx.walletFromId == wallet.id) calculatedBalance -= tx.amount
                        if (tx.walletToId == wallet.id) calculatedBalance += tx.amount
                    }
                }
            }

            // Compare with a tolerance for floating point inaccuracies
            if (kotlin.math.abs(wallet.currentBalance - calculatedBalance) > 0.01) {
                mismatches.add(
                    IntegrityMismatch(
                        walletId = wallet.id,
                        walletName = wallet.name,
                        storedBalance = wallet.currentBalance,
                        calculatedBalance = calculatedBalance
                    )
                )
            }
        }
        return mismatches
    }
}
