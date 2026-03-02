package com.yourname.moneypilot.domain.usecase.transaction

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class GetTransactionsByDateRangeUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> {
        return transactionRepository.getTransactionsWithDetailsByDateRange(startDate, endDate).map { list ->
            list.map { it.transaction }
        }
    }
}
