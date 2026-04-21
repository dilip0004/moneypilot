package com.yourname.moneypilot.di

import android.content.Context
import com.yourname.moneypilot.util.SecurityPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityPreferences(@ApplicationContext context: Context): SecurityPreferences {
        return SecurityPreferences(context)
    }
}