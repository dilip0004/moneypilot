package com.yourname.moneypilot.ui.features.goals

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.GoalRepository
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class GoalsState(
    val goals: List<GoalEntity> = emptyList()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
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
            allTxs.filter { it.transaction.goalId == goalId }
        }
    }

    // Contribute to goal
    fun contributeToGoal(goalId: Long, amount: Double) {
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId)
            if (goal == null) return@launch

            // Get first non-archived wallet
            val wallets = walletRepository.getAllWallets().first()
            val wallet = wallets.firstOrNull { !it.isArchived }
            if (wallet == null) return@launch

            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = wallet.id,
                goalId = goalId,
                type = TransactionType.Expense,
                amount = amount,
                note = "Contribution to goal: ${goal.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "GOAL_CONTRIBUTION"
            )
            transactionRepository.insertTransaction(transaction)

            // Refresh goals list
            loadGoals()
        }
    }
}