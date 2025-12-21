package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun getTotalBalance(): Double
}
