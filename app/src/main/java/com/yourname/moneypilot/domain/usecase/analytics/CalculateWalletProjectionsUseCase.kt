package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.domain.model.ForecastResult
import com.yourname.moneypilot.domain.model.WalletProjection
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CalculateWalletProjectionsUseCase @Inject constructor() {

    operator fun invoke(
        wallet: WalletEntity,
        unpaidBills: List<BigBillEntity>,
        avgDailyVelocity: Double,
        projectionDays: Int = 30
    ): ForecastResult {
        val projections = mutableListOf<WalletProjection>()
        var currentRunningBalance = wallet.currentBalance
        val today = LocalDate.now()
        
        var hitZeroDay: LocalDate? = null
        var totalCommitments = 0.0

        for (dayOffset in 1..projectionDays) {
            val date = today.plusDays(dayOffset.toLong())
            
            // 1. Subtract expected daily spend
            currentRunningBalance -= avgDailyVelocity
            
            // 2. Check for bills due on this day
            val billsDueToday = unpaidBills.filter { it.dueDate == date }
            val billNames = billsDueToday.map { it.name }
            val billTotal = billsDueToday.sumOf { it.amount }
            
            currentRunningBalance -= billTotal
            totalCommitments += billTotal
            
            if (currentRunningBalance <= 0 && hitZeroDay == null) {
                hitZeroDay = date
            }

            projections.add(
                WalletProjection(
                    date = date,
                    projectedBalance = currentRunningBalance,
                    pendingBills = billNames,
                    isBelowSafetyLimit = currentRunningBalance < (avgDailyVelocity * 5) // Safety buffer of 5 days spend
                )
            )
        }

        val runway = if (hitZeroDay != null) {
            ChronoUnit.DAYS.between(today, hitZeroDay).toInt()
        } else {
            999 // Representing "infinite" or beyond projection scope
        }

        return ForecastResult(
            dailyProjections = projections,
            projectedEndOfMonthBalance = projections.find { it.date == today.withDayOfMonth(today.lengthOfMonth()) }?.projectedBalance ?: currentRunningBalance,
            estimatedRunwayDays = runway,
            totalUpcomingCommitments = totalCommitments
        )
    }
}
