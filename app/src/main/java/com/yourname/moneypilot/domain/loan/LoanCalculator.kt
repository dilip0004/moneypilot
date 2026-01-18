package com.yourname.moneypilot.domain.loan

import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import java.time.LocalDate
import kotlin.math.max

data class LoanSnapshot(
    val outstandingPrincipal: Double,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val currentInterestRate: Double,
    val emiAmount: Double,
    val nextDueDate: LocalDate,
    val expectedEndDate: LocalDate,
    val interestSavedApprox: Double
)

data class MonthlyPoint(
    val month: LocalDate,
    val outstanding: Double,
    val interestPaid: Double,
    val principalPaid: Double
)

/**
 * Borrower-style loan computation engine.
 *
 * Assumptions:
 * - Monthly compounding.
 * - EMI paid on the same day-of-month as startDate (or nearest valid day).
 * - Prepayment reduces principal immediately.
 * - ROI changes apply forward from effective date.
 */
object LoanCalculator {

    fun computeSnapshot(loan: LoanEntity, events: List<LoanEventEntity>, asOf: LocalDate = LocalDate.now()): Pair<LoanSnapshot, List<MonthlyPoint>> {
        val timeline = buildTimeline(loan, events, asOf)
        val last = timeline.last()

        // Estimate end date using remaining balance & current EMI
        val remaining = last.outstanding
        val emi = max(loan.monthlyPayment, 0.0)
        val rate = last.rateAnnual

        val endDate = estimateEndDate(asOf, remaining, emi, rate)

        val (interestWith, _) = simulateTotalInterestToEnd(loan, events, includePrepayment = true)
        val (interestWithout, _) = simulateTotalInterestToEnd(loan, events.filter { it.eventType != "PREPAYMENT" }, includePrepayment = false)
        val interestSaved = max(0.0, interestWithout - interestWith)

        val snapshot = LoanSnapshot(
            outstandingPrincipal = remaining,
            totalPrincipalPaid = timeline.sumOf { it.principalPaid },
            totalInterestPaid = timeline.sumOf { it.interestPaid },
            currentInterestRate = rate,
            emiAmount = emi,
            nextDueDate = nextDueDate(loan.startDate, asOf),
            expectedEndDate = endDate,
            interestSavedApprox = interestSaved
        )

        val points = timeline.map { MonthlyPoint(it.date, it.outstanding, it.interestPaid, it.principalPaid) }
        return snapshot to points
    }

    private data class StateRow(
        val date: LocalDate,
        val outstanding: Double,
        val rateAnnual: Double,
        val interestPaid: Double,
        val principalPaid: Double
    )

    private fun buildTimeline(loan: LoanEntity, events: List<LoanEventEntity>, asOf: LocalDate): List<StateRow> {
        val start = loan.startDate
        val monthly = loan.monthlyPayment
        var outstanding = loan.totalAmount
        var currentRate = loan.interestRate

        val byDate = events.groupBy { it.eventDate }

        val rows = mutableListOf<StateRow>()
        var d = start
        // simulate month boundaries from start to asOf (inclusive)
        while (!d.isAfter(asOf)) {
            // apply rate changes/prepayments on this date
            byDate[d]?.forEach { ev ->
                when (ev.eventType) {
                    "RATE_CHANGE" -> currentRate = ev.newInterestRate ?: currentRate
                    "PREPAYMENT" -> outstanding = max(0.0, outstanding - (ev.amount ?: 0.0))
                    "EMI_CHANGE" -> { /* current model stores EMI in loan entity; ignore for now */ }
                    "TENURE_CHANGE" -> { /* ignore; end date is computed */ }
                }
            }

            // apply EMI (if due this month and not beyond asOf)
            val due = dueDateForMonth(start, d)
            if (!due.isAfter(asOf)) {
                val monthlyRate = currentRate / 12.0 / 100.0
                val interest = outstanding * monthlyRate
                val pay = max(0.0, monthly)
                val principal = max(0.0, pay - interest)
                outstanding = max(0.0, outstanding - principal)

                rows.add(StateRow(due, outstanding, currentRate, interest, principal))
            } else {
                rows.add(StateRow(d, outstanding, currentRate, 0.0, 0.0))
            }

            d = d.plusMonths(1)
        }
        return rows
    }

    private fun dueDateForMonth(start: LocalDate, monthCursor: LocalDate): LocalDate {
        val day = start.dayOfMonth
        val targetMonth = LocalDate.of(monthCursor.year, monthCursor.month, 1)
        val lastDay = targetMonth.lengthOfMonth()
        return targetMonth.withDayOfMonth(minOf(day, lastDay))
    }

    private fun nextDueDate(start: LocalDate, asOf: LocalDate): LocalDate {
        var d = dueDateForMonth(start, asOf)
        if (!d.isAfter(asOf)) d = dueDateForMonth(start, asOf.plusMonths(1))
        return d
    }


    private fun simulateTotalInterestToEnd(loan: LoanEntity, events: List<LoanEventEntity>, includePrepayment: Boolean): Pair<Double, LocalDate> {
        var outstanding = loan.totalAmount
        var currentRate = loan.interestRate
        val emi = max(loan.monthlyPayment, 0.0)
        val byDate = events.groupBy { it.eventDate }
        var date = loan.startDate
        var totalInterest = 0.0
        var guard = 0
        while (outstanding > 0.01 && guard < 2000) {
            byDate[date]?.forEach { ev ->
                when (ev.eventType) {
                    "RATE_CHANGE" -> currentRate = ev.newInterestRate ?: currentRate
                    "PREPAYMENT" -> if (includePrepayment) outstanding = max(0.0, outstanding - (ev.amount ?: 0.0))
                }
            }
            val monthlyRate = currentRate / 12.0 / 100.0
            val interest = outstanding * monthlyRate
            totalInterest += interest
            val principal = max(0.0, emi - interest)
            if (principal <= 0.0) break
            outstanding = max(0.0, outstanding - principal)
            date = date.plusMonths(1)
            guard++
        }
        return totalInterest to date
    }

    private fun estimateEndDate(asOf: LocalDate, principal: Double, emi: Double, rateAnnual: Double): LocalDate {
        if (emi <= 0.0 || principal <= 0.0) return asOf
        val monthlyRate = rateAnnual / 12.0 / 100.0
        var balance = principal
        var months = 0
        while (balance > 0.01 && months < 1000) {
            val interest = balance * monthlyRate
            val p = max(0.0, emi - interest)
            if (p <= 0.0) break
            balance -= p
            months++
        }
        return asOf.plusMonths(months.toLong())
    }
}
