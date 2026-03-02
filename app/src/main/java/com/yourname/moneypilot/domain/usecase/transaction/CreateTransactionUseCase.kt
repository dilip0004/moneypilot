package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

class CreateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity) {
        transactionRepository.insertTransaction(transaction)
    }
}
