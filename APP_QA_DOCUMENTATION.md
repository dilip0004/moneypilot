# MoneyPilot: Structured App Documentation (QA Master Edition)

## 1. App Overview
*   **App Name:** MoneyPilot
*   **Purpose:** An enterprise-grade, ledger-driven personal finance operating system. It ensures that every balance change is backed by a verifiable transaction entry, maintaining absolute data integrity across wallets, loans, and budgets.
*   **Target Users:** Users seeking professional-grade financial tracking, automated debt management, and proactive goal planning.

---

## 2. Complete Screen & Tab Hierarchy

### **A. Core Navigation Hubs (Bottom Bar)**
1.  **Transaction Hub**
    *   **Tab 1: Daily** – Searchable list of transactions grouped by date. Includes swipe/long-press actions for Edit/Delete.
    *   **Tab 2: Calendar** – Interactive heatmap monthly spending grid. Scaling: `intensity = dayExpense / maxMonthExpense`.
    *   **Tab 3: Monthly** – Detailed cash flow summary (Income vs. Expense) and Net Surplus calculation.
    *   **Tab 4: Total** – Consolidated Net Worth overview (Assets vs. Liabilities).
2.  **Stats Hub**
    *   **Tab 1: Weekly** – Categorized analysis (Expense, Income, Cash Flow).
    *   **Tab 2: Monthly** – Categorized analysis (Expense, Income, Cash Flow).
    *   **Tab 3: Yearly** – Categorized analysis (Expense, Income, Cash Flow).
3.  **Accounts Hub**
    *   **Tab 1: Wallets** – Management of Bank, Cash, and Credit Card accounts.
    *   **Tab 2: Loans** – Management of Borrowed and Lent debt records with automated EMI tracking.
4.  **Planning Hub**
    *   **Tab 1: Goals** – Long-term savings tracking with manual/automatic contributions.
    *   **Tab 2: Budgets** – Monthly spending constraints with Category/Subcategory support.
    *   **Tab 3: Investments** – Portfolio tracking showing invested vs. current market value.
    *   **Tab 4: Big Bills** – Planning for irregular, high-value expenses.
    *   **Tab 5: Distribution** – Rules-based engine for automating surplus allocation.
5.  **Settings Hub** – Central configuration, security controls, and diagnostic tools.

### **B. Functional & Input Screens**
*   **Flow Layer:** Add/Edit Transaction (with integrated calculator), Fund Transfer.
*   **Position Layer:** Add/Edit Account, Wallet Statement (Filtered Ledger view), Add/Edit Loan, Loan Details (with vertical Audit Trail).
*   **Intent Layer:** Add/Edit Goal, Add/Edit Budget, Add/Edit Big Bill, Add/Edit Investment.
*   **Governance Layer:** Category Manager (Subcategory nesting), Security (Biometric Lock), Backup/Restore (JSON/CSV), Reconciliation Engine.

---

## 3. User Flow
*   **Authentication Flow:** App Launch → Biometric Prompt (if enabled) → Dashboard.
*   **Recording Flow:** Hub → FAB (+) → Add Transaction Screen → Save → Dashboard (Daily List updated).
*   **Management Flow:** Bottom Nav → Hub → Select Item → Detail View → Edit Action → Save.
*   **Audit Flow:** Settings → App Diagnostics → Run Integrity Check → Detect Discrepancy → Approve Adjustment.

---

## 4. Inputs in the App
*   **Financial:** Amount ( integrated calculator), Interest Rate (%), EMI Amount, Credit Limit.
*   **Logical:** Category Picker, Subcategory Picker (hierarchical), Wallet Source/Destination.
*   **Temporal:** Transaction DateTime, Loan Start Date, Goal Target Date, Billing Cycle Day (1-31).
*   **Selection:** Color Hex Picker, Emoji/Icon Picker, "Rollover Enabled" toggles.

---

## 5. Validations
*   **Core Rules:** Amount must be > 0. Transfers require distinct source and destination wallets.
*   **Mandatory:** Name is required for all primary entities (Wallets, Loans, Goals).
*   **Integrity Blocks:** Deletion of Categories or Wallets is blocked if referenced by transactions.
*   **Loan Rules:** EMI must be greater than calculated monthly interest.

---

## 6. Local Storage & Security
*   **Room Database:** Primary high-performance storage for the ledger.
*   **DataStore:** Persistence for user preferences (Theme, Currency, Biometric status).
*   **Biometrics:** Hardware-backed authentication required for app entry.
*   **Atomic Transactions:** All financial writes use Room `@Transaction` to prevent partial data corruption.

---

## 7. Background Processes
*   **DailySummaryWorker:** Pushes notification of the daily financial "weather."
*   **LoanAutoDeduction:** Startup processor that identifies due EMIs and posts split ledger entries.
*   **MonthlyRollover:** Automation for budget resets and surplus distribution.

---

## 8. Edge Cases & Failure Scenarios
*   **February 29th:** Handling EMI/Billing dates on 29/30/31 in short months.
*   **Gaps in Usage:** Processing sequential rollovers if the app is closed for multiple months.
*   **Ledger Mismatch:** Real-time detection of manual database tampering.
*   **Low Memory:** Ensuring atomic rollback if the system kills the app during a multi-step transfer.
