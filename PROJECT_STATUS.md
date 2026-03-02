# MoneyPilot Project Status & Complete Backlog

## 1. Document Purpose

This document is the single source of truth for all pending development work. It tracks everything from critical architectural fixes to new feature implementation, technical debt, and quality improvements required to bring the application into full compliance with the `MoneyPilot_Enterprise_Architecture_v5.mk` specification.

---

## 2. Current Status: **STABLE & AWAITING FEATURE IMPLEMENTATION**

The project is now in a stable, buildable state. All critical architectural and schema-related compilation errors have been resolved. The foundational data layer is now compliant with the v5 architecture. The next steps involve implementing the remaining features on this new, solid foundation.

---

## 3. Implementation Backlog

### **Phase 1: Architectural Compliance & Core Fixes (High Priority)**

**Objective:** Fix the foundational layers of the application to ensure ledger integrity and architectural compliance. This phase is mandatory before any feature work can proceed.

*   **Data Model & Schema:**
    *   [x] **TASK-01:** Rename `AccountEntity` to `WalletEntity` and add all missing fields (`creditLimit`, `billingStartDay`, `billingEndDay`, `dueDate`).
    *   [x] **TASK-02:** Refactor `TransactionEntity` to use a `String` UUID, add required fields (`transactionSourceType`, `softDeleted`), and rename columns (`accountId` -> `walletFromId`, etc.).
    *   [x] **TASK-03:** Create a `TransactionType` enum to replace the error-prone `String`-based transaction types.
    *   [x] **TASK-04:** Create and validate all necessary Room `Migration` scripts to apply schema changes without data loss.

*   **Core Bug Fixes:**
    *   [x] **TASK-08 (Fix BUG-001):** Fix the data-loading race condition in `AddEditTransactionViewModel` to ensure categories and subcategories load reliably. This must be done in a compliant way after the repository layer is fixed.
    *   [x] **INTERNAL-FIX-01:** Resolved build error caused by schema changes in `LoanDao.kt`.
    *   [x] **INTERNAL-FIX-02:** Resolved cascading build errors caused by schema changes and corrupted files.

### **Phase 2: Implement Compliant Flow Layer (Repository)**

**Objective:** Create a centralized, safe, and atomic "Flow Layer" within the `TransactionRepository`.

*   **Flow Layer (Repository & Atomicity):**
    *   [x] **TASK-05:** Implement a compliant, `@Transaction`-annotated `createTransfer` method in the `TransactionRepository` that uses a single `TRANSFER` type transaction.
    *   [x] **TASK-06:** Refactor the `insertTransaction` method for Income/Expense into an atomic, `@Transaction`-annotated operation that updates wallet balances safely.
    *   [x] **TASK-07:** Remove all financial logic (balance calculations) from all ViewModels (`AddEditTransactionViewModel`, etc.). This logic belongs exclusively in the Repository.

### **Phase 3: Feature Implementation & Enhancement (Medium Priority)**

**Objective:** Build out the application's features on top of the new, stable foundation.

*   **Fund Transfers:**
    *   [x] **TASK-09:** Implement a fully functional, compliant UI for `TransferScreen.kt` that uses a new, simple `TransferViewModel`.

*   **Credit Card Features:**
    *   [x] **TASK-10:** Implement logic to handle interest posting, creating a new `Expense` transaction linked to the credit card wallet.
    *   [x] **TASK-11:** Implement UI for viewing credit card details, including `creditLimit`, billing cycle dates, and `dueDate`.

*   **Planning Engine:**
    *   [x] **TASK-12:** Implement the "Surplus Distribution" feature. This includes the UI for creating `DistributionRuleEntity` objects and the backend logic to execute the distribution plan, creating real `Transfer` transactions.
    *   [x] **TASK-13:** Implement budget rollover logic based on the `carryForwardFlag` in the `BudgetEntity`.

*   **Reconciliation Engine:**
    *   [x] **TASK-14:** Implement the startup integrity check as defined in Section #9 of the architecture. It must compare stored balances against a derived balance from the transaction ledger.
    *   [x] **TASK-15:** Create a UI for the user to review and approve a "Repair" or "Adjustment" transaction if a mismatch is found.

*   **Analytics Engine:**
    *   [x] **TASK-16:** Refactor all analytics queries to correctly exclude `Transfer` type transactions.
    *   [x] **TASK-17:** Implement the specific formulas from Section #6 of the architecture, such as "Efficiency," "Velocity," and "Dominance."

### **Phase 4: Technical Debt & Quality of Life (Low Priority)**

**Objective:** Improve the long-term health, maintainability, and security of the codebase.

*   **Architecture & Refactoring:**
    *   [ ] **TASK-18:** Refactor large ViewModels (like `AddEditTransactionViewModel`) into smaller, more manageable components.
    *   [ ] **TASK-19:** Introduce a proper `domain` layer with specific Use Case classes (e.g., `CreateCompliantTransferUseCase`) to further separate business logic from the `data` layer.

*   **Security:**
    *   [ ] **TASK-20:** Implement database encryption using SQLCipher, as suggested in Section #14 of the architecture.
    *   [ ] **TASK-21:** Implement a biometric lock feature for app startup.

*   **Testing:**
    *   [ ] **TASK-22:** Write unit tests for all atomic financial operations in the repositories.
    *   [ ] **TASK-23:** Create simulation tests for the credit card billing cycle and reconciliation engine.
