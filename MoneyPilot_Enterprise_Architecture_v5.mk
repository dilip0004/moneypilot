# MONEY PILOT --- ENTERPRISE ARCHITECTURE & COMPLETE SYSTEM SPEC (v5.0)

------------------------------------------------------------------------

# 0. DOCUMENT STATUS

Version: 5.0\
Scope: Exhaustive\
Audience: Engineering, Architecture, Product, Audit\
Authority: This document overrides all prior informal specifications.

This specification defines:

-   Data architecture
-   Financial doctrine
-   Credit card modeling
-   Ledger integrity rules
-   Concurrency model
-   Failure recovery
-   Reconciliation engine
-   Analytics engine
-   Planning engine
-   Security model
-   Backup & migration strategy
-   Timezone doctrine
-   Testing doctrine
-   Performance targets
-   Edge case matrix
-   Future extensibility strategy

No financial behavior may contradict this document.

------------------------------------------------------------------------

# 1. SYSTEM IDENTITY

Money Pilot is a **ledger-driven personal finance operating system**.

## 1.1 Core Principles

1.  Ledger is the source of truth.
2.  Stored balances are performance optimizations only.
3.  No silent balance mutations.
4.  No double counting.
5.  No irreversible financial action.
6.  Flow, Position, Insight, and Intent layers must remain separate.

------------------------------------------------------------------------

# 2. ARCHITECTURE LAYERS

## 2.1 Flow Layer (Transactions)

Responsible for: - Recording financial events - Editing and deleting
events - Ensuring atomic financial writes

## 2.2 Position Layer (Accounts & Debt)

Responsible for: - Wallet balances - Credit card liabilities - Loans -
Net worth calculation

## 2.3 Insight Layer (Analytics)

Responsible for: - Aggregations - Reflection triggers - Derived
metrics - Behavioral insights

## 2.4 Intent Layer (Planning)

Responsible for: - Goals - Budgets - Big bills - Surplus distribution

## 2.5 Governance Layer (Settings)

Responsible for: - Configuration - Security - Backup - Integrity checks

------------------------------------------------------------------------

# 3. DATABASE SPECIFICATION

## 3.1 Storage Engine

-   SQLite (Room)
-   All financial writes must occur inside atomic DB transactions
-   WAL mode enabled
-   Foreign key constraints enabled

## 3.2 Transaction Table

Fields:

-   id (UUID PRIMARY KEY)
-   dateTime (UTC ISO8601)
-   amount (DECIMAL(18,2) NOT NULL)
-   type (TEXT CHECK: Income, Expense, Transfer)
-   categoryId (FOREIGN KEY, nullable)
-   subCategoryId (nullable)
-   walletFromId (nullable)
-   walletToId (nullable)
-   transactionSourceType
-   note
-   createdAt
-   updatedAt
-   softDeleted (BOOLEAN DEFAULT 0)

Indexes:

-   idx_transaction_date
-   idx_transaction_walletFrom
-   idx_transaction_walletTo
-   idx_transaction_category

Constraints:

-   amount != 0
-   Transfer must have walletFrom AND walletTo
-   Expense must have walletFrom
-   Income must have walletTo

------------------------------------------------------------------------

## 3.3 Wallet Table

Fields:

-   id (UUID PRIMARY KEY)
-   name
-   type (CASH \| BANK \| CREDIT_CARD \| SAVINGS \| INVESTMENT_LINKED)
-   initialBalance
-   currentBalance
-   creditLimit (nullable)
-   billingStartDay (nullable)
-   billingEndDay (nullable)
-   dueDate (nullable)
-   archived
-   createdAt
-   updatedAt

Index: - idx_wallet_type

------------------------------------------------------------------------

## 3.4 Loan Table

-   id
-   name
-   principalAmount
-   outstandingAmount
-   interestRate
-   dueDate
-   linkedWalletId

------------------------------------------------------------------------

## 3.5 Goal Table

-   id
-   name
-   targetAmount
-   currentAmount
-   targetDate
-   priorityLevel
-   createdAt
-   updatedAt

------------------------------------------------------------------------

## 3.6 Budget Table

-   id
-   categoryId
-   periodType (CalendarMonth)
-   limitAmount
-   carryForwardFlag
-   alertThresholdPercent

------------------------------------------------------------------------

## 3.7 Investment Table

-   id
-   name
-   type
-   investedAmount
-   currentValue
-   lastUpdated

------------------------------------------------------------------------

## 3.8 Big Bill Table

-   id
-   name
-   amount
-   dueDate
-   recurrenceType
-   linkedWalletId
-   autoReserveFlag
-   reminderDaysBefore

------------------------------------------------------------------------

# 4. ACCOUNTING DOCTRINE

## 4.1 Expense

Represents consumption.\
Impacts expense analytics immediately.\
Belongs to purchase date month.

## 4.2 Income

Represents inflow.\
Impacts income analytics.

## 4.3 Transfer

Internal movement only.\
Never impacts income or expense metrics.

------------------------------------------------------------------------

# 5. CREDIT CARD MODEL

## 5.1 Liability Behavior

Credit cards are liability wallets.

## 5.2 Billing Cycle

-   billingStartDay = 23
-   billingEndDay = 22

Billing cycle affects statements only.

Analytics use calendar date.

## 5.3 Rules

-   Purchase → Expense on transaction date
-   Payment → Transfer (Bank → CreditCard)
-   Refund → Reverse original expense
-   Partial payment allowed
-   Interest posting must create new Expense transaction
-   EMI must split into principal (Transfer) + interest (Expense)

------------------------------------------------------------------------

# 6. ANALYTICS ENGINE

## 6.1 Periods

-   Daily
-   Weekly
-   Monthly (Calendar)
-   Yearly

## 6.2 Formulas

Efficiency = (Income - Expense) / Income (if Income \> 0 else N/A)

Velocity = TotalAmount / DaysInPeriod

Frequency = Count(transactions)

Dominance = CategoryAmount / TotalExpense

Transfers excluded from analytics.

## 6.3 Insight Rules

-   Maximum 2 insights per screen
-   Insights must be explainable
-   Suppress if insufficient data
-   Never judgmental language

------------------------------------------------------------------------

# 7. NET WORTH ENGINE

NetWorth = Sum(Assets) - Sum(Liabilities)

Assets: - Cash - Bank - Savings - Investments - Goals (configurable
inclusion)

Liabilities: - Credit Cards - Loans

------------------------------------------------------------------------

# 8. PLANNING ENGINE

## 8.1 Surplus

Surplus = Income - Expense (Calendar Month)

## 8.2 Distribution

-   Must generate Transfer transactions
-   Must be atomic
-   Must not bypass ledger

## 8.3 Goal Allocation

-   Reduces wallet
-   Increases goal.currentAmount
-   Ledger-backed only

------------------------------------------------------------------------

# 9. RECONCILIATION ENGINE

DerivedBalance = SUM(transaction impacts)

If mismatch: 1. Flag wallet 2. Offer repair 3. Create Adjustment
transaction if approved

Automatic integrity check on app launch.

------------------------------------------------------------------------

# 10. CONCURRENCY & ATOMICITY

All financial writes must:

-   Use DB transaction block
-   Insert transaction
-   Update wallet balance
-   Commit atomically

Failure → rollback.

------------------------------------------------------------------------

# 11. FAILURE RECOVERY

On startup:

-   Run integrity check
-   Validate DB schema version
-   Validate checksum of last backup

Restore must:

-   Rebuild ledger first
-   Recalculate balances
-   Validate totals

------------------------------------------------------------------------

# 12. TIMEZONE DOCTRINE

-   All dates stored in UTC
-   Display localized
-   Day boundary = device locale midnight
-   Billing cycle calculated in local time
-   DST handled via UTC conversion

------------------------------------------------------------------------

# 13. BACKUP & MIGRATION

## 13.1 Backup

Formats: - CSV - JSON - Excel

Includes: - SchemaVersion - ExportTimestamp - CurrencyCode

## 13.2 Migration

-   Increment schemaVersion on DB change
-   Provide migration scripts
-   Never destructive migration in production

------------------------------------------------------------------------

# 14. SECURITY MODEL

-   Biometric lock
-   Optional SQLCipher (future)
-   No background SMS scraping
-   No external auto-sync
-   No silent automation

------------------------------------------------------------------------

# 15. PERFORMANCE TARGETS

-   100k+ transactions supported
-   Cold start \< 2s on mid-range device
-   Analytics compute \< 200ms for monthly view
-   Reconciliation \< 500ms for 100k transactions

------------------------------------------------------------------------

# 16. EDGE CASE MATRIX

-   Zero income month
-   Negative income
-   Income \< Expense
-   Refund across billing cycle
-   Deleting transfer
-   Editing historical transaction
-   Leap year February
-   Billing cycle spanning year boundary
-   Wallet archived with balance
-   Loan prepayment
-   Goal overshoot
-   Budget carry forward

All cases must preserve ledger integrity.

------------------------------------------------------------------------

# 17. TESTING DOCTRINE

-   Unit tests for transaction impact
-   Credit cycle simulation tests
-   Randomized ledger mutation tests
-   Reconciliation tests
-   Migration tests
-   Backup/restore round-trip tests

------------------------------------------------------------------------

# 18. FUTURE EXTENSIBILITY

-   Multi-currency support (ISO code ready)
-   Exchange rate table isolation
-   Feature flags
-   Modular analytics engine

------------------------------------------------------------------------

# FINAL GUARANTEE

If any feature:

-   Breaks ledger integrity
-   Introduces double counting
-   Modifies balances without transaction
-   Obscures calculation logic

It must not be implemented.
