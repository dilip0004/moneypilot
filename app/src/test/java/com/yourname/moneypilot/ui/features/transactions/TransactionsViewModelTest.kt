package com.yourname.moneypilot.ui.features.transactions

import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.BudgetRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.ScreenState
import io.mockk.coEvery
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Setup initial flow
        every { transactionRepository.getAllTransactionsWithCategory() } returns flowOf(emptyList())
        viewModel = TransactionsViewModel(transactionRepository, accountRepository, budgetRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTransactions sets success state when data exists`() = runTest {
        val transactions = listOf(
            TransactionWithCategory(
                transaction = TransactionEntity(
                    id = 1,
                    accountId = 1,
                    type = "EXPENSE",
                    amount = 100.0,
                    description = "Coffee",
                    date = LocalDateTime.now()
                ),
                category = null,
                account = null
            )
        )
        every { transactionRepository.getAllTransactionsWithCategory() } returns flowOf(transactions)
        
        // Re-init or trigger load
        viewModel = TransactionsViewModel(transactionRepository, accountRepository, budgetRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScreenState.Success)
        assertEquals(1, (state as ScreenState.Success).data.groupedTransactions.size)
    }

    @Test
    fun `deleteTransaction updates account balance`() = runTest {
        val transaction = TransactionEntity(
            id = 1,
            accountId = 1,
            type = "EXPENSE",
            amount = 100.0,
            description = "Coffee",
            date = LocalDateTime.now()
        )

        viewModel.deleteTransaction(transaction)
        testDispatcher.scheduler.advanceUntilIdle()

        // For an expense, deleting it should ADD back the balance
        coVerify { accountRepository.updateBalance(1, 100.0) }
        coVerify { transactionRepository.deleteTransaction(transaction) }
    }

    @Test
    fun `duplicateTransaction inserts new entry and updates balance`() = runTest {
        val transaction = TransactionEntity(
            id = 1,
            accountId = 1,
            type = "INCOME",
            amount = 500.0,
            description = "Bonus",
            date = LocalDateTime.now()
        )

        viewModel.duplicateTransaction(transaction)
        testDispatcher.scheduler.advanceUntilIdle()

        // Duplicating income should increase balance
        coVerify { accountRepository.updateBalance(1, 500.0) }
        coVerify { transactionRepository.insertTransaction(any()) }
    }
}
