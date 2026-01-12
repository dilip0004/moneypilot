package com.yourname.moneypilot.ui.features.loans

import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.repository.LoanRepository
import com.yourname.moneypilot.ui.common.ScreenState
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LoansViewModelTest {

    private val loanRepository = mockk<LoanRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoansViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { loanRepository.getAllLoans() } returns flowOf(emptyList())
        every { loanRepository.getTotalBorrowedAmount() } returns flowOf(0.0)
        every { loanRepository.getTotalLentAmount() } returns flowOf(0.0)
        viewModel = LoansViewModel(loanRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadLoans correctly identifies borrowed and lent totals`() = runTest {
        val loans = listOf(
            LoanEntity(id = 1, name = "Car", lender = "Bank", totalAmount = 10000.0, currentBalance = 8000.0, monthlyPayment = 500.0, type = "BORROWED", startDate = LocalDate.now(), interestRate = 0.0, durationMonths = 12, accountId = null),
            LoanEntity(id = 2, name = "Friend", lender = "Me", totalAmount = 1000.0, currentBalance = 1000.0, monthlyPayment = 100.0, type = "LENT", startDate = LocalDate.now(), interestRate = 0.0, durationMonths = 12, accountId = null)
        )
        every { loanRepository.getAllLoans() } returns flowOf(loans)
        every { loanRepository.getTotalBorrowedAmount() } returns flowOf(8000.0)
        every { loanRepository.getTotalLentAmount() } returns flowOf(1000.0)

        viewModel = LoansViewModel(loanRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScreenState.Success)
        val data = (state as ScreenState.Success).data
        assertEquals(8000.0, data.totalBorrowed, 0.1)
        assertEquals(1000.0, data.totalLent, 0.1)
    }
}
