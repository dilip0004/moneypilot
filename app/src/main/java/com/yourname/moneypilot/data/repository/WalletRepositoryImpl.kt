package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.WalletDao
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val walletDao: WalletDao
) : WalletRepository {

    override fun getAllWallets(): Flow<List<WalletEntity>> = walletDao.getAllWallets()

    override suspend fun getWalletById(id: Long): WalletEntity? = walletDao.getWalletById(id)

    override suspend fun insertWallet(wallet: WalletEntity): Long = walletDao.insert(wallet)

    override suspend fun updateWallet(wallet: WalletEntity) = walletDao.update(wallet)

    override suspend fun deleteWallet(wallet: WalletEntity) = walletDao.delete(wallet)

    override suspend fun updateBalance(walletId: Long, amount: Double) = walletDao.updateBalance(walletId, amount)
}
