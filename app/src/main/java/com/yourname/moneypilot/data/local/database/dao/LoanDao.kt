package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class LoanWithHistory(
    @Embedded val loan: LoanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loan_id"
    )
    val history: List<TransactionEntity>
)

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<LoanEntity>)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: Long): LoanEntity?

    @Transaction
    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun getLoanWithHistory(loanId: Long): Flow<LoanWithHistory?>

    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansOnce(): List<LoanEntity>

    @Transaction
    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoansWithHistory(): Flow<List<LoanWithHistory>>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansList(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY startDate DESC")
    fun getLoansByStatus(status: String): Flow<List<LoanEntity>>

    @Query("SELECT SUM(currentBalance) FROM loans WHERE type = 'BORROWED' AND status = 'ACTIVE'")
    fun getTotalBorrowedAmount(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM loans WHERE type = 'LENT' AND status = 'ACTIVE'")
    fun getTotalLentAmount(): Flow<Double?>
}
