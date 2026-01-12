package com.yourname.moneypilot.ui.features.accounts

import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.ui.common.ScreenState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { accountRepository.getTotalBalance() } returns flowOf(0.0)
        viewModel = AccountsViewModel(accountRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAccounts calculates total balance correctly`() = runTest {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Bank", type = "BANK", initialBalance = 1000.0, currentBalance = 1200.0, color = 0, icon = ""),
            AccountEntity(id = 2, name = "Cash", type = "CASH", initialBalance = 500.0, currentBalance = 300.0, color = 0, icon = "")
        )
        every { accountRepository.getAllAccounts() } returns flowOf(accounts)
        every { accountRepository.getTotalBalance() } returns flowOf(1500.0)

        viewModel = AccountsViewModel(accountRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScreenState.Success)
        assertEquals(1500.0, (state as ScreenState.Success).data.totalBalance, 0.1)
    }

    @Test
    fun `archiveAccount calls repository update`() = runTest {
        val account = AccountEntity(id = 1, name = "Bank", type = "BANK", initialBalance = 1000.0, currentBalance = 1200.0, color = 0, icon = "")
        
        viewModel.archiveAccount(account)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { accountRepository.updateAccount(any()) }
    }
}
