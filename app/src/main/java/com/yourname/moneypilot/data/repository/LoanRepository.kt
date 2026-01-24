package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.LoanEntity
import com.yourname.moneypilot.data.local.database.entities.LoanEventEntity
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    suspend fun getAllLoansOnce(): List<LoanEntity>
    suspend fun getLoanEventsOnce(loanId: Long): List<LoanEventEntity>

    fun getAllLoans(): Flow<List<LoanEntity>>
    fun getLoansByStatus(status: String): Flow<List<LoanEntity>>
    suspend fun getLoanById(id: Long): LoanEntity?
    suspend fun insertLoan(loan: LoanEntity): Long
    suspend fun updateLoan(loan: LoanEntity)
    suspend fun deleteLoan(loan: LoanEntity)
    fun getTotalBorrowedAmount(): Flow<Double>
    fun getTotalLentAmount(): Flow<Double>

    fun getLoanEvents(loanId: Long): kotlinx.coroutines.flow.Flow<List<LoanEventEntity>>
    suspend fun addLoanEvent(event: LoanEventEntity): Long
    suspend fun updateLoanEvent(event: LoanEventEntity)
    suspend fun deleteLoanEvent(event: LoanEventEntity)
}
