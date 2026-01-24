package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.MonthlyAccountSnapshotEntity
import kotlinx.coroutines.flow.Flow

interface MonthlySnapshotRepository {
    suspend fun upsert(snapshot: MonthlyAccountSnapshotEntity): Long
    suspend fun getLatestSnapshotMonth(): String?
    fun getSnapshotsForAccount(accountId: Long): Flow<List<MonthlyAccountSnapshotEntity>>
}
