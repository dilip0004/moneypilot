package com.yourname.moneypilot.ui.features.goals

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsState(
    val goals: List<GoalEntity> = emptyList()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel<GoalsState>() {

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            goalRepository.getAllGoals().collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(GoalsState(list))
                }
            }
        }
    }

    fun getTransactionsForGoal(goalId: Long): Flow<List<TransactionWithDetails>> {
        return transactionRepository.getAllTransactionsWithDetails().map { allTxs ->
            // A more direct query would be better, but for now we filter.
            allTxs.filter { it.transaction.note?.contains("Goal: $goalId") == true } 
        }
    }
}
