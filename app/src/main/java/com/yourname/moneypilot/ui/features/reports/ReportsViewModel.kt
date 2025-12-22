package com.yourname.moneypilot.ui.features.reports

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.domain.usecase.transaction.CalculateMonthlySummaryUseCase
import com.yourname.moneypilot.domain.usecase.transaction.MonthlySummary
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val calculateMonthlySummaryUseCase: CalculateMonthlySummaryUseCase
) : BaseViewModel<MonthlySummary>() {

    init {
        loadMonthlyReport()
    }

    private fun loadMonthlyReport() {
        val now = LocalDateTime.now()
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            calculateMonthlySummaryUseCase(now.monthValue, now.year).collectLatest { summary ->
                _uiState.value = ScreenState.Success(summary)
            }
        }
    }
}
