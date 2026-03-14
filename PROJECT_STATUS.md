# MoneyPilot Project Status & Complete Backlog

## 1. Document Purpose

This document is the single source of truth for all pending development work. It tracks everything from critical architectural fixes to new feature implementation, technical debt, and quality improvements required to bring the application into full compliance with the `MoneyPilot_Enterprise_Architecture_v5.mk` specification.

---

## 2. Current Status: **CORE LEDGER STABILIZED**

The core data models and ledger logic are now compliant. Foundational layers (Data, Repository) ensure atomic financial writes and safe transaction editing.

---

## 3. Implementation Backlog

### **Phase 1: Architectural Compliance & Core Fixes (Complete)**

**Objective:** Fix the foundational layers of the application to ensure ledger integrity.

*   **Data Model & Schema:**
    *   [x] **TASK-01:** Rename `AccountEntity` to `WalletEntity` and add missing fields.
    *   [x] **TASK-02:** Refactor `TransactionEntity` to use UUID, add source type and soft delete.
    *   [x] **TASK-03:** Create `TransactionType` enum.
    *   [x] **TASK-04:** Create and validate Room `Migration` scripts.

*   **Flow Layer (Repository & Atomicity):**
    *   [x] **TASK-05:** Implement compliant `createTransfer` method in `TransactionRepository`.
    *   [x] **TASK-06:** Refactor `insertTransaction` into an atomic operation with full Revert/Apply logic for edits.
    *   [x] **TASK-07:** Remove all financial logic from all ViewModels.

*   **Core Bug Fixes:**
    *   [x] **TASK-08 (Fix BUG-001):** Fix the data-loading race condition in `AddEditTransactionViewModel`.
    *   [x] **INTERNAL-FIX-01:** Resolved build error in `LoanDao.kt`.
    *   [x] **INTERNAL-FIX-02:** Resolved cascading build errors and corrupted files.

### **Phase 2: Feature Implementation & Enhancement (Medium Priority)**

**Objective:** Build out the application's features on top of the solid foundation.

*   **Fund Transfers:**
    *   [x] **TASK-09:** Implement fully functional, compliant UI for `TransferScreen.kt`.

*   **Credit Card Features:**
    *   [x] **TASK-10:** Implement logic to handle interest posting.
    *   [x] **TASK-11:** Implement UI for viewing credit card details (Limit, Billing Cycle, Due Date).
    *   [x] **TASK-28:** Implement Configurable Billing Cycle in Wallet setup.

*   **Planning Engine:**
    *   [x] **TASK-12:** Implement the "Surplus Distribution" feature.
    *   [x] **TASK-13:** Implement budget rollover and self-healing logic.

*   **Reconciliation Engine:**
    *   [x] **TASK-14:** Implement the startup integrity check.
    *   [x] **TASK-15:** Create a UI for the user to review and approve Adjustment transactions.

*   **Analytics Engine:**
    *   [x] **TASK-16:** Refactor all analytics queries to correctly exclude `Transfer` type transactions.
    *   [x] **TASK-17:** Implement the specific formulas (Efficiency, Velocity, Dominance).

### **Phase 3: Wallet Enhancements (Complete)**

**Objective:** Improve the management and visibility of financial positions.

*   [x] **TASK-24:** Implement safe wallet deletion logic.
*   [x] **TASK-25:** Update UI/ViewModel to support editing existing wallet names and types.
*   [x] **TASK-26:** Implement `WalletStatementScreen` (Filtered Ledger view).
*   [x] **TASK-29:** Implement Date Range Selection for Wallet Statement Screen.

### **Phase 4: Technical Debt & Quality of Life (Current Priority)**

*   [x] **TASK-27:** Refactor Theme Engine for Dynamic Accent & Semantic Colors.
*   [x] **TASK-30:** Enhance Calendar UI with Heatmap and Animated Selection.
*   [x] **TASK-31:** Clean up Calendar Screen hierarchy to remove redundant summary headers.
*   [x] **TASK-32:** Implement smooth progress animations for Budgets and Goals.
*   [x] **TASK-33:** Unify and stabilize Notification Scheduling (Fix BUG-023).
*   [x] **TASK-18:** Refactor large ViewModels into smaller components. (AddEditTransactionViewModel refactored)
*   [x] **TASK-19:** Introduce a proper `domain` layer with specific Use Case classes. (SaveTransactionUseCase added)
*   [x] **TASK-20:** Implement database encryption using SQLCipher.
*   [x] **TASK-21:** Implement a biometric lock feature.
*   [ ] **TASK-22:** Write unit tests for all atomic financial operations.
*   [ ] **TASK-23:** Create simulation tests for credit cycles and reconciliation.
