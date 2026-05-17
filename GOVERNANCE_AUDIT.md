# MONEYPILOT — GOVERNANCE & ARCHITECTURAL AUDIT (v5.0 Compliance)

This document tracks the delta between the **Enterprise Architecture v5.0** and the current implementation.

## 1. POSITION LAYER (Wallets & Debt)
- [ ] **Credit Card EMI Splitter**: EMIs are not yet automatically split into Principal (Transfer) and Interest (Expense).
- [ ] **Credit Card Statement Logic**: No UI/Logic for 23rd-22nd billing cycle boundaries.
- [ ] **Loan Prepayment Processor**: Audit trail handles basic changes, but explicit "Principal-only" prepayment needs a dedicated flow.

## 2. INSIGHT LAYER (Analytics)
- [ ] **Behavioral Pattern Engine**: Identification of "Weekend Spikes" or "Salary Day" spending patterns.
- [ ] **Configurable Asset View**: Toggle to include/exclude Goals and specific Investments from the primary Net Worth calculation.
- [ ] **Excel (.xlsx) Support**: Architecture requires Excel format for exports; currently JSON/CSV only.

## 3. INTENT LAYER (Planning)
- [ ] **Automated Distribution Trigger**: `ExecuteDistributionUseCase` exists but is not automatically fired on month-end or income arrival.
- [ ] **Financial Forecast UI**: Visual projection of wallet "runway" based on velocity vs scheduled Big Bills.

## 4. GOVERNANCE LAYER (Security & Integrity)
- [ ] **Startup Integrity Check**: App launch must trigger a background reconciliation sum to flag drifts immediately.
- [ ] **Atomic Write Enforcement**: Audit required to ensure ALL transaction inserts are wrapped in `Room.withTransaction` with their respective wallet balance updates.
- [ ] **Database Constraint Hardening**: Add SQLite `CHECK` constraints for `amount != 0` and Transfer validity (From + To).
- [ ] **Timezone Audit**: Verify `LocalDateTimeConverter` correctly handles UTC storage as per spec section 12.0.
