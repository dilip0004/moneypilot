package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.BigBillDao
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BigBillRepositoryImpl @Inject constructor(
    private val bigBillDao: BigBillDao
) : BigBillRepository {
    override fun getAllBigBills(): Flow<List<BigBillEntity>> = bigBillDao.getAllBigBills()
    
    override fun getUnpaidBigBills(): Flow<List<BigBillEntity>> = bigBillDao.getUnpaidBigBills()
    
    override suspend fun getBigBillById(id: Long): BigBillEntity? = bigBillDao.getBigBillById(id)
    
    override suspend fun insertBigBill(bigBill: BigBillEntity): Long = bigBillDao.insert(bigBill)
    
    override suspend fun updateBigBill(bigBill: BigBillEntity) = bigBillDao.update(bigBill)
    
    override suspend fun deleteBigBill(bigBill: BigBillEntity) = bigBillDao.delete(bigBill)
}
