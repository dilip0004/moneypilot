# Money Pilot - Exhaustive Manual Test Plan (Architecture v5.0)

## 1. Document Purpose
This is the master authority for manual verification. Test Case IDs are continuous. Passing every case in this document guarantees a stable, architecturally compliant, and bug-free application.

---

## Domain 1: Flow Layer (Transactions & Ledger)

| ID | Feature | Scenario | Expected Result |
| :--- | :--- | :--- | :--- |
| **TC-0001** | Transaction Entry | Add Expense with ₹0 amount. | UI blocks saving; Error: "Amount must be greater than zero." |
| **TC-0002** | Transaction Entry | Add Income to a liability wallet (Credit Card). | CC outstanding balance decreases; Inflow correctly logged. |
| **TC-0003** | Transaction Entry | Add Transfer from Bank to Cash. | Bank -X, Cash +X. Total Income/Expense stats remain unchanged. |
| **TC-0004** | Transaction Entry | Use Subcategory (e.g. Food > Groceries). | List shows "Food > Groceries"; Both Category and Sub-budget update. |
| **TC-0005** | Transaction Entry | Calculator: Add multiple values (50 + 20 + 30). | Field populates with ₹100 accurately. |
| **TC-0006** | Transaction Entry | Input: Interactivity of Description. | Tapping Description while Calculator is open swaps to Keyboard instantly. |
| **TC-0007** | Transaction Edit | Modify Amount (₹100 to ₹500). | Wallet balance adjusted by -₹400 (Undo ₹100, Apply ₹500). |
| **TC-0008** | Transaction Edit | Modify Wallet (Bank to Cash). | Bank balance restored; Cash balance deducted. |
| **TC-0009** | Transaction Edit | Modify Type (Expense to Income). | Balance increases by 2x amount (Undo deduction, add gain). |
| **TC-0010** | Transaction Edit | Modify Category (Food to Bills). | Food budget spent decreases; Bills budget spent increases. |
| **TC-0011** | Daily List | Search by Note snippet. | Searching "lunch" filters only items with "lunch" in the note. |
| **TC-0012** | Daily List | Long-press Delete. | Transaction removed; Balance restored; List re-animates smoothly. |

---

## Domain 2: Position Layer (Accounts, Liability & Wealth)

| ID | Feature | Scenario | Expected Result |
| :--- | :--- | :--- | :--- |
| **TC-0013** | Wallet Setup | Create BANK wallet with initial ₹10,000. | Wallet appears with ₹10k; Net worth increases by ₹10k. |
| **TC-0014** | Wallet Setup | Create CC wallet with ₹0 initial and ₹50k limit. | Wallet appears; Billing cycle fields visible. |
| **TC-0015** | Wallet Management | Edit Wallet Name/Icon. | Updates reflect in Hub, List, and all Transaction pickers. |
| **TC-0016** | Wallet Management | Archive Wallet with active balance. | Wallet hidden from Hub; Total Balance still includes archived amount. |
| **TC-0017** | Wallet Management | Delete Wallet with history (Safe Block). | Blocked; User prompted to Archive instead to protect ledger. |
| **TC-0018** | CC Logic | Log transaction on Billing Day vs Day After. | Day after transaction is deferred to the next statement cycle. |
| **TC-0019** | CC Logic | Full Payment (Bank -> CC). | Transfer created; CC balance returns to ₹0 (or limit). |
| **TC-0020** | Wallet Statement | custom date range calculation. | Opening balance matches "Initial + Sum(Transactions before Range Start)". |
| **TC-0021** | Net Worth | Verify (Assets - Liabilities). | Sum of [Bank+Cash+Investments] minus [Loans+CC] is accurate. |

---

## Domain 3: Intent Layer (Planning & Budgets)

| ID | Feature | Scenario | Expected Result |
| :--- | :--- | :--- | :--- |
| **TC-0022** | Budgets | Subcategory Budget exceeding Parent. | Allowed (Planning freedom), but UI shows both progress bars. |
| **TC-0023** | Budgets | Threshold Warning (90%). | Progress bar turns **Orange** at 90.1% usage. |
| **TC-0024** | Budgets | Overflow State (100%+). | Progress bar turns **Bright Red**; Overflow amount clearly displayed. |
| **TC-0025** | Budgets | Self-Healing on Load. | App re-verifies spent_amount vs ledger on screen open. |
| **TC-0026** | Budgets | Future Budget. | Set budget for next month; verify it shows 0% until 1st of month. |
| **TC-0027** | Goals | Add Goal with Target Date 5 years out. | Appears in Planning Hub; Progress = 0%. |
| **TC-0028** | Goals | Manual Contribution (Transfer). | Wallet balance decreases; Goal progress increases; Event logged. |
| **TC-0029** | Goals | Achievement (Reaching Target). | Visual celebration/indicator shown; Status moves to "Completed". |
| **TC-0030** | Goals | Delete category used in Goal. | **Blocked**; System prevents breaking the Goal's semantic link. |
| **TC-0031** | Loans | Auto-EMI: Interest/Principal Split. | Today = EMI Date -> Link Wallet -> Restart -> 2 Ledger entries created. |
| **TC-0032** | Loans | Auto-EMI: Duplicate Prevention. | App opened multiple times on EMI day; Only one set of EMIs posted. |
| **TC-0033** | Loans | Principal Prepayment. | Balance decreases; Tenure shortened; "Interest Saved" recalculated. |
| **TC-0034** | Loans | ROI Change. | New rate applied forward; Audit trail shows ROI history log. |
| **TC-0035** | Loans | EMI Day 31 in February. | Logic correctly fires on Feb 28th/29th (Month-end fallback). |

---

## Domain 4: Insight Layer (Analytics & Visuals)

| ID | Feature | Scenario | Expected Result |
| :--- | :--- | :--- | :--- |
| **TC-0036** | Calendar | Heatmap Scaling (Max vs Min spend). | Thickest ring on highest spend day; hairline ring on lowest. |
| **TC-0037** | Analytics | Efficiency Formula Verification. | Savings % = (Income - Expense) / Income. Accurate to 2 decimals. |
| **TC-0038** | Analytics | Category Dominance. | Verifies the category with highest expense is correctly identified. |
| **TC-0039** | Visuals | Color Uniformity (Dash vs Tabs). | Income is #00C853 (Bright Green) and Expense is #FF0000 (Bright Red) everywhere. |
| **TC-0040** | Reports | Time Range Swap (Week / Month / Year). | Chart data and insights refresh instantly with correct ranges. |

---

## Domain 5: Governance & System (Security & Integrity)

| ID | Feature | Scenario | Expected Result |
| :--- | :--- | :--- | :--- |
| **TC-0041** | Security | Biometric Lock on Startup. | Splash screen blocks content until successful fingerprint/face scan. |
| **TC-0042** | Security | Stealth Mode Toggle. | Amounts blurred/masked (₹ *****) on all screens instantly. |
| **TC-0043** | Integrity | Manual DB Tamper Check. | Alter balance in DB file -> Launch -> System detects ledger mismatch. |
| **TC-0044** | Integrity | Reconciliation Repair. | Accept "Repair" -> Adjustment transaction generated -> Ledger verified. |
| **TC-0045** | System | JSON Backup Export. | File contains full schema version, timestamp, and all data entities. |
| **TC-0046** | System | JSON Restore (Clean Wipe). | All current data erased; Backup data restored with 100% fidelity. |
| **TC-0047** | Lifecycle | Background/Foreground persistence. | App minimized during transaction entry -> Returned -> Data preserved. |
| **TC-0048** | System | Locale change (INR to USD). | Currency symbols and number formatting ($ 1,234.00) update globally. |

---

## Domain 6: Advanced Permutations (Edge Cases)

| ID | Scenario | Expected Result |
| :--- | :--- | :--- |
| **TC-0049** | Leap Year Handling (Feb 29). | Recurring bills and analytics handle 29-day month correctly. |
| **TC-0050** | Deleting Category with Budgets. | **Blocked**; User must delete associated budgets first. |
| **TC-0051** | Transfer deletion between CC and Bank. | CC Liability correctly restored; Bank balance restored. |
| **TC-0052** | Loan repayment larger than balance. | UI blocks or Warns; Principal capped at outstanding amount. |
| **TC-0053** | Multi-month App Closure. | Startup catch-up logic runs multiple rollovers/EMIs in sequence. |
| **TC-0054** | Zero balance Wallet deduction. | Allowed (Overdraft); balance shown in Bright Red. |
| **TC-0055** | Category name conflict. | System prevents duplicate names in the same Category Type. |
