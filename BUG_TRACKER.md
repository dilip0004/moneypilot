# ============================================================
# MONEYPILOT — OFFICIAL BUG TRACKER
# ============================================================
# This document tracks active and resolved bugs to prevent
# regressions, as per AI Rule #3 & #5.
# ============================================================

# ------------------------------------------------------------
# ACTIVE BUGS (PENDING FIX)
# ------------------------------------------------------------

- [ ] **BUG-001**: Category/Subcategory fields are not visible in Add Transaction screen.
  - **Description**: The dropdowns for selecting a category and subcategory are incorrectly hidden for standard EXPENSE/INCOME types. Only the description field is visible.
  - **File(s) Affected**: `AddEditTransactionScreen.kt`, `AddEditTransactionViewModel.kt`
  - **Severity**: CRITICAL

- [ ] **BUG-002**: Transaction can be saved without selecting a subcategory.
  - **Description**: If a selected category has subcategories, the app still allows the user to save the transaction without choosing one.
  - **File(s) Affected**: `AddEditTransactionViewModel.kt`
  - **Severity**: HIGH

- [ ] **BUG-003**: "Confirm Date" step is no longer desired.
  - **Description**: The red-glow and confirmation button for the transaction date should be removed to streamline the data entry workflow.
  - **File(s) Affected**: `AddEditTransactionScreen.kt`, `AddEditTransactionViewModel.kt`
  - **Severity**: MEDIUM (Feature Change)

# ------------------------------------------------------------
# RESOLVED BUGS
# ------------------------------------------------------------

# This section will be populated as bugs are fixed.

# ============================================================
# END OF FILE
# ============================================================
