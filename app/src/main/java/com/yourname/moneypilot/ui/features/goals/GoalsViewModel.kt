package com.yourname.moneypilot.ui.features.goals

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsState(
    val goals: List<GoalEntity> = emptyList()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
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
}
