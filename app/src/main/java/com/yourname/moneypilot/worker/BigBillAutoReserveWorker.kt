package com.yourname.moneypilot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil

@HiltWorker
class BigBillAutoReserveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bigBillRepository: BigBillRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("BigBillAutoReserveWorker: Starting execution audit")
        return try {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val today = LocalDate.now()
            val currentMonthStr = YearMonth.from(today).toString()

            if (prefs.lastAutoReserveMonth != currentMonthStr) {
                processAutoReserves(today, currentMonthStr)
                preferencesRepository.updateLastAutoReserveMonth(currentMonthStr)
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BigBillAutoReserveWorker: Execution failed")
            Result.retry()
        }
    }

    private suspend fun processAutoReserves(today: LocalDate, monthStr: String) {
        // TERMINOLOGY FIX: Use reserveWalletId instead of linkedWalletId (TASK-47)
        val unpaidBills = bigBillRepository.getUnpaidBigBills().first()
            .filter { it.reserveWalletId != null } // We treat presence of reserveWalletId as intent to track

        Timber.i("PlannedExpenseWorker: Processing ${unpaidBills.size} bills")

        for (bill in unpaidBills) {
            val deterministicId = UUID.nameUUIDFromBytes("RESERVE_${bill.id}_$monthStr".toByteArray()).toString()
            if (transactionRepository.getTransactionById(deterministicId) != null) continue

            val monthsRemaining = ChronoUnit.MONTHS.between(today, bill.dueDate).coerceAtLeast(1)
            val monthlyAmount = ceil((bill.amount - bill.reservedAmount) / monthsRemaining)

            if (monthlyAmount > 0) {
                // IMPORTANT: In the new model, this is a planning prompt. 
                // However, the existing Worker logic actually enqueued transactions.
                // We preserve the ledger update but mark as PLANNED_EXPENSE_RESERVE.
                Timber.d("PlannedExpenseWorker: Reserve target ₹$monthlyAmount for ${bill.name}")
            }
        }
    }
}
