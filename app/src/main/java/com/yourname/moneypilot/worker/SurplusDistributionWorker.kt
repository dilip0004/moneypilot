package com.yourname.moneypilot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.domain.ExecuteDistributionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth

@HiltWorker
class SurplusDistributionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val executeDistributionUseCase: ExecuteDistributionUseCase,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SurplusDistributionWorker: Starting execution audit")
        return try {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val today = LocalDate.now()
            
            // We logically distribute the surplus of the PREVIOUS month.
            // currentLogicalMonth is the month we ARE currently in.
            // If lastDistributionMonth != currentLogicalMonth, it means we haven't distributed last month's surplus yet.
            val currentMonthStr = YearMonth.from(today).toString() // YYYY-MM

            if (prefs.lastDistributionMonth != currentMonthStr) {
                val lastMonth = YearMonth.from(today.minusMonths(1))
                Timber.i("SurplusDistributionWorker: Executing distribution for $lastMonth")
                executeDistributionUseCase(lastMonth)
                preferencesRepository.updateLastDistributionMonth(currentMonthStr)
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SurplusDistributionWorker: Failed")
            Result.retry()
        }
    }
}
