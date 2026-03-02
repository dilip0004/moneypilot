package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BigBillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bigBill: BigBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bigBills: List<BigBillEntity>)

    @Update
    suspend fun update(bigBill: BigBillEntity)

    @Delete
    suspend fun delete(bigBill: BigBillEntity)

    @Query("DELETE FROM big_bills")
    suspend fun deleteAll()

    @Query("SELECT * FROM big_bills ORDER BY dueDate ASC")
    fun getAllBigBills(): Flow<List<BigBillEntity>>

    @Query("SELECT * FROM big_bills")
    suspend fun getAllBigBillsList(): List<BigBillEntity>

    @Query("SELECT * FROM big_bills WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidBigBills(): Flow<List<BigBillEntity>>

    @Query("SELECT * FROM big_bills WHERE id = :id")
    suspend fun getBigBillById(id: Long): BigBillEntity?
}
