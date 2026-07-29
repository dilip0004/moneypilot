package com.yourname.moneypilot.ui.features.goals

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class GoalsState(
    val goals: List<GoalEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) : BaseViewModel<GoalsState>() {

    init {
        observeGoalsAndWallets()
    }

    private fun observeGoalsAndWallets() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            combine(
                goalRepository.getAllGoals(),
                walletRepository.getAllWallets()
            ) { goals, wallets ->
                if (goals.isEmpty()) {
                    ScreenState.Empty
                } else {
                    ScreenState.Success(GoalsState(goals, wallets))
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun getTransactionsForGoal(goalId: Long): Flow<List<TransactionWithDetails>> {
        return transactionRepository.getAllTransactionsWithDetails().map { allTxs ->
            allTxs.filter { it.transaction.goalId == goalId }
        }
    }

    fun contributeToGoal(goalId: Long, amount: Double, walletId: Long) {
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId) ?: return@launch

            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = walletId,
                goalId = goalId,
                type = TransactionType.Expense,
                amount = amount,
                note = "Contribution to goal: ${goal.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "GOAL_CONTRIBUTION"
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun withdrawFromGoal(goalId: Long, amount: Double, walletId: Long) {
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId) ?: return@launch
            
            // Note: Withdrawal from goal is modeled as an Income to the target wallet
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = walletId, // Acts as destination wallet in this case
                goalId = goalId,
                type = TransactionType.Income,
                amount = amount,
                note = "Withdrawal from goal: ${goal.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "GOAL_WITHDRAWAL"
            )
            transactionRepository.insertTransaction(transaction)
        }
    }
}
