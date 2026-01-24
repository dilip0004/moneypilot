package com.yourname.moneypilot.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Update
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LoanEventEntity): Long

    @Update
    suspend fun update(event: LoanEventEntity)

    @Delete
    suspend fun delete(event: LoanEventEntity)

    @Query("SELECT * FROM loan_events WHERE loan_id = :loanId ORDER BY event_date ASC")
    fun getEventsForLoan(loanId: Long): Flow<List<LoanEventEntity>>

    @Query("SELECT * FROM loan_events WHERE loan_id = :loanId ORDER BY event_date ASC")
    suspend fun getEventsForLoanOnce(loanId: Long): List<LoanEventEntity>
}
