package com.yourname.moneypilot.data.repository

import androidx.room.withTransaction
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.dao.*
import com.yourname.moneypilot.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val database: MoneyPilotDatabase,
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

    override suspend fun insertTransaction(transaction: TransactionEntity, tagIds: List<Long>) {
        database.withTransaction {
            transactionDao.insert(transaction)
            // Section 3.2: Tag Association
            tagIds.forEach { tagId ->
                tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(transaction.id, tagId))
            }
            applyFinancialImpact(transaction, 1.0)
        }
    }

    override suspend fun createTransfer(transaction: TransactionEntity) {
        require(transaction.type == TransactionType.Transfer) { "Transaction type must be Transfer" }
        require(transaction.walletFromId != null && transaction.walletToId != null) { "Transfer must have from and to wallets" }

        database.withTransaction {
            transactionDao.insert(transaction)
            walletDao.updateBalance(transaction.walletFromId!!, -transaction.amount)
            walletDao.updateBalance(transaction.walletToId!!, transaction.amount)
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity, tagIds: List<Long>) {
        database.withTransaction {
            val oldTx = transactionDao.getTransactionById(transaction.id)
            if (oldTx != null) {
                if (oldTx.type == TransactionType.Transfer) {
                    walletDao.updateBalance(oldTx.walletFromId!!, oldTx.amount)
                    walletDao.updateBalance(oldTx.walletToId!!, -oldTx.amount)
                } else {
                    applyFinancialImpact(oldTx, -1.0)
                }
            }

            transactionDao.update(transaction)
            
            // Update tags: clear existing and re-add
            tagDao.deleteTagsForTransaction(transaction.id)
            tagIds.forEach { tagId ->
                tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(transaction.id, tagId))
            }

            if (transaction.type == TransactionType.Transfer) {
                walletDao.updateBalance(transaction.walletFromId!!, -transaction.amount)
                walletDao.updateBalance(transaction.walletToId!!, transaction.amount)
            } else {
                applyFinancialImpact(transaction, 1.0)
            }
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            if (transaction.type == TransactionType.Transfer) {
                walletDao.updateBalance(transaction.walletFromId!!, transaction.amount)
                walletDao.updateBalance(transaction.walletToId!!, -transaction.amount)
            } else {
                applyFinancialImpact(transaction, -1.0)
            }
            tagDao.deleteTagsForTransaction(transaction.id)
            transactionDao.delete(transaction)
        }
    }

    override fun getTagsForTransaction(transactionId: String): Flow<List<TagEntity>> =
        tagDao.getTagsForTransaction(transactionId)

    override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    override suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)

    private suspend fun applyFinancialImpact(tx: TransactionEntity, multiplier: Double) {
        val amount = tx.amount * multiplier

        // 1. Wallet Balance Impact
        if (tx.walletFromId != null) {
            val balanceChange = if (tx.type == TransactionType.Income || tx.isRefund) amount else -amount
            walletDao.updateBalance(tx.walletFromId, balanceChange)
        }
        
        if (tx.type == TransactionType.Income && tx.walletToId != null) {
            walletDao.updateBalance(tx.walletToId, amount)
        }

        // 2. Loan Impact
        if (tx.loanId != null) {
            if (tx.transactionSourceType != "AUTO_EMI_INTEREST" && tx.transactionSourceType != "INTEREST_POSTING") {
                loanDao.getLoanById(tx.loanId)?.let { loan ->
                    val loanChange = if (tx.isRefund) -amount else amount
                    val newOutstanding = (loan.currentBalance - loanChange).coerceAtLeast(0.0)
                    loanDao.updateLoan(loan.copy(currentBalance = newOutstanding))
                }
            }
        }

        // 3. Goal Impact
        if (tx.goalId != null) {
            goalDao.getGoalById(tx.goalId)?.let { goal ->
                val goalChange = if (tx.type == TransactionType.Expense) {
                    if (tx.isRefund) -amount else amount
                } else {
                    if (tx.isRefund) amount else -amount
                }
                val newAmount = (goal.currentAmount + goalChange).coerceAtLeast(0.0)
                goalDao.update(goal.copy(currentAmount = newAmount))
            }
        }

        // 4. Investment Impact
        if (tx.investmentId != null) {
            investmentDao.getInvestmentById(tx.investmentId)?.let { investment ->
                if (tx.type == TransactionType.Expense) {
                    val priceToUse = if (investment.averagePrice > 0) investment.averagePrice else 1.0
                    val addedQuantity = amount / priceToUse
                    val quantityChange = if (tx.isRefund) -addedQuantity else addedQuantity
                    val newQuantity = (investment.quantity + quantityChange).coerceAtLeast(0.0)
                    
                    val totalCostBasis = (investment.quantity * investment.averagePrice) + (if (tx.isRefund) -amount else amount)
                    val newAveragePrice = if (newQuantity > 0) totalCostBasis / newQuantity else investment.averagePrice

                    investmentDao.updateInvestment(investment.copy(
                        quantity = newQuantity,
                        averagePrice = newAveragePrice,
                        lastUpdated = LocalDateTime.now()
                    ))
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
            val end = budget.endDate.atTime(LocalTime.MAX)
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
