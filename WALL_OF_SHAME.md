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
-   **Build Error:** Invalid parameter 'alpha' in HorizontalDivider. (Resolved)
-   **Build Error:** Version Catalog Naming Mismatch. (Resolved)
-   **Build Error:** SQLCipher Import and Missing Nav Parameters (5 errors). (Active)
