# MoneyPilot: Technical Blueprint

## 1. App Overview
MoneyPilot is a high-density, professional-grade personal finance manager designed for Android (Power-user focused, optimized for S23 Ultra class devices). It facilitates a "Calm & Reflective" financial lifestyle through detailed logging and visual strategy.

### Core User Flows
*   **Ledger Operations**: Rapid logging of income, expenses, and transfers with an integrated calculator and SMS parsing (Magic Paste).
*   **Visual Reflection**: Deep analysis of spending patterns via animated trend lines and centered, labeled pie charts.
*   **Physical Balance Tracking**: Management of physical wallets (Bank/Cash) alongside liability tracking (Loans/Credit Cards).
*   **Strategic Growth**: Multi-tier planning involving categorical Budgets, targeted Savings Goals, and Investment Portfolios.
*   **Data Sovereignty**: Complete system state backup and restoration via versioned JSON files.

### Platform Assumptions
*   **Min SDK**: 26 (Android 8.0)
*   **Target SDK**: 34 (Android 14)
*   **UI Framework**: Jetpack Compose (Material 3)
*   **BOM Version**: 2024.12.01 (Latest stable)

---

## 2. Architecture
The application follows the **MVVM (Model-View-ViewModel)** pattern with a clean separation of concerns.

### Layer Responsibilities
*   **Data Layer**: Room Entities and DAOs provide the raw persistence. Repositories orchestrate data between local storage and the Domain/UI layers.
*   **Domain Layer**: Contains use cases for complex operations (e.g., `ExportTransactionsUseCase`). *Note: Much of the logic currently resides in ViewModels (see Risks).*
*   **UI Layer**: Composed of high-density Jetpack Compose Hubs. ViewModels manage UI state using `StateFlow` and handle user events.
*   **Dependency Injection**: Dagger/Hilt manages the singleton lifecycle of the Database, DAOs, and Repositories.

### Dependency Flow
`Composables ➔ ViewModels ➔ Repositories ➔ DAOs ➔ Room Database`

---

## 3. Data Layer
**Persistence**: SQLite managed by Room.
**Schema Version**: 7

### Primary Entities
*   `AccountEntity`: Represents physical locations of money (Cash, Bank, Credit Card).
*   `TransactionEntity`: The core ledger entry. Links to `accountId`, `categoryId`, `goalId`, and `loanId`.
*   `CategoryEntity` / `SubcategoryEntity`: Hierarchical classification system.
*   `LoanEntity`: Specialized tracking for debt (Borrowed) and assets (Lent).
*   `InvestmentEntity`: Tracks capital invested vs. current market value.
*   `BigBillEntity`: Manages large, non-monthly upcoming outflows (Insurance, Taxes).
*   `GoalEntity`: Target-based savings tracking.
*   `DistributionRuleEntity`: Automated logic for surplus allocation.

### Data Invariants
*   **Balance Sync**: Every transaction must trigger a balance update in the linked `AccountEntity`.
*   **Reverse Sync**: Deleting or updating a transaction must accurately reverse the previous balance impact before applying the new one.
*   **Foreign Keys**: Transactions are set to `CASCADE` on account deletion and `SET_NULL` on category deletion.

---

## 4. State & Lifecycle Handling
*   **Persistence across Process Death**: `AddEdit` ViewModels use `SavedStateHandle` to preserve user input.
*   **UI State Observation**: Composables observe `MutableStateFlow` from ViewModels using `collectAsStateWithLifecycle()` or `collectAsState()`.
*   **Navigation State**: `MainActivity` manages the `NavController`. Bottom hubs use `saveState = true` and `restoreState = true` to maintain scroll positions and filter contexts when switching hubs.
*   **Tab Highlighting**: Active sub-tabs (Daily, Calendar, etc.) are tracked via `selectedTabIndex` and visually anchored with a Primary Purple indicator.

---

## 5. Business Logic
*   **SMS Parsing (Magic Paste)**: A Regex-based engine (`SmsParser`) extracts amount, merchant, and account suffix from Indian bank SMS templates.
*   **Loan Velocity**: Repayments marked as `LOAN_REPAYMENT` simultaneously reduce the Wallet balance and the Loan's `currentBalance`.
*   **JSON Restore Engine**: Performs a transactional "Wipe & Write". It inserts data in a specific order (Categories ➔ Accounts ➔ Transactions) to satisfy foreign key constraints.
*   **Budget Audit**: Expenses are matched against active `BudgetEntity` records based on the transaction date and category.

---

## 6. UI Layer (The 5-Hub System)
The app is organized into 5 primary Bottom Navigation Hubs:

1.  **Transactions (Hub 1)**: Features a `ScrollableTabRow` containing Daily, Calendar, Monthly Summary, and Net Worth.
2.  **Analytics (Hub 2)**: Visualizes spending velocity. Uses a centered Donut chart with trigonometric pointer labels to prevent overlap.
3.  **Wallets (Hub 3)**: A dual-tab hub for physical balance management and detailed loan repayment history.
4.  **Planning (Hub 4)**: A strategic workspace for Goals, Budgets, Investments, and Big Bills.
5.  **Settings (Hub 5)**: Administrative controls including the **In-App Diagnostics Hub** for system integrity testing.

---

## 7. Known Fixes & Critical Constraints
*   **Icon Library Incompatibility**: MUST use `Icons.AutoMirrored` for directional icons (Back/Chevron). If unavailable, fall back to `Icons.Default` Keyboard versions.
*   **High-Density Constraint**: Sub-tabs in Hub 1 MUST use `labelSmall` and `ScrollableTabRow` to prevent text wrapping on standard Android UI scales.
*   **Numeric Precision**: All currency displays MUST use `String.format(Locale.getDefault(), "%,.2f", amount)` to ensure consistent ₹ rendering.
*   **Hilt Bindings**: All new DAOs (like `BigBillDao`) must be explicitly provided in `AppModule.kt` to avoid "Missing Binding" errors.

---

## 8. Risks & Technical Debt
*   **ViewModel Bloat**: Some business logic (like loan principal reduction) is inside ViewModels. This should eventually move to Domain UseCases.
*   **Manual Mapping**: The JSON Restore engine requires manual updates to the `MoneyPilotBackup` class every time a new entity is added to the database.
*   **SMS Variation**: `SmsParser` is optimized for major Indian banks; rural bank SMS formats may not be fully supported.
*   **UI Tests**: Missing end-to-end Compose UI tests (Diagnostics Hub partially covers this).

---

## 9. Appendix
### Glossary
*   **Hub**: A primary navigation destination in the bottom bar.
*   **Magic Paste**: The feature that parses bank SMS from the clipboard.
*   **Debt Velocity**: The speed at which a loan principal is reduced via repayments.

### Sequence: Restore from JSON
1.  User selects file via `OpenDocument` launcher.
2.  `BackupRepository` reads URI stream into a String.
3.  `Json.decodeFromString` converts content to `MoneyPilotBackup` object.
4.  `Room.withTransaction` starts.
5.  `clearAllData()` wipes all tables.
6.  Batch `insertAll()` executed for all 11 entities in dependency order.
7.  UI state triggers a full refresh.

---
**Status**: Finalized Version 7.0
