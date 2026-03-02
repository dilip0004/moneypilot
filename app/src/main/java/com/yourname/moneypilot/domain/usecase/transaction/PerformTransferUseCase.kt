package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.TransactionRepository
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class PerformTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Double,
        note: String?,
        date: LocalDateTime
    ) {
        val transfer = TransactionEntity(
            id = UUID.randomUUID().toString(),
            dateTime = date,
            amount = amount,
            type = TransactionType.Transfer,
            walletFromId = fromWalletId,
            walletToId = toWalletId,
            transactionSourceType = "MANUAL_TRANSFER",
            note = note
        )
        transactionRepository.createTransfer(transfer)
    }
}
