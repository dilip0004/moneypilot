package com.yourname.moneypilot.di

import android.content.Context
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
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
    fun provideAccountDao(database: MoneyPilotDatabase) = database.accountDao()

    @Provides
    fun provideTransactionDao(database: MoneyPilotDatabase) = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: MoneyPilotDatabase) = database.categoryDao()

    @Provides
    fun provideBudgetDao(database: MoneyPilotDatabase) = database.budgetDao()

    @Provides
    fun provideGoalDao(database: MoneyPilotDatabase) = database.goalDao()
}
