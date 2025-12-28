package com.yourname.moneypilot.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC, created_at DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC, created_at DESC
    """)
    fun getTransactionsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    // One-shot query for Worker
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTransactionsByDateRangeOnce(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE account_id = :accountId 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getAccountTransactionsByDateRange(
        accountId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = :type
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalSumByType(
        type: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE account_id = :accountId 
        AND type = :type
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTransactionSum(
        accountId: Long,
        type: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE category_id = :categoryId 
        AND type = 'EXPENSE'
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getCategoryExpenseSum(
        categoryId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions WHERE is_recurring = 1")
    fun getRecurringTransactions(): Flow<List<TransactionEntity>>
}
