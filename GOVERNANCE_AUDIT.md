# MONEYPILOT — GOVERNANCE & ARCHITECTURAL AUDIT (v5.0 Compliance)

This document tracks the delta between the **Enterprise Architecture v5.0** and the current implementation.

## 1. POSITION LAYER (Wallets & Debt)
- [x] **Credit Card EMI Splitter**: Implemented via `SaveTransactionUseCase` and `LoanAutoDeductionProcessor`.
- [x] **Credit Card Statement Logic**: Implemented in `WalletStatementViewModel`.
- [x] **Loan Prepayment Processor**: Prepayments are now ledger-backed transactions via `LoanDetailsViewModel`.

## 2. INSIGHT LAYER (Analytics)
- [x] **Behavioral Pattern Engine**: Identification of "Weekend Spikes" implemented in `GenerateReflectionPromptsUseCase`.
- [x] **Configurable Asset View**: Toggle implemented in `SecurityScreen` and respected by `DashboardHubViewModel`.
- [ ] **Excel (.xlsx) Support**: Architecture requires Excel format for exports; currently JSON/CSV only.

## 3. INTENT LAYER (Planning)
- [x] **Automated Distribution Trigger**: `SurplusDistributionWorker` implemented with reliability tracking.
- [x] **Financial Forecast UI**: `ForecastScreen` and `ForecastViewModel` implemented.
- [x] **Big Bill Auto-Reserve**: `BigBillAutoReserveWorker` implemented to automate monthly smooth-out.

## 4. GOVERNANCE LAYER (Security & Integrity)
- [x] **Startup Integrity Check**: background check and Snackbar alert implemented in `MainActivity`.
- [x] **Atomic Write Enforcement**: All repository mutations now wrapped in `database.withTransaction`.
- [x] **Database Constraint Hardening**: SQLite `CHECK` constraints added for `amount != 0` and Transfer validity.
- [x] **Timezone Audit**: `LocalDateTimeConverter` refactored to enforce UTC storage.
- [x] **Database Encryption**: SQLCipher enabled in `MoneyPilotDatabase`.
