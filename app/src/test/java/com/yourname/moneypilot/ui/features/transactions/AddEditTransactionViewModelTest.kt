package com.yourname.moneypilot.ui.features.transactions

import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.repository.*
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {

    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val loanRepository = mockk<LoanRepository>(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AddEditTransactionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks for initialization
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(
            AccountEntity(id = 1, name = "Primary", type = "BANK", initialBalance = 1000.0, currentBalance = 1000.0, color = 0, icon = "", isPrimary = true)
        ))
        every { categoryRepository.getCategoriesByType(any()) } returns flowOf(emptyList())
        every { goalRepository.getAllGoals() } returns flowOf(emptyList())
        every { loanRepository.getAllLoans() } returns flowOf(emptyList())

        viewModel = AddEditTransactionViewModel(
            transactionRepository, accountRepository, categoryRepository,
            goalRepository, budgetRepository, loanRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveTransaction inserts expense and deducts wallet balance`() = runTest {
        viewModel.onEvent(AddEditTransactionEvent.EnteredAmount("250.0"))
        viewModel.onEvent(AddEditTransactionEvent.EnteredDescription("Lunch"))
        viewModel.onEvent(AddEditTransactionEvent.TypeChanged("EXPENSE"))
        viewModel.onEvent(AddEditTransactionEvent.AccountChanged(1))

        viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify balance deduction (-250)
        coVerify { accountRepository.updateBalance(1, -250.0) }
        coVerify { transactionRepository.insertTransaction(any()) }
    }

    @Test
    fun `saveTransaction inserts income and increases wallet balance`() = runTest {
        viewModel.onEvent(AddEditTransactionEvent.EnteredAmount("5000.0"))
        viewModel.onEvent(AddEditTransactionEvent.TypeChanged("INCOME"))
        viewModel.onEvent(AddEditTransactionEvent.AccountChanged(1))

        viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify balance addition (+5000)
        coVerify { accountRepository.updateBalance(1, 5000.0) }
    }

    @Test
    fun `saveTransaction with LOAN_REPAYMENT deducts both wallet and loan balance`() = runTest {
        val mockLoan = LoanEntity(id = 10, name = "EMI", lender = "Bank", totalAmount = 10000.0, interestRate = 0.0, startDate = LocalDate.now(), durationMonths = 12, currentBalance = 5000.0, monthlyPayment = 1000.0, type = "BORROWED")
        coEvery { loanRepository.getLoanById(10) } returns mockLoan

        viewModel.onEvent(AddEditTransactionEvent.EnteredAmount("1000.0"))
        viewModel.onEvent(AddEditTransactionEvent.TypeChanged("LOAN_REPAYMENT"))
        viewModel.onEvent(AddEditTransactionEvent.LoanChanged(10))
        viewModel.onEvent(AddEditTransactionEvent.AccountChanged(1))

        viewModel.onEvent(AddEditTransactionEvent.SaveTransaction)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify wallet deduction (-1000)
        coVerify { accountRepository.updateBalance(1, -1000.0) }
        // Verify loan balance reduction (5000 - 1000 = 4000)
        coVerify { loanRepository.updateLoan(match { it.id == 10L && it.currentBalance == 4000.0 }) }
    }
}
