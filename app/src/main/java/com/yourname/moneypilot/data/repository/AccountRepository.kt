package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    suspend fun getAllAccountsOnce(): List<com.yourname.moneypilot.data.local.database.entities.AccountEntity>

    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun updateBalance(accountId: Long, amount: Double)
    suspend fun getTotalBalance(): Double
}
