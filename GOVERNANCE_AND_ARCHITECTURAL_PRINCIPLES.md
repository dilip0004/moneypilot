# MoneyPilot Governance & Architectural Principles

This document serves as the high-level authority for development within the MoneyPilot project, as defined by the Enterprise Architecture v5 specification.

---

## 1. Ledger Safety (CRITICAL)

*   **Single Source of Truth:** The ledger is the absolute authority.
*   **Balance Integrity:** Stored balances are performance optimizations only. Never modify a wallet balance without a corresponding ledger transaction.
*   **Anti-Double Counting:** Never introduce logic that causes transactions to be counted twice in analytics or balances.
*   **Atomicity:** All financial writes must be executed as atomic Room transactions.
*   **Transfer Neutrality:** Transfers must never affect income or expense analytics.
*   **Integrity Guard:** If any change risks ledger integrity, development must stop and the risk must be explained.

## 2. Layer Discipline

The project follows a strict five-layer separation of concerns. Responsibilities must never be mixed across these layers:

1.  **Flow Layer:** Transactions (The movement of money).
2.  **Position Layer:** Accounts & Debt (The state of money).
3.  **Insight Layer:** Analytics (The understanding of money).
4.  **Intent Layer:** Planning (The future of money).
5.  **Governance Layer:** Settings & Safety (The rules of the system).

*   **No Mixed Logic:** Do not place analytics logic inside transaction writers.
*   **No Leaky Logic:** Business logic belongs in use cases/repositories, never in the UI.

## 3. Android Architecture Rules

*   **Strict MVVM + Repository:** ViewModels manage UI state; Repositories manage data.
*   **No Direct DB Access:** ViewModels must never access Room DAOs directly.
*   **Concurrency:** Use Kotlin Coroutines with structured concurrency. Never block the main thread.
*   **Scope Management:** Avoid `GlobalScope`.
*   **Reactivity:** Prefer `Flow` and `StateFlow` for data streams.

## 4. Bug & Regression Protection

*   **Legacy Fixes:** Previously fixed bugs (tracked in `BUG_TRACKER.md`) must not be reintroduced.
*   **Reconciliation Verification:** When modifying financial logic, verify that system reconciliation remains accurate.
*   **Minimalism:** Prefer targeted, minimal changes over large refactors unless specified.
*   **No Hacks:** Temporary "hacks" are prohibited.

## 5. Test Discipline

*   **Behavior Definition:** Existing tests define the correct system behavior.
*   **Persistence:** All existing tests must pass after any change.
*   **Financial Validation:** Any change to financial logic requires accompanying unit tests.

## 6. Change Safety Protocol

Before writing any code, the following must be verified:
1.  **Impact Analysis:** What is the impact on ledger integrity?
2.  **Double-Count Risk:** Is there any risk of double-counting?
3.  **Reconciliation Impact:** How does this affect system-wide reconciliation?
4.  **Atomicity:** Is the operation wrapped in an atomic transaction?

---
*Note: This document is the highest authority for MoneyPilot development. Violations of these principles are considered critical architectural failures.*
