package com.yourname.moneypilot.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourname.moneypilot.data.local.database.dao.AccountDao
import com.yourname.moneypilot.data.local.database.dao.BudgetDao
import com.yourname.moneypilot.data.local.database.dao.CategoryDao
import com.yourname.moneypilot.data.local.database.dao.GoalDao
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.local.database.converters.LocalDateConverter
import com.yourname.moneypilot.data.local.database.converters.LocalDateTimeConverter

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(LocalDateConverter::class, LocalDateTimeConverter::class)
abstract class MoneyPilotDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: MoneyPilotDatabase? = null

        fun getDatabase(context: Context): MoneyPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneyPilotDatabase::class.java,
                    "moneypilot.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
