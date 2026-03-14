package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TransactionRepository {
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>
    fun getTransactionsWithDetailsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionWithDetails>>
    suspend fun getTransactionById(id: String): TransactionEntity?
    suspend fun getTransactionsForWallet(walletId: Long): List<TransactionEntity>
    suspend fun getTransactionCountForWallet(walletId: Long): Int
    
    fun getTransactionsWithDetailsForWallet(
        walletId: Long, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): Flow<List<TransactionWithDetails>>
    
    suspend fun getSumBeforeDate(walletId: Long, startDate: LocalDateTime): Double

    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun createTransfer(transaction: TransactionEntity)

    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double
    suspend fun getSubcategoryExpenseSum(subcategoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double
    suspend fun getTotalSumByType(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): Double?
}
