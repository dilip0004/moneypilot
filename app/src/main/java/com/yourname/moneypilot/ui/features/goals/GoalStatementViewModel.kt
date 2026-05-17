package com.yourname.moneypilot.ui.features.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalStatementState(
    val goal: GoalEntity? = null,
    val transactions: List<TransactionWithDetails> = emptyList()
)

@HiltViewModel
class GoalStatementViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<GoalStatementState>() {

    private val goalId: Long = savedStateHandle.get<Long>("goalId") ?: -1L

    init {
        loadData()
    }

    private fun loadData() {
        if (goalId == -1L) {
            _uiState.value = ScreenState.Error("Invalid Goal ID")
            return
        }

        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            combine(
                goalRepository.getTransactionsForGoal(goalId),
                // Since getGoalById is not a flow, we'll fetch it inside the collect or use a separate flow if needed
                // For now, let's wrap it in a flow-like behavior or just fetch it once if it doesn't change often
                kotlinx.coroutines.flow.flow { emit(goalRepository.getGoalById(goalId)) }
            ) { txs, goal ->
                if (goal == null) {
                    ScreenState.Error("Goal not found")
                } else {
                    ScreenState.Success(GoalStatementState(goal, txs.sortedByDescending { it.transaction.dateTime }))
                }
            }.collect {
                _uiState.value = it
            }
        }
    }
}
