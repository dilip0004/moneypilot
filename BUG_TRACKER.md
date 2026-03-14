# MoneyPilot - Bug Tracker

This document tracks all known active and resolved bugs in the MoneyPilot application. It is a source of truth for the project's current quality status.

---

## ACTIVE BUGS (PENDING FIX)

-   **BUG-001**: Category and Subcategory fields are not visible in the Add Transaction screen.
    -   **Status**: Partially fixed. The underlying race condition in the ViewModel has been resolved, but a full UI verification is pending.

-   **BUG-021**: Catastrophic Data Loss (Subcategories deleted during migration).
    -   **Status**: Schema fixed for new data; legacy data recovery is not possible without manual intervention.

---

## RESOLVED BUGS

-   [x] **BUG-002**: Transaction can be saved without selecting a subcategory.
    -   **Fix**: Implemented strict validation in `AddEditTransactionViewModel`.

-   [x] **BUG-003**: "Confirm Date" step is no longer desired.
    -   **Fix**: Removed `isDateConfirmed` logic from ViewModel and Screen.

-   [x] **BUG-004**: Add Account screen is non-interactive.
    -   **Fix**: Refactored ViewModel to use `StateFlow`.

-   [x] **BUG-005**: App crashes when attempting to add a transaction.
    -   **Fix**: Corrected navigation argument type mismatch in `MainActivity.kt` and refactored ID handling.
