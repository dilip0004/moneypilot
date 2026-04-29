# MoneyPilot Implementation & Verification Tracker

This document tracks the verified status of features and architectural tasks based on direct codebase analysis.

---

## 1. Verified Implementations (Complete)

| Task ID | Feature | Verification Evidence |
| :--- | :--- | :--- |
| **Point #1** | **Padding & Alignment** | Removed double-padding and fixed FAB positioning across all core screens. |
| **Point #2, 9** | **Goal Management** | Added Add/Withdraw buttons, Emoji Picker, and fixed navigation logic. |
| **Point #3** | **Wallet Customization** | Integrated Emoji Picker for custom wallet icons. |
| **Point #4, 5, 6**| **Loan Stats & UX** | Removed clunky UI; added strict validation and payoff/months-left statistics. |
| **Point #7, 8** | **Loan Audit Trail** | Edits to ROI/EMI now auto-log historical events (LoanEventEntity). |
| **Point #10** | **Budget UI Uniformity**| Refactored Add/Edit Budget screen to match professional M3 card styles. |
| **Point #11** | **Investments 2.0** | Added support for Stocks, Gold, FD, SIP with specialized calculation logic. |
| **TASK-42** | **Integrity Audit** | `BackupViewModel.kt` now calls `VerifyLedgerIntegrityUseCase` before export. |
| **#71** | **Backup Version Sync** | Version 16 used in `BackupRepository.kt` to match current DB schema. |
| **#69** | **Loan History Backup** | `LoanEventEntity` added to JSON schema; repayment history is preserved. |
| **#70** | **Tags in Backup** | Tags and CrossRefs added to JSON backup schema. |
| **#51** | **New Wallet Types** | `SAVINGS` and `BACKUP` added to `AccountType.kt` and UI. |
| **#64** | **Planning Tab State** | Used `rememberSaveable` to persist selected tab during navigation. |
| **#14** | **Goal Auto-refresh** | Converted `GoalsViewModel.kt` to reactive flow; UI updates instantly. |
| **#52** | **Goal Withdrawals** | Added full withdrawal logic to Repo and ViewModel. |
| **#37** | **Transfer Date/Time** | Added `AppDateTimePickerField` to `TransferScreen.kt`. |
| **#73** | **CSV Export Polish** | `ExportTransactionsUseCase.kt` includes human-readable names and escaping. |
| **TASK-37** | Daily Tab Grouping | `TransactionsScreen.kt` uses `groupedTransactions` logic. |
| **TASK-38** | Undo Snackbar | Fully integrated in Transactions module. |
| **TASK-41** | Refund Behavior | Handles positive amounts for expense categories. |
| **TASK-20** | SQLCipher Encryption | Verified dependencies and DB config. |

## 2. Partial Implementations (Work in Progress)

| Task ID | Feature | Current Status | Remaining Work |
| :--- | :--- | :--- | :--- |
| **TASK-43** | Savings Rate UI | Efficiency logic exists in Domain. | Need to surface specific "Savings Rate %" on the main Dashboard Hub. |
| **TASK-21** | Biometric Lock | Logic exists in `MainActivity.kt`. | Full user flow verification (Setup -> Auth). |
| **TASK-18** | ViewModel Refactoring | Partially done. | Continue refactoring `AddEditTransactionViewModel`. |
| **TASK-19** | Domain Layer | Multiple Use Cases created. | Migrate remaining Repository calls to Use Cases. |
| **BUG-001** | Category Visibility | Partially addressed. | Full verification of Subcategory loading edge-cases. |
| **#55/#62** | Goal History | Basic history exists. | Dedicated UI screen for full goal statements missing. |

## 3. Identified Gaps & High-Priority TODOs (Not Implemented)

### **Feature Gaps**
| ID | Task | Impact |
| :--- | :--- | :--- |
| **#72** | Bank Statement Import | No support for CSV/OFX imports. |
| **#68** | Amount Blurring | Missing Privacy/Incognito mode for balance security. |
| **TASK-40** | Worker Idempotency | Risk of duplicate EMI/Loan deductions if worker retries. |
| **TASK-23** | Simulation Tests | No "Time-Travel" tests to verify multi-month cycles. |
| **#54** | Closed Goals Section | No way to hide or view completed/archived goals. |

---

## 4. Immediate Architectural Checklist
*   **Ledger Safety:** All writes must be atomic Room transactions. (Verified in `TransactionRepositoryImpl`).
*   **Double-Count Check:** Analytics must exclude `TransactionType.Transfer`. (Verified in `GetReportDataUseCase`).
*   **Validation:** Replace `toDoubleOrNull() ?: 0.0` with explicit errors. (Implemented for Loans).
*   **Layer Discipline:** Keep business logic in Use Cases.
