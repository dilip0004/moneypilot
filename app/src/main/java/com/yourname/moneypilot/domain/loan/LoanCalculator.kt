package com.yourname.moneypilot.domain.loan

import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

data class LoanSnapshot(
    val outstandingPrincipal: Double,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val currentInterestRate: Double,
    val currentEmi: Double,
    val nextDueDate: LocalDate,
    val expectedEndDate: LocalDate,
    val originalEndDate: LocalDate,
    val interestSavedApprox: Double,
    val tenureSavedMonths: Int,
    val monthsRemaining: Int
)

data class MonthlyPoint(
    val date: LocalDate,
    val outstanding: Double,
    val interestPaid: Double,
    val principalPaid: Double,
    val rate: Double
)

/**
 * Advanced Event-Sourced Loan Engine.
 * Calculates current status and future projections by replaying all historical events.
 */
object LoanCalculator {

    fun computeSnapshot(loan: LoanEntity, events: List<LoanEventEntity>, asOf: LocalDate = LocalDate.now()): Pair<LoanSnapshot, List<MonthlyPoint>> {
        val timeline = buildFullTimeline(loan, events, asOf)
        val currentStatus = timeline.lastOrNull { !it.date.isAfter(asOf) } ?: timeline.first()
        
        val remainingBalance = currentStatus.outstanding
        val currentRate = currentStatus.rate
        val currentEmi = loan.monthlyPayment

        // Projection WITH current and future events
        val (totalInterestWithEvents, endDate) = simulateToZero(currentStatus.date, remainingBalance, currentEmi, currentRate, events.filter { it.eventDate.isAfter(asOf) })
        
        // Baseline: No prepayments at all (from start)
        val (totalInterestBaseline, originalEndDate) = simulateToZero(loan.startDate, loan.totalAmount, loan.monthlyPayment, loan.interestRate, emptyList())
        
        // Interest saved calculation
        val interestSaved = max(0.0, totalInterestBaseline - (timeline.filter { !it.date.isAfter(asOf) }.sumOf { it.interestPaid } + totalInterestWithEvents))

        // Tenure saved calculation
        val tenureSaved = ChronoUnit.MONTHS.between(endDate, originalEndDate).toInt().coerceAtLeast(0)

        val snapshot = LoanSnapshot(
            outstandingPrincipal = remainingBalance,
            totalPrincipalPaid = timeline.filter { !it.date.isAfter(asOf) }.sumOf { it.principalPaid },
            totalInterestPaid = timeline.filter { !it.date.isAfter(asOf) }.sumOf { it.interestPaid },
            currentInterestRate = currentRate,
            currentEmi = currentEmi,
            nextDueDate = nextDueDate(loan.startDate, asOf),
            expectedEndDate = endDate,
            originalEndDate = originalEndDate,
            interestSavedApprox = interestSaved,
            tenureSavedMonths = tenureSaved,
            monthsRemaining = ChronoUnit.MONTHS.between(asOf, endDate).toInt().coerceAtLeast(0)
        )

        return snapshot to timeline
    }

    private fun buildFullTimeline(loan: LoanEntity, events: List<LoanEventEntity>, asOf: LocalDate): List<MonthlyPoint> {
        val points = mutableListOf<MonthlyPoint>()
        var cursorDate = loan.startDate
        var balance = loan.totalAmount
        var rate = loan.interestRate
        var emi = loan.monthlyPayment

        val eventsByDate = events.sortedBy { it.eventDate }.groupBy { it.eventDate }

        // Start with initial state
        points.add(MonthlyPoint(cursorDate, balance, 0.0, 0.0, rate))

        // Simulate until balance is zero or 50 years (guard)
        var months = 0
        while (balance > 0.1 && months < 600) {
            cursorDate = cursorDate.plusMonths(1)
            months++

            // Apply events for this month
            eventsByDate[cursorDate]?.forEach { event ->
                when (event.eventType) {
                    "RATE_CHANGE" -> rate = event.newInterestRate ?: rate
                    "PREPAYMENT" -> balance = max(0.0, balance - (event.amount ?: 0.0))
                    "EMI_CHANGE" -> emi = event.newMonthlyPayment ?: emi
                }
            }

            val monthlyRate = rate / 12.0 / 100.0
            val interestThisMonth = balance * monthlyRate
            val principalThisMonth = (emi - interestThisMonth).coerceIn(0.0, balance)
            
            balance -= principalThisMonth
            
            points.add(MonthlyPoint(cursorDate, balance, interestThisMonth, principalThisMonth, rate))
        }

        return points
    }

    private fun simulateToZero(startDate: LocalDate, principal: Double, emi: Double, annualRate: Double, futureEvents: List<LoanEventEntity>): Pair<Double, LocalDate> {
        var balance = principal
        var rate = annualRate
        var currentEmi = emi
        var date = startDate
        var totalInterest = 0.0
        
        val eventsByDate = futureEvents.groupBy { it.eventDate }

        var guard = 0
        while (balance > 0.1 && guard < 600) {
            date = date.plusMonths(1)
            guard++

            eventsByDate[date]?.forEach { event ->
                when (event.eventType) {
                    "RATE_CHANGE" -> rate = event.newInterestRate ?: rate
                    "PREPAYMENT" -> balance = max(0.0, balance - (event.amount ?: 0.0))
                    "EMI_CHANGE" -> currentEmi = event.newMonthlyPayment ?: currentEmi
                }
            }

            val monthlyRate = rate / 12.0 / 100.0
            val interest = balance * monthlyRate
            val principalPaid = (currentEmi - interest).coerceIn(0.0, balance)
            
            totalInterest += interest
            balance -= principalPaid
            
            if (principalPaid <= 0 && balance > 0) break // Stuck loop (EMI < Interest)
        }
        return totalInterest to date
    }

    private fun nextDueDate(start: LocalDate, asOf: LocalDate): LocalDate {
        val day = start.dayOfMonth
        var due = asOf.withDayOfMonth(minOf(day, asOf.lengthOfMonth()))
        if (!due.isAfter(asOf)) due = due.plusMonths(1).withDayOfMonth(minOf(day, due.plusMonths(1).lengthOfMonth()))
        return due
    }
}
