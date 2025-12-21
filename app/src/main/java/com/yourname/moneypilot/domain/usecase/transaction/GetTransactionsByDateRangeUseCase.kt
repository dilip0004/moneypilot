package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

class GetTransactionsByDateRangeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> {
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
    }
}
