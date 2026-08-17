package com.yourname.moneypilot.data.repository

import androidx.room.Transaction
import com.yourname.moneypilot.data.local.database.dao.*
import com.yourname.moneypilot.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val goalDao: GoalDao,
    private val loanDao: LoanDao,
    private val investmentDao: InvestmentDao,
    private val budgetDao: BudgetDao,
    private val tagDao: TagDao
) : TransactionRepository {

    override fun getAllTransactionsWithDetails(): Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetails()

    override fun getTransactionsWithDetailsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsWithDetailsByDateRange(startDate, endDate)

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun getTransactionsForWallet(walletId: Long): List<TransactionEntity> =
        transactionDao.getTransactionsForWallet(walletId)

    override suspend fun getTransactionCountForWallet(walletId: Long): Int =
        transactionDao.getTransactionCountForWallet(walletId)

    override fun getTransactionsWithDetailsForWallet(
        walletId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionWithDetails>> = 
        transactionDao.getTransactionsWithDetailsForWallet(walletId, startDate, endDate)

    override suspend fun getSumBeforeDate(walletId: Long, startDate: LocalDateTime): Double =
        transactionDao.getSumBeforeDate(walletId, startDate) ?: 0.0

    @Transaction
    override suspend fun insertTransaction(transaction: TransactionEntity, tagIds: List<Long>) {
        transactionDao.insert(transaction)
        
        tagIds.forEach { tagId ->
            tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(transaction.id, tagId))
        }
        
        applyFinancialImpact(transaction, 1.0)
    }

    @Transaction
    override suspend fun createTransfer(transaction: TransactionEntity) {
        require(transaction.type == TransactionType.Transfer) { "Transaction type must be Transfer" }
        val fromId = transaction.walletFromId ?: throw IllegalArgumentException("Transfer must have from wallet")
        val toId = transaction.walletToId ?: throw IllegalArgumentException("Transfer must have to wallet")

        transactionDao.insert(transaction)
        walletDao.updateBalance(fromId, -transaction.amount)
        walletDao.updateBalance(toId, transaction.amount)
    }

    @Transaction
    override suspend fun updateTransaction(transaction: TransactionEntity, tagIds: List<Long>) {
        val oldTx = transactionDao.getTransactionById(transaction.id)
        if (oldTx != null) {
            if (oldTx.type == TransactionType.Transfer) {
                val oldFrom = oldTx.walletFromId
                val oldTo = oldTx.walletToId
                if (oldFrom != null) walletDao.updateBalance(oldFrom, oldTx.amount)
                if (oldTo != null) walletDao.updateBalance(oldTo, -oldTx.amount)
            } else {
                applyFinancialImpact(oldTx, -1.0)
            }
        }

        transactionDao.update(transaction)
        
        tagDao.deleteTagsForTransaction(transaction.id)
        tagIds.forEach { tagId ->
            tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(transaction.id, tagId))
        }

        if (transaction.type == TransactionType.Transfer) {
            val fromId = transaction.walletFromId
            val toId = transaction.walletToId
            if (fromId != null) walletDao.updateBalance(fromId, -transaction.amount)
            if (toId != null) walletDao.updateBalance(toId, transaction.amount)
        } else {
            applyFinancialImpact(transaction, 1.0)
        }
    }

    @Transaction
    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        if (transaction.type == TransactionType.Transfer) {
            val fromId = transaction.walletFromId
            val toId = transaction.walletToId
            if (fromId != null) walletDao.updateBalance(fromId, transaction.amount)
            if (toId != null) walletDao.updateBalance(toId, -transaction.amount)
        } else {
            applyFinancialImpact(transaction, -1.0)
        }
        transactionDao.delete(transaction)
    }

    override fun getTagsForTransaction(transactionId: String): Flow<List<TagEntity>> =
        tagDao.getTagsForTransaction(transactionId)

    override fun getAllTags(): Flow<List<TagEntity>> =
        tagDao.getAllTags()

    override suspend fun insertTag(tag: TagEntity): Long =
        tagDao.insertTag(tag)

    private suspend fun applyFinancialImpact(tx: TransactionEntity, multiplier: Double) {
        val amount = tx.amount * multiplier
        
        // 1. Wallet Balance Impact - FIXED LOGIC (Section 1.1)
        when (tx.type) {
            TransactionType.Income -> {
                tx.walletToId?.let { walletDao.updateBalance(it, amount) }
            }
            TransactionType.Expense -> {
                tx.walletFromId?.let { walletDao.updateBalance(it, -amount) }
            }
            TransactionType.Transfer -> {
                // Handled separately in createTransfer/updateTransaction for clarity, 
                // but included here for consistency if applyFinancialImpact is used elsewhere.
                tx.walletFromId?.let { walletDao.updateBalance(it, -amount) }
                tx.walletToId?.let { walletDao.updateBalance(it, amount) }
            }
        }

        // 2. Loan Impact
        if (tx.loanId != null) {
            val isPrincipal = tx.transactionSourceType in listOf("AUTO_EMI_PRINCIPAL", "LOAN_REPAYMENT")
            if (isPrincipal) {
                loanDao.getLoanById(tx.loanId)?.let { loan ->
                    val newOutstanding = (loan.currentBalance - amount).coerceAtLeast(0.0)
                    loanDao.updateLoan(loan.copy(currentBalance = newOutstanding))
                }
            }
        }

        // 3. Goal Impact
        if (tx.goalId != null) {
            goalDao.getGoalById(tx.goalId)?.let { goal ->
                val goalChange = if (tx.type == TransactionType.Income) -amount else amount
                val newAmount = (goal.currentAmount + goalChange).coerceAtLeast(0.0)
                goalDao.update(goal.copy(currentAmount = newAmount))
            }
        }

        // 4. Investment Impact
        if (tx.investmentId != null) {
            investmentDao.getInvestmentById(tx.investmentId)?.let { investment ->
                if (tx.transactionSourceType == "INVESTMENT_BUY") {
                    val priceToUse = if (investment.currentPrice > 0) investment.currentPrice else investment.averagePrice
                    val addedQuantity = if (priceToUse > 0) amount / priceToUse else 0.0
                    val newQuantity = (investment.quantity + addedQuantity).coerceAtLeast(0.0)
                    val totalCostBasis = (investment.quantity * investment.averagePrice) + amount
                    val newAveragePrice = if (newQuantity > 0) totalCostBasis / newQuantity else investment.averagePrice
                    
                    investmentDao.updateInvestment(investment.copy(
                        quantity = newQuantity,
                        averagePrice = newAveragePrice,
                        lastUpdated = LocalDateTime.now()
                    ))
                } else if (tx.transactionSourceType == "INVESTMENT_SELL") {
                    val priceToUse = if (investment.currentPrice > 0) investment.currentPrice else investment.averagePrice
                    val removedQuantity = if (priceToUse > 0) (amount / priceToUse) else 0.0
                    val newQuantity = (investment.quantity - removedQuantity).coerceAtLeast(0.0)
                    investmentDao.updateInvestment(investment.copy(quantity = newQuantity, lastUpdated = LocalDateTime.now()))
                }
            }
        }

        // 5. Budget Sync
        if (tx.type == TransactionType.Expense && tx.categoryId != null) {
            syncBudgets(tx)
        }
    }

    private suspend fun syncBudgets(tx: TransactionEntity) {
        val date = tx.dateTime.toLocalDate()
        val activeBudgets = budgetDao.getActiveBudgets(date).first()
        val affectedBudgets = activeBudgets.filter { 
            it.categoryId == tx.categoryId || (tx.subcategoryId != null && it.subcategoryId == tx.subcategoryId)
        }

        for (budget in affectedBudgets) {
            val start = budget.startDate.atStartOfDay()
            val end = (budget.endDate ?: tx.dateTime.toLocalDate().withDayOfMonth(tx.dateTime.toLocalDate().lengthOfMonth())).atTime(LocalTime.MAX)
            val totalSpent = if (budget.subcategoryId != null) {
                transactionDao.getSubcategoryExpenseSum(budget.subcategoryId, start, end) ?: 0.0
            } else {
                transactionDao.getCategoryExpenseSum(budget.categoryId, start, end) ?: 0.0
            }
            budgetDao.updateSpentAmount(budget.id, totalSpent)
        }
    }

    override suspend fun getCategoryExpenseSum(categoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double =
        transactionDao.getCategoryExpenseSum(categoryId, startDate, endDate) ?: 0.0

    override suspend fun getSubcategoryExpenseSum(subcategoryId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Double =
        transactionDao.getSubcategoryExpenseSum(subcategoryId, startDate, endDate) ?: 0.0

    override suspend fun getTotalSumByType(type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime): Double? =
        transactionDao.getTotalSumByType(type, startDate, endDate)
}
