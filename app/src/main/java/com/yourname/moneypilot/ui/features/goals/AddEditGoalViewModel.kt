package com.yourname.moneypilot.ui.features.goals

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class AddEditGoalState(
    val name: String = "",
    val targetAmount: String = "",
    val currentAmount: String = "0",
    val targetDate: LocalDate = LocalDate.now().plusMonths(6),
    val priority: Int = 3,
    val color: Int = 0xFF0067FF.toInt(),
    val icon: String = "💰",  // default emoji (only one declaration)
    val linkedWalletId: Long? = null,
    val wallets: List<WalletEntity> = emptyList(),
    val isEditMode: Boolean = false
)

@HiltViewModel
class AddEditGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = mutableStateOf(AddEditGoalState())
    val state: State<AddEditGoalState> = _state

    private var currentGoalId: Long? = null

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object SaveGoal : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadWallets()
        checkEditMode()
    }

    private fun loadWallets() {
        walletRepository.getAllWallets().onEach { wallets ->
            _state.value = _state.value.copy(
                wallets = wallets,
                linkedWalletId = _state.value.linkedWalletId ?: wallets.find { it.isPrimary }?.id ?: wallets.firstOrNull()?.id
            )
        }.launchIn(viewModelScope)
    }

    private fun checkEditMode() {
        savedStateHandle.get<Long>("goalId")?.let { goalId ->
            if (goalId != -1L) {
                viewModelScope.launch {
                    goalRepository.getGoalById(goalId)?.also { goal ->
                        currentGoalId = goal.id
                        _state.value = _state.value.copy(
                            name = goal.name,
                            targetAmount = goal.targetAmount.toString(),
                            currentAmount = goal.currentAmount.toString(),
                            targetDate = goal.targetDate,
                            priority = goal.priority,
                            color = goal.color,
                            icon = goal.icon,
                            isEditMode = true
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AddEditGoalEvent) {
        when (event) {
            is AddEditGoalEvent.EnteredName -> _state.value = _state.value.copy(name = event.value)
            is AddEditGoalEvent.EnteredTargetAmount -> _state.value = _state.value.copy(targetAmount = event.value)
            is AddEditGoalEvent.EnteredCurrentAmount -> _state.value = _state.value.copy(currentAmount = event.value)
            is AddEditGoalEvent.PriorityChanged -> _state.value = _state.value.copy(priority = event.value)
            is AddEditGoalEvent.DateChanged -> _state.value = _state.value.copy(targetDate = event.value)
            is AddEditGoalEvent.WalletChanged -> _state.value = _state.value.copy(linkedWalletId = event.value)
            is AddEditGoalEvent.SaveGoal -> saveGoal()
            is AddEditGoalEvent.IconChanged -> _state.value = _state.value.copy(icon = event.value)
        }
    }

    private fun saveGoal() {
        viewModelScope.launch {
            try {
                val s = _state.value
                if (s.name.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("The name of the goal cannot be empty."))
                    return@launch
                }
                val target = s.targetAmount.toDoubleOrNull() ?: 0.0
                if (target <= 0) {
                    _eventFlow.emit(UiEvent.ShowSnackbar("Please enter a valid target amount."))
                    return@launch
                }

                // 1. Initial Position: Save Goal Metadata
                // currentAmount is 0.0 initially; it MUST be driven by a transaction
                val goal = GoalEntity(
                    id = currentGoalId ?: 0L,
                    name = s.name,
                    description = "",
                    type = "SAVINGS",
                    targetAmount = target,
                    currentAmount = if (s.isEditMode) goalRepository.getGoalById(currentGoalId!!)?.currentAmount ?: 0.0 else 0.0,
                    targetDate = s.targetDate,
                    priority = s.priority,
                    color = s.color,
                    icon = s.icon
                )

                val id = goalRepository.insertGoal(goal)

                // 2. Ledger Integrity: Create Initial Contribution Transaction if any
                val initialAmount = s.currentAmount.toDoubleOrNull() ?: 0.0
                if (!s.isEditMode && initialAmount > 0 && s.linkedWalletId != null) {
                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            walletFromId = s.linkedWalletId,
                            goalId = id,
                            type = TransactionType.Expense, // Contribution is an outflow from wallet
                            amount = initialAmount,
                            note = "Initial contribution to goal: ${s.name}",
                            dateTime = LocalDateTime.now(),
                            transactionSourceType = "GOAL_INITIAL_CONTRIBUTION"
                        )
                    )
                }

                _eventFlow.emit(UiEvent.SaveGoal)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Could not save goal: ${e.message}"))
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
    data class WalletChanged(val value: Long) : AddEditGoalEvent()
    data class IconChanged(val value: String) : AddEditGoalEvent()
    object SaveGoal : AddEditGoalEvent()
}
