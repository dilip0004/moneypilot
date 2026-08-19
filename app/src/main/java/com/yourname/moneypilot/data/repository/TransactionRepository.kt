package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TagEntity
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

    suspend fun insertTransaction(transaction: TransactionEntity, tagIds: List<Long> = emptyList())
    suspend fun createTransfer(transaction: TransactionEntity)

    suspend fun updateTransaction(transaction: TransactionEntity, tagIds: List<Long> = emptyList())
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    fun getTagsForTransaction(transactionId: String): Flow<List<TagEntity>>
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun insertTag(tag: TagEntity): Long
    
    fun getTransactionsForInvestment(investmentId: Long): Flow<List<TransactionWithDetails>>
    fun getRecentInvestmentTransactions(limit: Int): Flow<List<TransactionWithDetails>>

    suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double
    suspend fun getSubcategoryExpenseSum(subcategoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double
    suspend fun getTotalSumByType(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): Double?
}
