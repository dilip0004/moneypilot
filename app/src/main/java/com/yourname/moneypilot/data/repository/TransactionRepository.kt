package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double
}
