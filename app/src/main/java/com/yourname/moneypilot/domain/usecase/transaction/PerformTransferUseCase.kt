package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDateTime
import javax.inject.Inject

class PerformTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        description: String,
        date: LocalDateTime
    ) {
        // Create the transfer transaction record
        val transaction = TransactionEntity(
            accountId = fromAccountId,
            transferToAccountId = toAccountId,
            type = "TRANSFER",
            amount = amount,
            description = description,
            date = date
        )
        transactionRepository.insertTransaction(transaction)

        // Update from account balance
        val fromAccount = accountRepository.getAccountById(fromAccountId)
        fromAccount?.let {
            accountRepository.updateAccount(it.copy(currentBalance = it.currentBalance - amount))
        }

        // Update to account balance
        val toAccount = accountRepository.getAccountById(toAccountId)
        toAccount?.let {
            accountRepository.updateAccount(it.copy(currentBalance = it.currentBalance + amount))
        }
    }
}
