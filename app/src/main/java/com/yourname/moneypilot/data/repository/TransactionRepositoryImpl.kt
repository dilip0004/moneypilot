package com.yourname.moneypilot.data.repository

import androidx.room.Transaction
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.dao.WalletDao
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao // Injected for atomic balance updates
) : TransactionRepository {

    override fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetails()

    override fun getTransactionsWithDetailsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsWithDetailsByDateRange(startDate, endDate)

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun getTransactionsForWallet(walletId: Long): List<TransactionEntity> =
        transactionDao.getTransactionsForWallet(walletId)

    @Transaction
    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
        val balanceChange = if (transaction.type == TransactionType.Income) transaction.amount else -transaction.amount
        transaction.walletFromId?.let { walletDao.updateBalance(it, balanceChange) }
    }

    @Transaction
    override suspend fun createTransfer(transaction: TransactionEntity) {
        require(transaction.type == TransactionType.Transfer) { "Transaction type must be Transfer" }
        require(transaction.walletFromId != null && transaction.walletToId != null) { "Transfer must have from and to wallets" }

        transactionDao.insert(transaction)
        walletDao.updateBalance(transaction.walletFromId!!, -transaction.amount)
        walletDao.updateBalance(transaction.walletToId!!, transaction.amount)
    }

    @Transaction
    override suspend fun updateTransaction(transaction: TransactionEntity) {
        // More complex logic needed here to revert old transaction impact and apply new one
        // For now, just a simple update.
        transactionDao.update(transaction)
    }

    @Transaction
    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        val balanceChange = if (transaction.type == TransactionType.Income) -transaction.amount else transaction.amount
        transaction.walletFromId?.let { walletDao.updateBalance(it, balanceChange) }
        transactionDao.delete(transaction)
    }

    override suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double =
        transactionDao.getCategoryExpenseSum(categoryId, startDate, endDate) ?: 0.0

    override suspend fun getTotalSumByType(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): Double? =
        transactionDao.getTotalSumByType(type, startDate, endDate)
}
