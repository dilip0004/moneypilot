package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity?,
    @Relation(
        parentColumn = "subcategory_id",
        entityColumn = "id"
    )
    val subcategory: SubcategoryEntity?,
    @Relation(
        parentColumn = "account_id",
        entityColumn = "id"
    )
    val account: AccountEntity?
    ,
    @Relation(
        parentColumn = "goal_id",
        entityColumn = "id"
    )
    val goal: GoalEntity?
)

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE
                WHEN type IN ('INCOME','TRANSFER_IN') THEN amount
                WHEN type IN ('EXPENSE','TRANSFER_OUT','LOAN_REPAYMENT','GOAL_CONTRIBUTION') THEN -amount
                ELSE 0
            END
        ), 0)
        FROM transactions
        WHERE account_id = :accountId AND date <= :asOf
        """
    )
    suspend fun getBalanceAt(accountId: Long, asOf: LocalDateTime): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE account_id = :accountId
          AND type IN ('INCOME','TRANSFER_IN')
          AND date BETWEEN :start AND :end
        """
    )
    suspend fun getIncomeInRange(accountId: Long, start: LocalDateTime, end: LocalDateTime): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE account_id = :accountId
          AND type IN ('EXPENSE','TRANSFER_OUT','LOAN_REPAYMENT','GOAL_CONTRIBUTION')
          AND date BETWEEN :start AND :end
        """
    )
    suspend fun getExpenseInRange(accountId: Long, start: LocalDateTime, end: LocalDateTime): Double

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

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, created_at DESC")
    fun getTransactionsWithCategoryByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionWithCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE goal_id = :goalId ORDER BY date DESC, created_at DESC")
    fun getTransactionsWithCategoryByGoal(goalId: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionsByDateRangeOnce(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<TransactionEntity>

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE type = :type
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getTotalSumByType(
        type: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double?

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE category_id = :categoryId
        AND type = 'EXPENSE'
        AND date BETWEEN :startDate AND :endDate
        """
    )
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
