package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.LoanDao
import com.yourname.moneypilot.data.local.database.dao.LoanEventDao
import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LoanRepositoryImpl @Inject constructor(
    private val loanDao: LoanDao,
    private val loanEventDao: LoanEventDao
) : LoanRepository {

    override fun getLoanEvents(loanId: Long) =
        loanEventDao.getEventsForLoan(loanId)

    override suspend fun getLoanEventsOnce(loanId: Long) =
        loanEventDao.getEventsForLoanOnce(loanId)

    override suspend fun getAllLoansOnce() =
        loanDao.getAllLoansOnce()

    override suspend fun addLoanEvent(event: LoanEventEntity) =
        loanEventDao.insert(event)

    override suspend fun updateLoanEvent(event: LoanEventEntity) =
        loanEventDao.update(event)

    override suspend fun deleteLoanEvent(event: LoanEventEntity) =
        loanEventDao.delete(event)

    override fun getAllLoans(): Flow<List<LoanEntity>> =
        loanDao.getAllLoans()

    override fun getLoansByStatus(status: String): Flow<List<LoanEntity>> =
        loanDao.getLoansByStatus(status)

    override suspend fun getLoanById(id: Long): LoanEntity? =
        loanDao.getLoanById(id)

    override suspend fun insertLoan(loan: LoanEntity): Long =
        loanDao.insertLoan(loan)

    override suspend fun updateLoan(loan: LoanEntity) =
        loanDao.updateLoan(loan)

    override suspend fun deleteLoan(loan: LoanEntity) =
        loanDao.deleteLoan(loan)

    override fun getTotalBorrowedAmount(): Flow<Double> =
        loanDao.getTotalBorrowedAmount().map { it ?: 0.0 }

    override fun getTotalLentAmount(): Flow<Double> =
        loanDao.getTotalLentAmount().map { it ?: 0.0 }
}
