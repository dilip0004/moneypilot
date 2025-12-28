package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = 
        transactionDao.getAllTransactionsWithCategory().map { list -> 
            list.map { it.transaction } 
        }

    override fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>> =
        transactionDao.getAllTransactionsWithCategory()

    override fun getTransactionsWithCategoryByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionWithCategory>> =
        transactionDao.getTransactionsWithCategoryByDateRange(startDate, endDate)

    override fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsWithCategoryByDateRange(startDate, endDate).map { list -> 
            list.map { it.transaction } 
        }

    override suspend fun getTransactionById(id: Long): TransactionEntity? = 
        transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long = 
        transactionDao.insert(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) = 
        transactionDao.update(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) = 
        transactionDao.delete(transaction)

    override suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double =
        transactionDao.getCategoryExpenseSum(categoryId, startDate, endDate) ?: 0.0

    override suspend fun getTotalSumByType(type: String, startDate: LocalDateTime, endDate: LocalDateTime): Double =
        transactionDao.getTotalSumByType(type, startDate, endDate) ?: 0.0
}
