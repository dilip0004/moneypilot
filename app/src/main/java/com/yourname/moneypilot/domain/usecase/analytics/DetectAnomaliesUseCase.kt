package com.yourname.moneypilot.domain.usecase.analytics

import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import javax.inject.Inject

data class Anomaly(
    val transaction: TransactionWithDetails,
    val reason: String
)

class DetectAnomaliesUseCase @Inject constructor() {

    operator fun invoke(transactions: List<TransactionWithDetails>): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        val nonTransfers = transactions.filter { it.transaction.type == TransactionType.Expense }
        
        if (nonTransfers.isEmpty()) return emptyList()

        // Group by category to find category-specific spikes
        val byCategory = nonTransfers.groupBy { it.transaction.categoryId }

        byCategory.forEach { (catId, txs) ->
            if (txs.size >= 3) { // Need a baseline
                val amounts = txs.map { it.transaction.amount }
                val avg = amounts.average()
                
                txs.forEach { tx ->
                    // Spike Detection: Transaction is > 3x the average of its category
                    if (tx.transaction.amount > avg * 3 && tx.transaction.amount > 500) {
                        anomalies.add(
                            Anomaly(
                                transaction = tx,
                                reason = "Unusually high spending for ${tx.category?.name ?: "this category"}."
                            )
                        )
                    }
                }
            }
        }

        // Daily Spike Detection: Total daily spending > 2x the period's daily average
        val byDate = nonTransfers.groupBy { it.transaction.dateTime.toLocalDate() }
        val dailyTotals = byDate.mapValues { it.value.sumOf { t -> t.transaction.amount } }
        val periodAvg = dailyTotals.values.average()

        dailyTotals.forEach { (date, total) ->
            if (total > periodAvg * 2.5 && total > 1000) {
                val spikeTx = byDate[date]?.maxByOrNull { it.transaction.amount }
                if (spikeTx != null && anomalies.none { it.transaction.transaction.id == spikeTx.transaction.id }) {
                    anomalies.add(
                        Anomaly(
                            transaction = spikeTx,
                            reason = "Spending spike detected on this day."
                        )
                    )
                }
            }
        }

        return anomalies.sortedByDescending { it.transaction.transaction.amount }
    }
}
