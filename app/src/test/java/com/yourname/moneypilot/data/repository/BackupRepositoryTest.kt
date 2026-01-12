package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryTest {

    private val database = mockk<MoneyPilotDatabase>(relaxed = true)
    private lateinit var repository: BackupRepository

    @Before
    fun setup() {
        // Context is only needed for URI reading, which we mock in restore tests
        repository = BackupRepository(mockk(relaxed = true), database)
    }

    @Test
    fun `createJsonBackup generates valid non-empty string`() = runTest {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Bank", type = "BANK", initialBalance = 1000.0, currentBalance = 1200.0, color = 0, icon = "")
        )
        coEvery { database.accountDao().getAllAccountsList() } returns accounts
        coEvery { database.categoryDao().getAllCategoriesList() } returns emptyList()
        coEvery { database.transactionDao().getAllTransactionsList() } returns emptyList()
        // ... mock other DAOs

        val json = repository.createJsonBackup()
        
        assertTrue(json.contains("accounts"))
        assertTrue(json.contains("Bank"))
        assertTrue(json.contains("\"version\": 7"))
    }
}
