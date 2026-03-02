package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallets: List<WalletEntity>)

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()

    @Query("SELECT * FROM wallets WHERE id = :walletId")
    suspend fun getWalletById(walletId: Long): WalletEntity?

    @Query("SELECT * FROM wallets WHERE is_archived = 0 ORDER BY name")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWalletsList(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE is_archived = 1 ORDER BY updated_at DESC")
    fun getArchivedWallets(): Flow<List<WalletEntity>>

    @Query("SELECT COUNT(*) FROM wallets WHERE is_archived = 0")
    suspend fun getActiveWalletCount(): Int

    @Query("SELECT SUM(current_balance) FROM wallets WHERE is_archived = 0")
    suspend fun getTotalBalance(): Double?

    @Query("UPDATE wallets SET current_balance = current_balance + :amount WHERE id = :walletId")
    suspend fun updateBalance(walletId: Long, amount: Double)
}
