package com.yourname.moneypilot.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourname.moneypilot.data.local.database.entities.MonthlyAccountSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: MonthlyAccountSnapshotEntity): Long

    @Query("SELECT * FROM monthly_account_snapshots WHERE account_id = :accountId ORDER BY month DESC")
    fun getSnapshotsForAccount(accountId: Long): Flow<List<MonthlyAccountSnapshotEntity>>

    @Query("SELECT * FROM monthly_account_snapshots WHERE month = :month")
    suspend fun getSnapshotsForMonth(month: String): List<MonthlyAccountSnapshotEntity>

    @Query("SELECT month FROM monthly_account_snapshots ORDER BY month DESC LIMIT 1")
    suspend fun getLatestSnapshotMonth(): String?
}
