package com.yourname.moneypilot.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourname.moneypilot.BuildConfig
import com.yourname.moneypilot.data.local.database.dao.*
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.local.database.converters.LocalDateConverter
import com.yourname.moneypilot.data.local.database.converters.LocalDateTimeConverter
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        SubcategoryEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        InvestmentEntity::class,
        DistributionRuleEntity::class,
        LoanEntity::class,
        LoanEventEntity::class,
        MonthlyAccountSnapshotEntity::class,
        BigBillEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(LocalDateConverter::class, LocalDateTimeConverter::class)
abstract class MoneyPilotDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun tagDao(): TagDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun distributionRuleDao(): DistributionRuleDao
    abstract fun loanDao(): LoanDao
    abstract fun loanEventDao(): LoanEventDao
    abstract fun monthlySnapshotDao(): MonthlySnapshotDao
    abstract fun bigBillDao(): BigBillDao

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
                    .apply {
                    // NOTE: In debug we allow destructive migrations for development speed.
                    // For released apps, add and implement concrete Room Migration objects.
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    } else {
                        // Register migration scaffold - replace with real migrations before production releases.
                        try {
                            addMigrations(*DatabaseMigrations.ALL)
                        } catch (e: Exception) {
                            Log.w("MoneyPilotDatabase", "Failed to register migrations: ${e.message}")
                        }
                    }
                }
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DatabaseMigrations {
    // Placeholder migration from 6 -> 7. Implement actual SQL operations here when schema changes.
    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Safe conditional migration: add any new columns that may be missing
            // We query the existing columns and only ALTER TABLE to add columns
            // that are not present. This avoids destructive operations and keeps
            // the migration idempotent.
            val existingColumns = mutableSetOf<String>()
            val cursor = database.query("PRAGMA table_info('transactions')")
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    existingColumns.add(cursor.getString(nameIndex))
                }
            }
            cursor.close()

            fun addColumnIfMissing(columnDef: String, columnName: String) {
                if (!existingColumns.contains(columnName)) {
                    database.execSQL("ALTER TABLE transactions ADD COLUMN $columnDef")
                }
            }

            try {
                // boolean -> INTEGER DEFAULT 0
                addColumnIfMissing("is_recurring INTEGER DEFAULT 0", "is_recurring")
                // pattern -> TEXT
                addColumnIfMissing("recurring_pattern TEXT", "recurring_pattern")
                // transfer target account id -> INTEGER
                addColumnIfMissing("transfer_to_account_id INTEGER", "transfer_to_account_id")
                // attachment path -> TEXT
                addColumnIfMissing("attachment_path TEXT", "attachment_path")
                // note -> TEXT
                addColumnIfMissing("note TEXT", "note")
                // created_at / updated_at stored as TEXT via converters
                addColumnIfMissing("created_at TEXT", "created_at")
                addColumnIfMissing("updated_at TEXT", "updated_at")
            } catch (e: Exception) {
                Log.w("MoneyPilotDatabase", "Migration 6->7 encountered an issue: ${e.message}")
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_6_7)
}
