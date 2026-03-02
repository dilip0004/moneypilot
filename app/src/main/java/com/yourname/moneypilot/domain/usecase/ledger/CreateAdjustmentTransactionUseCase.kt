package com.yourname.moneypilot.domain.usecase.ledger

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class CreateAdjustmentTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) {

    suspend operator fun invoke(walletId: Long, discrepancy: Double) {
        if (discrepancy == 0.0) return

        val wallet = walletRepository.getWalletById(walletId) ?: return

        val adjustmentType = if (discrepancy > 0) TransactionType.Income else TransactionType.Expense
        val adjustmentAmount = kotlin.math.abs(discrepancy)

        val adjustmentTransaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            dateTime = LocalDateTime.now(),
            amount = adjustmentAmount,
            type = adjustmentType,
            walletFromId = if (adjustmentType == TransactionType.Expense) wallet.id else null,
            walletToId = if (adjustmentType == TransactionType.Income) wallet.id else null,
            transactionSourceType = "RECONCILIATION_ADJUSTMENT",
            note = "Ledger integrity adjustment."
        )

        // This is a privileged operation. We insert the transaction and then
        // directly set the wallet balance to the reconciled value.
        transactionRepository.insertTransaction(adjustmentTransaction)
        val transactions = transactionRepository.getTransactionsForWallet(wallet.id)
        val calculatedBalance = wallet.initialBalance + transactions.sumOf { 
            when(it.type) {
                TransactionType.Income -> if (it.walletToId == wallet.id) it.amount else 0.0
                TransactionType.Expense -> if (it.walletFromId == wallet.id) -it.amount else 0.0
                TransactionType.Transfer -> {
                    var balance = 0.0
                    if (it.walletFromId == wallet.id) balance -= it.amount
                    if (it.walletToId == wallet.id) balance += it.amount
                    balance
                }
            }
        }
        walletRepository.updateWallet(wallet.copy(currentBalance = calculatedBalance))
    }
}
