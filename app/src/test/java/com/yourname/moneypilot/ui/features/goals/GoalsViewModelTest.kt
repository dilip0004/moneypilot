package com.yourname.moneypilot.ui.features.goals

import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.repository.GoalRepository
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
class GoalsViewModelTest {

    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: GoalsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { goalRepository.getAllGoals() } returns flowOf(emptyList())
        viewModel = GoalsViewModel(goalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadGoals sets success state with correct count`() = runTest {
        val goals = listOf(
            GoalEntity(id = 1, name = "Car", targetAmount = 500000.0, currentAmount = 50000.0, targetDate = LocalDate.now(), priority = 1, color = 0, icon = ""),
            GoalEntity(id = 2, name = "Vacation", targetAmount = 50000.0, currentAmount = 10000.0, targetDate = LocalDate.now(), priority = 2, color = 0, icon = "")
        )
        every { goalRepository.getAllGoals() } returns flowOf(goals)

        viewModel = GoalsViewModel(goalRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScreenState.Success)
        assertEquals(2, (state as ScreenState.Success).data.goals.size)
    }
}
