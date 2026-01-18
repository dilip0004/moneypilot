package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>
    fun getTransactionsWithCategoryByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionWithCategory>>
    fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun getAccountBalanceAt(accountId: Long, asOf: java.time.LocalDateTime): Double
    suspend fun getAccountIncomeInRange(accountId: Long, start: java.time.LocalDateTime, end: java.time.LocalDateTime): Double
    suspend fun getAccountExpenseInRange(accountId: Long, start: java.time.LocalDateTime, end: java.time.LocalDateTime): Double

    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double
    suspend fun getTotalSumByType(type: String, startDate: LocalDateTime, endDate: LocalDateTime): Double
}
