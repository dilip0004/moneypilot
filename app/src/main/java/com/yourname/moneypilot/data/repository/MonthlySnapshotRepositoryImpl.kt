package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.MonthlySnapshotDao
import com.yourname.moneypilot.data.local.database.entities.MonthlyAccountSnapshotEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MonthlySnapshotRepositoryImpl @Inject constructor(
    private val dao: MonthlySnapshotDao
) : MonthlySnapshotRepository {

    override suspend fun upsert(snapshot: MonthlyAccountSnapshotEntity) = dao.upsert(snapshot)

    override suspend fun getLatestSnapshotMonth(): String? = dao.getLatestSnapshotMonth()

    override fun getSnapshotsForAccount(accountId: Long): Flow<List<MonthlyAccountSnapshotEntity>> =
        dao.getSnapshotsForAccount(accountId)
}
