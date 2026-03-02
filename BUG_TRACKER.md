# MoneyPilot - Bug Tracker

This document tracks all known active and resolved bugs in the MoneyPilot application. It is a source of truth for the project's current quality status.

---

## ACTIVE BUGS (PENDING FIX)

-   **BUG-001**: Category and Subcategory fields are not visible in the Add Transaction screen.
    -   **Status**: Partially fixed. The underlying race condition in the ViewModel has been resolved, but a full UI verification is pending.

-   **BUG-002**: Transaction can be saved without selecting a subcategory.
    -   **Status**: Not yet addressed.

-   **BUG-003**: "Confirm Date" step is no longer desired.
    -   **Status**: Not yet addressed.

-   **BUG-005**: App crashes when attempting to add a transaction.
    -   **Description**: The application crashes immediately when the user tries to save or navigate to the "Add Transaction" screen (details pending investigation).
    -   **Severity**: BLOCKER
    -   **File(s) Affected**: `AddEditTransactionScreen.kt`, `AddEditTransactionViewModel.kt` (Likely)

---

## RESOLVED BUGS

-   [x] **BUG-004**: Add Account screen is non-interactive.
    -   **Resolution Date**: Session End
    -   **Fix**: Refactored `AddEditAccountViewModel` to use a `StateFlow` instead of `mutableStateOf`, ensuring the UI correctly recomposes on state changes. The `AddEditAccountScreen` was also updated to use `collectAsState()`.
