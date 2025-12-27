package com.yourname.moneypilot.ui.features.goals

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AddEditGoalState(
    val name: String = "",
    val targetAmount: String = "",
    val currentAmount: String = "0",
    val targetDate: LocalDate = LocalDate.now().plusMonths(6),
    val priority: Int = 3,
    val color: Int = 0xFF0067FF.toInt(),
    val icon: String = "savings"
)

@HiltViewModel
class AddEditGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _state = mutableStateOf(AddEditGoalState())
    val state: State<AddEditGoalState> = _state

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveGoal : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    fun onEvent(event: AddEditGoalEvent) {
        when (event) {
            is AddEditGoalEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditGoalEvent.EnteredTargetAmount -> _state.value = _state.value.copy(targetAmount = event.value)
            is AddEditGoalEvent.EnteredCurrentAmount -> _state.value = _state.value.copy(currentAmount = event.value)
            is AddEditGoalEvent.PriorityChanged -> _state.value = _state.value.copy(priority = event.value)
            is AddEditGoalEvent.DateChanged -> _state.value = _state.value.copy(targetDate = event.value)
            is AddEditGoalEvent.SaveGoal -> saveGoal()
        }
    }

    private fun saveGoal() {
        viewModelScope.launch {
            try {
                if (_state.value.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("The name of the goal cannot be empty."))
                    return@launch
                }
                val target = _state.value.targetAmount.toDoubleOrNull() ?: 0.0
                if (target <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid target amount."))
                    return@launch
                }

                goalRepository.insertGoal(
                    GoalEntity(
                        name = _state.value.name,
                        description = "",
                        type = "SAVINGS",
                        targetAmount = target,
                        currentAmount = _state.value.currentAmount.toDoubleOrNull() ?: 0.0,
                        targetDate = _state.value.targetDate,
                        priority = _state.value.priority,
                        color = _state.value.color,
                        icon = _state.value.icon
                    )
                )
                _eventFlow.emit(UiEvent.SaveGoal)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save goal"))
            }
        }
    }
}

sealed class AddEditGoalEvent {
    data class EnteredName(val value: String) : AddEditGoalEvent()
    data class EnteredTargetAmount(val value: String) : AddEditGoalEvent()
    data class EnteredCurrentAmount(val value: String) : AddEditGoalEvent()
    data class PriorityChanged(val value: Int) : AddEditGoalEvent()
    data class DateChanged(val value: LocalDate) : AddEditGoalEvent()
    object SaveGoal : AddEditGoalEvent()
}
