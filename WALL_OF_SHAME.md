# Wall of Shame: Build Error Tracker

This document tracks the number of compilation errors and critical runtime bugs.

---

## Error & Bug History

-   **Initial State (Post-Refactoring Catastrophe):** 153 Errors
-   **After Fix Passes:** 0 compilation errors.
-   **New Runtime Bug:** Wallet balance double-deduction on Edit. (Resolved)
-   **New Runtime Bug:** Subcategory can be removed on Edit. (Resolved)
-   **New Runtime Bug:** Wallet deletion is not working. (Resolved)
-   **UI Consistency Bug:** Transaction list items (Daily view) using incorrect/non-uniform colors. (Resolved)
-   **UI Consistency Bug:** Wallet Statement screen using hardcoded legacy colors. (Resolved)
-   **UI Consistency Bug (Global):** Multiple screens bypassing unified color engine. (Resolved)
-   **Runtime Bug:** Goals screen failing to render due to invalid LazyColumn DSL usage. (Resolved)
-   **Runtime Bug:** Budget spent amounts not updating on transaction changes. (Resolved)
-   **Runtime Bug:** Description field in Add/Edit Transaction screen sometimes uninteractable. (Resolved)
-   **UI Bug:** Transaction screen (Daily view) not displaying subcategories. (Resolved)
-   **CATASTROPHIC BUG:** Existing subcategory data deleted during MIGRATION_8_9. (Active)
-   **Runtime Bug:** Add Big Bill screen is non-interactive. (Resolved)
-   **Data Integrity Bug:** Categories and subcategories can be deleted even if transactions are using them. (Resolved)
-   **Notification Bug:** Inconsistent daily summary notifications. (Resolved)
-   **Runtime Bug:** Loan details screen is non-interactive/unable to save. (Resolved)
-   **Validation Bug:** Loan screen amount parsing failure and missing tenure input. (Resolved)
-   **Dependency Injection Bug:** Failed to register LoanEventDao in Hilt and Database entity list. (Resolved)
-   **Build Script Failure:** Version catalog naming mismatch resulting in 29 unresolved reference errors. (Resolved)
-   **Documentation Failure:** Failed to capture granular tab structure. (Resolved)
-   **UI & Nav Bug:** Big Bills screen using invalid color references and broken navigation. (Resolved)
-   **UI & UX Bug:** Big Bills cannot be edited, deleted, or configured for recurrence despite spec requirements. (Resolved)
-   **CATASTROPHIC RUNTIME BUG:** Persistent startup crashes due to fatal database migration syntax errors and naming mismatches (db vs database). (Resolved)
-   **Cascading Build Failure:** Incomplete parameter updates in MainActivity and PlanningHubScreen, combined with property name mismatches in LoanDetailsScreen (5 errors). (Resolved)
-   **Repository Implementation Failure:** Attempted to use `getInvestmentById` in the ViewModel without implementing it in the Dao or Repository layers. (Resolved)
-   **Naming Convention Failure:** Attempted to call `updateGoal` on `GoalDao` instead of the correct `update` method, causing an unresolved reference compilation error. (Resolved)
-   **Critical Gap: The Ghost Write Loophole:** Discovered operations (Investments/Goals) modifying balances without ledger records. Fixed via atomic transaction enforcement. (Resolved)
-   **Data Integrity Bug: Investment Double-Counting:** New investments were inflating balances by 2x due to overlapping entity and transaction updates. Fixed by making Ledger the single source of truth for holdings. (Resolved)
