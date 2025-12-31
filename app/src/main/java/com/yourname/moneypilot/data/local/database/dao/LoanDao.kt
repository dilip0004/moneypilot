package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: Long): LoanEntity?

    @Query("SELECT * FROM loans ORDER BY startDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY startDate DESC")
    fun getLoansByStatus(status: String): Flow<List<LoanEntity>>

    @Query("SELECT SUM(currentBalance) FROM loans WHERE type = 'BORROWED' AND status = 'ACTIVE'")
    fun getTotalBorrowedAmount(): Flow<Double?>

    @Query("SELECT SUM(currentBalance) FROM loans WHERE type = 'LENT' AND status = 'ACTIVE'")
    fun getTotalLentAmount(): Flow<Double?>
}
