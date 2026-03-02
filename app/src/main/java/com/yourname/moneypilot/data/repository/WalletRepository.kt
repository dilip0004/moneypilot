package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun getAllWallets(): Flow<List<WalletEntity>>
    suspend fun getWalletById(id: Long): WalletEntity?
    suspend fun insertWallet(wallet: WalletEntity): Long
    suspend fun updateWallet(wallet: WalletEntity)
    suspend fun deleteWallet(wallet: WalletEntity)
    suspend fun updateBalance(walletId: Long, amount: Double)
}
