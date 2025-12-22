package com.yourname.moneypilot.di

import android.content.Context
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyPilotDatabase {
        return MoneyPilotDatabase.getDatabase(context)
    }

    @Provides
    fun provideAccountDao(database: MoneyPilotDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideTransactionDao(database: MoneyPilotDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: MoneyPilotDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideBudgetDao(database: MoneyPilotDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideGoalDao(database: MoneyPilotDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideTagDao(database: MoneyPilotDatabase): TagDao = database.tagDao()

    @Provides
    fun provideInvestmentDao(database: MoneyPilotDatabase): InvestmentDao = database.investmentDao()
}
