package com.yourname.moneypilot.ui.features.planning

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.repository.*
import com.yourname.moneypilot.domain.usecase.analytics.RunWhatIfSimulationUseCase
import com.yourname.moneypilot.domain.usecase.analytics.SimulationImpact
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import com.yourname.moneypilot.ui.features.reports.CategoryRank
import com.yourname.moneypilot.ui.features.reports.ReportType
import com.yourname.moneypilot.domain.usecase.analytics.GetReportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class WhatIfState(
    val categories: List<CategoryRank> = emptyList(),
    val activeGoals: List<com.yourname.moneypilot.data.local.database.entities.GoalEntity> = emptyList(),
    val categoryReductions: Map<Long, Float> = emptyMap(),
    val simulationResults: List<SimulationImpact> = emptyList(),
    val currentMonthlySurplus: Double = 0.0,
    val totalSimulatedSavings: Double = 0.0
)

@HiltViewModel
class WhatIfSimulationViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val getReportDataUseCase: GetReportDataUseCase,
    private val runWhatIfSimulationUseCase: RunWhatIfSimulationUseCase
) : BaseViewModel<WhatIfState>() {

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            
            val startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay()
            val endOfMonth = LocalDateTime.now()

            combine(
                goalRepository.getGoalsByStatus("ACTIVE"),
                getReportDataUseCase(startOfMonth, endOfMonth, ReportType.EXPENSE)
            ) { goals, reportData ->
                WhatIfState(
                    categories = reportData.categoryBreakdown,
                    activeGoals = goals,
                    currentMonthlySurplus = 1000.0, 
                    simulationResults = emptyList()
                )
            }.collect { state ->
                _uiState.value = ScreenState.Success(state)
                updateSimulation()
            }
        }
    }

    fun onReductionChanged(categoryId: Long, percentage: Float) {
        val currentUiState = uiState.value
        if (currentUiState is ScreenState.Success) {
            val currentState = currentUiState.data
            val newReductions = currentState.categoryReductions.toMutableMap()
            newReductions[categoryId] = percentage
            
            _uiState.value = ScreenState.Success(currentState.copy(categoryReductions = newReductions))
            updateSimulation()
        }
    }

    private fun updateSimulation() {
        val currentUiState = uiState.value
        if (currentUiState is ScreenState.Success) {
            val currentState = currentUiState.data
            
            var addedSavings = 0.0
            currentState.categoryReductions.forEach { (catId, pct) ->
                val cat = currentState.categories.find { it.categoryId == catId }
                if (cat != null) {
                    addedSavings += cat.amount * pct
                }
            }

            val results = runWhatIfSimulationUseCase(
                activeGoals = currentState.activeGoals,
                currentMonthlyContribution = currentState.currentMonthlySurplus,
                monthlySavingsBoost = addedSavings
            )

            _uiState.value = ScreenState.Success(
                currentState.copy(
                    simulationResults = results,
                    totalSimulatedSavings = addedSavings
                )
            )
        }
    }
}
