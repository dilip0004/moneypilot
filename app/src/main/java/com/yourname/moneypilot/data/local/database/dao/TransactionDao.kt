package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,

    @Relation(parentColumn = "wallet_from_id", entityColumn = "id")
    val walletFrom: WalletEntity?,

    @Relation(parentColumn = "wallet_to_id", entityColumn = "id")
    val walletTo: WalletEntity?
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET soft_deleted = 1 WHERE id = :transactionId")
    suspend fun softDeleteById(transactionId: String)

    @Query("SELECT * FROM transactions WHERE id = :transactionId AND soft_deleted = 0")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsForBackup(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE wallet_from_id = :walletId OR wallet_to_id = :walletId")
    suspend fun getTransactionsForWallet(walletId: Long): List<TransactionEntity>

    @Transaction
    @Query("SELECT * FROM transactions WHERE soft_deleted = 0 ORDER BY dateTime DESC")
    fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE dateTime BETWEEN :startDate AND :endDate AND soft_deleted = 0 ORDER BY dateTime DESC")
    fun getTransactionsWithDetailsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionWithDetails>>

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE type = :type
        AND dateTime BETWEEN :startDate AND :endDate
        AND soft_deleted = 0
        """
    )
    suspend fun getTotalSumByType(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE category_id = :categoryId
        AND type = 'Expense'
        AND dateTime BETWEEN :startDate AND :endDate
        AND soft_deleted = 0
        """
    )
    suspend fun getCategoryExpenseSum(
        categoryId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?

    @Query("SELECT COUNT(*) FROM transactions WHERE soft_deleted = 0")
    suspend fun getTransactionCount(): Int
}
