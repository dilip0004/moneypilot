package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.AccountDao
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override suspend fun getAccountById(id: Long): AccountEntity? = accountDao.getAccountById(id)

    override suspend fun insertAccount(account: AccountEntity): Long = accountDao.insert(account)

    override suspend fun updateAccount(account: AccountEntity) = accountDao.update(account)

    override suspend fun deleteAccount(account: AccountEntity) = accountDao.delete(account)

    override suspend fun updateBalance(accountId: Long, amount: Double) = accountDao.updateBalance(accountId, amount)

    override suspend fun getTotalBalance(): Double = accountDao.getTotalBalance() ?: 0.0
}
