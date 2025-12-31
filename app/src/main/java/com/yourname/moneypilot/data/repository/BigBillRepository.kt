package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import kotlinx.coroutines.flow.Flow

interface BigBillRepository {
    fun getAllBigBills(): Flow<List<BigBillEntity>>
    fun getUnpaidBigBills(): Flow<List<BigBillEntity>>
    suspend fun getBigBillById(id: Long): BigBillEntity?
    suspend fun insertBigBill(bigBill: BigBillEntity): Long
    suspend fun updateBigBill(bigBill: BigBillEntity)
    suspend fun deleteBigBill(bigBill: BigBillEntity)
}
