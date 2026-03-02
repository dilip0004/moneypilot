package com.yourname.moneypilot.di

import com.yourname.moneypilot.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWalletRepository( // Updated
        walletRepositoryImpl: WalletRepositoryImpl
    ): WalletRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        goalRepositoryImpl: GoalRepositoryImpl
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindInvestmentRepository(
        investmentRepositoryImpl: InvestmentRepositoryImpl
    ): InvestmentRepository

    @Binds
    @Singleton
    abstract fun bindDistributionRuleRepository( // Added
        distributionRuleRepositoryImpl: DistributionRuleRepositoryImpl
    ): DistributionRuleRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(
        loanRepositoryImpl: LoanRepositoryImpl
    ): LoanRepository

    @Binds
    @Singleton
    abstract fun bindBigBillRepository(
        bigBillRepositoryImpl: BigBillRepositoryImpl
    ): BigBillRepository
}
