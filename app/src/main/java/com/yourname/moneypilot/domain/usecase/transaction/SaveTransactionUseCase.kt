package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * Encapsulates the logic for saving or updating a transaction.
 * Adheres to Section 2.1 (Flow Layer) of the v5 architecture.
 */
class SaveTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity, isEdit: Boolean) {
        if (isEdit) {
            transactionRepository.updateTransaction(transaction)
        } else {
            transactionRepository.insertTransaction(transaction)
        }
    }
}
