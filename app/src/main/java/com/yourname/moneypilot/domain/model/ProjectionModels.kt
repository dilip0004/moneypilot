package com.yourname.moneypilot.domain.model

import java.time.LocalDate

data class WalletProjection(
    val date: LocalDate,
    val projectedBalance: Double,
    val pendingBills: List<String> = emptyList(),
    val isBelowSafetyLimit: Boolean = false
)

data class ForecastResult(
    val dailyProjections: List<WalletProjection>,
    val projectedEndOfMonthBalance: Double,
    val estimatedRunwayDays: Int, // How many days until balance hits zero based on velocity
    val totalUpcomingCommitments: Double
)
