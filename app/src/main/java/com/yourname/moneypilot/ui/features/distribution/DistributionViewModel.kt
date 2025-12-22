package com.yourname.moneypilot.ui.features.distribution

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.DistributionRepository
import com.yourname.moneypilot.domain.usecase.distribution.DistributionAction
import com.yourname.moneypilot.domain.usecase.distribution.GenerateLeftoverPlanUseCase
import com.yourname.moneypilot.domain.usecase.transaction.PerformTransferUseCase
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class DistributionState(
    val rules: List<DistributionRuleEntity> = emptyList(),
    val plan: List<DistributionAction> = emptyList(),
    val totalSurplus: Double = 0.0
)

@HiltViewModel
class DistributionViewModel @Inject constructor(
    private val distributionRepository: DistributionRepository,
    private val accountRepository: AccountRepository,
    private val generateLeftoverPlanUseCase: GenerateLeftoverPlanUseCase,
    private val performTransferUseCase: PerformTransferUseCase
) : BaseViewModel<DistributionState>() {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    sealed class UiEvent {
        object PlanExecuted : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            distributionRepository.getAllRules().collectLatest { rules ->
                val now = LocalDateTime.now()
                val plan = generateLeftoverPlanUseCase(now.monthValue, now.year)
                val surplus = plan.sumOf { it.amount }
                _uiState.value = ScreenState.Success(DistributionState(rules, plan, surplus))
            }
        }
    }

    fun addRule(rule: DistributionRuleEntity) {
        viewModelScope.launch {
            distributionRepository.insertRule(rule)
        }
    }

    fun deleteRule(rule: DistributionRuleEntity) {
        viewModelScope.launch {
            distributionRepository.deleteRule(rule)
        }
    }

    fun executePlan() {
        viewModelScope.launch {
            val currentState = (uiState.value as? ScreenState.Success)?.data ?: return@launch
            try {
                currentState.plan.forEach { action ->
                    performTransferUseCase(
                        fromAccountId = action.sourceWalletId,
                        toAccountId = action.targetWalletId,
                        amount = action.amount,
                        description = "Monthly Distribution",
                        date = LocalDateTime.now()
                    )
                }
                _eventFlow.emit(UiEvent.PlanExecuted)
                _eventFlow.emit(UiEvent.ShowSnackbar("Transfer plan executed successfully!"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Failed to execute plan: ${e.message}"))
            }
        }
    }
}
