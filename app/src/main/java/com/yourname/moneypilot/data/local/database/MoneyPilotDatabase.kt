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
import com.yourname.moneypilot.data.local.database.converters.TransactionTypeConverter
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WalletEntity::class,
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
        BigBillEntity::class,
        LoanEventEntity::class
    ],
    version = 13,
    exportSchema = true
)
@TypeConverters(LocalDateConverter::class, LocalDateTimeConverter::class, TransactionTypeConverter::class)
abstract class MoneyPilotDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun tagDao(): TagDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun distributionRuleDao(): DistributionRuleDao
    abstract fun loanDao(): LoanDao
    abstract fun bigBillDao(): BigBillDao
    abstract fun loanEventDao(): LoanEventDao

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
                    if (BuildConfig.DEBUG) {
                        fallbackToDestructiveMigration()
                    } else {
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
    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts RENAME TO wallets")
            db.execSQL("ALTER TABLE wallets ADD COLUMN credit_limit REAL")
            db.execSQL("ALTER TABLE wallets ADD COLUMN billing_start_day INTEGER")
            db.execSQL("ALTER TABLE wallets ADD COLUMN billing_end_day INTEGER")
            db.execSQL("ALTER TABLE wallets ADD COLUMN due_date INTEGER")
        }
    }

    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Re-create transactions table with correct columns and constraints
            db.execSQL("""
                CREATE TABLE transactions_new (
                    id TEXT PRIMARY KEY NOT NULL, 
                    dateTime TEXT NOT NULL, 
                    amount REAL NOT NULL, 
                    type TEXT NOT NULL, 
                    category_id INTEGER, 
                    wallet_from_id INTEGER, 
                    wallet_to_id INTEGER, 
                    transaction_source_type TEXT NOT NULL, 
                    note TEXT, 
                    soft_deleted INTEGER NOT NULL DEFAULT 0, 
                    created_at TEXT NOT NULL, 
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY(wallet_from_id) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(wallet_to_id) REFERENCES wallets(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
            """)
            // Map old data to new schema, handling missing created_at/updated_at
            db.execSQL("""
                INSERT INTO transactions_new (id, dateTime, amount, type, category_id, wallet_from_id, wallet_to_id, transaction_source_type, note, soft_deleted, created_at, updated_at) 
                SELECT id, date, amount, 
                CASE WHEN type = 'INCOME' THEN 'Income' WHEN type = 'EXPENSE' THEN 'Expense' ELSE 'Transfer' END, 
                category_id, account_id, transfer_to_account_id, 'MANUAL', note, 0, datetime('now'), datetime('now') 
                FROM transactions
            """)
            db.execSQL("DROP TABLE transactions")
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS distribution_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sourceWalletId INTEGER NOT NULL, targetWalletId INTEGER NOT NULL, percentage REAL, fixedAmount REAL, priority INTEGER NOT NULL)")
        }
    }

    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN loan_id INTEGER")
        }
    }

    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory_id INTEGER")
        }
    }

    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE loans ADD COLUMN repaymentDayOfMonth INTEGER NOT NULL DEFAULT 1")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS loan_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    loan_id INTEGER NOT NULL, 
                    event_type TEXT NOT NULL, 
                    event_date TEXT NOT NULL, 
                    amount REAL, 
                    newInterestRate REAL, 
                    newMonthlyPayment REAL, 
                    newDurationMonths INTEGER, 
                    note TEXT, 
                    FOREIGN KEY(loan_id) REFERENCES loans(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_loan_events_loan_id ON loan_events (loan_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_loan_events_event_date ON loan_events (event_date)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
}
