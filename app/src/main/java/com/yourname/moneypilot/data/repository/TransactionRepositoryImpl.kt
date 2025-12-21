package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = 
        transactionDao.getAllTransactions()

    override fun getTransactionsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    override fun insertTransaction(transaction: TransactionEntity): Long = 
        transactionDao.insert(transaction)

    override fun deleteTransaction(transaction: TransactionEntity) = 
        transactionDao.delete(transaction)
}
