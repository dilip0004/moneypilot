package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.features.reports.WeatherState
import java.time.LocalDate
import javax.inject.Inject

class CalculateFinancialWeatherUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionWithDetails>,
        start: LocalDate,
        end: LocalDate
    ): Pair<Map<WeatherState, Int>, String> {
        val dailyNet = transactions.groupBy { it.transaction.dateTime.toLocalDate() }
            .mapValues { entry ->
                entry.value.sumOf { 
                    when (it.transaction.type) {
                        TransactionType.Income -> it.transaction.amount
                        else -> -it.transaction.amount 
                    }
                }
            }

        val summary = mutableMapOf<WeatherState, Int>()
        var currentDate = start
        while (!currentDate.isAfter(end)) {
            val net = dailyNet[currentDate] ?: 0.0
            val state = when {
                net > 100 -> WeatherState.SUNNY
                net < -1000 -> WeatherState.STORMY
                net < 0 -> WeatherState.RAINY
                else -> WeatherState.CLOUDY
            }
            summary[state] = summary.getOrDefault(state, 0) + 1
            currentDate = currentDate.plusDays(1)
        }

        val sunnyDays = summary.getOrDefault(WeatherState.SUNNY, 0)
        val rainyDays = summary.getOrDefault(WeatherState.RAINY, 0) + summary.getOrDefault(WeatherState.STORMY, 0)
        
        val insight = when {
            sunnyDays > rainyDays -> "Predicting a clear horizon: Inflows are outpacing outflows."
            rainyDays > sunnyDays -> "Caution: High frequency of outflows detected."
            else -> "A balanced financial climate."
        }

        return Pair(summary, insight)
    }
}
