# MoneyPilot Project Status & Complete Backlog

## 1. Document Purpose

This document is the single source of truth for all pending development work. It tracks everything from critical architectural fixes to new feature implementation, technical debt, and quality improvements required to bring the application into full compliance with the `MoneyPilot_Enterprise_Architecture_v5.mk` specification.

---

## 2. Current Status: **STABLE & IMPLEMENTING AUTOMATION**

The core data models and ledger logic are now compliant. We are currently implementing advanced financial automation features.

---

## 3. Implementation Backlog

### **Phase 1: Architectural Compliance & Core Fixes (Complete)**

*   [x] **TASK-01 through TASK-08**: Core Ledger, Schema, and ViewModel race condition fixes.

### **Phase 2: Feature Implementation & Enhancement (Complete)**

*   [x] **TASK-09 through TASK-17**: Transfers, Credit Card interest posting, surplus distribution, reconciliation engine, and analytics formulas.

### **Phase 3: Wallet & UI Enhancements (Complete)**

*   [x] **TASK-24 through TASK-26**: Safe deletion, wallet editing, and professional statement views.
*   [x] **TASK-27 through TASK-32**: Theme engine, configurable billing cycles, date range pickers, and calendar heatmap/animations.

### **Phase 4: Advanced Automation & Technical Debt (Current Priority)**

*   [ ] **TASK-34:** Implement Automated EMI Deduction for Loans.
    *   [x] Update `LoanEntity` and Database Migration (v13).
    *   [ ] Implement `LoanAutoDeductionProcessor` (Split Interest/Principal logic).
    *   [ ] Update `AddEditLoanScreen` to configure EMI date and link wallets.
*   [ ] **TASK-18:** Refactor large ViewModels into smaller components.
*   [ ] **TASK-19:** Introduce a proper `domain` layer with specific Use Case classes.
*   [ ] **TASK-21:** Implement a biometric lock feature.
*   [ ] **TASK-22:** Write unit tests for all atomic financial operations.
*   [ ] **TASK-23:** Create simulation tests for credit cycles and reconciliation.
*   [ ] **TASK-20:** Implement database encryption using SQLCipher (Deferred to end).
