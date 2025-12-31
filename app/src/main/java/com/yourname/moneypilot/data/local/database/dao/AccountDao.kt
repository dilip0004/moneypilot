package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY name")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE is_archived = 1 ORDER BY updated_at DESC")
    fun getArchivedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts WHERE is_archived = 0")
    suspend fun getActiveAccountCount(): Int

    @Query("SELECT SUM(current_balance) FROM accounts WHERE is_archived = 0")
    suspend fun getTotalBalance(): Double?

    @Query("UPDATE accounts SET current_balance = current_balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)
}
