# MoneyPilot Project Status & Complete Backlog

## 1. Document Purpose

This document is the single source of truth for all pending development work. It tracks everything from critical architectural fixes to new feature implementation, technical debt, and quality improvements required to bring the application into full compliance with the `MoneyPilot_Enterprise_Architecture_v5.mk` specification and the recent product improvement recommendations.

---

## 2. Current Status: **ENHANCING FINANCIAL INTEGRITY & UX**

The core ledger is stable. We are currently implementing advanced safety features (Idempotency, Integrity Audits) and modern UX patterns (Undo, Natural Grouping).

---

## 3. Implementation Backlog

### **Phase 1: Architectural Compliance & Core Fixes (Complete)**

*   [x] **TASK-01 through TASK-08**: Core Ledger, Schema, and ViewModel race condition fixes.

### **Phase 2: Feature Implementation & Enhancement (Complete)**

*   [x] **TASK-09 through TASK-17**: Transfers, Credit Card interest posting, surplus distribution, reconciliation engine, and analytics formulas.

### **Phase 3: Wallet & UI Enhancements (Complete)**

*   [x] **TASK-24 through TASK-26**: Safe deletion, wallet editing, and professional statement views.
*   [x] **TASK-27 through TASK-32**: Theme engine, configurable billing cycles, date range pickers, and calendar heatmap/animations.

### **Phase 4: Product & Safety Improvements (Current Priority)**

*   [x] **TASK-37:** Implement Daily Tab Grouping (Today/Yesterday).
*   [x] **TASK-38:** Implement Undo Snackbar for Transaction Deletion.
*   [x] **TASK-39:** Implement EMI Transparency (Split Principal/Interest view).
*   [x] **TASK-40:** Implement Worker Idempotency for Loan deductions.
*   [x] **TASK-41:** Implement Refund Behavior (+ Sign/Green color for expense refunds).
*   [ ] **TASK-42:** Implement Backup Integrity Validation (Ledger Audit before Export).
*   [ ] **TASK-43:** Add Savings Rate and Net Surplus to Monthly Dashboard.

### **Phase 5: Technical Debt & Quality Assurance**

*   [x] **TASK-33:** Unify and stabilize Notification Scheduling.
*   [x] **TASK-22:** Write unit tests for all atomic financial operations.
*   [x] **TASK-35:** Write unit tests for Loan EMI Auto-Deduction Engine.
*   [x] **TASK-36:** Write unit tests for Reconciliation Engine.
*   [ ] **TASK-23:** Create simulation tests for credit cycles and reconciliation.
*   [ ] **TASK-18:** Refactor large ViewModels into smaller components. (AddEditTransactionViewModel partially refactored)
*   [ ] **TASK-19:** Introduce a proper `domain` layer with specific Use Case classes. (SaveTransactionUseCase added)
*   [ ] **TASK-21:** Implement a biometric lock feature.
*   [ ] **TASK-20:** Implement database encryption using SQLCipher (Deferred to end).
