# Money Pilot - Manual Test Plan (Architecture v5.0 compliant)

## 1. Document Purpose
This document provides a detailed flow for manual testing of the Money Pilot application. Test Case IDs (e.g., **TC-FLOW-01**) must be used as references when reporting or fixing bugs to ensure traceability to the core architecture.

---

## 2. Flow Layer: Transactions (Ledger Integrity)

### **TC-FLOW-01: Add Manual Expense**
*   **Steps:**
    1.  Navigate to the Dashboard.
    2.  Tap the "Add Transaction" FAB.
    3.  Select a Wallet (e.g., Bank).
    4.  Select a Category and a Subcategory.
    5.  Enter an amount (e.g., ₹500).
    6.  Tap "Save".
*   **Expected Result:** 
    *   Transaction appears in the "Daily" list.
    *   Wallet balance decreases by exactly ₹500.
    *   Associated budget (if any) shows ₹500 consumption.

### **TC-FLOW-02: Add Fund Transfer**
*   **Steps:**
    1.  Go to the "Transfer" screen.
    2.  Select "Source Wallet" (e.g., Bank) and "Destination Wallet" (e.g., Cash).
    3.  Enter amount (e.g., ₹1000).
    4.  Tap "Execute Transfer".
*   **Expected Result:**
    *   Source Wallet decreases by ₹1000.
    *   Destination Wallet increases by ₹1000.
    *   Transaction appears in the ledger as a "Transfer".
    *   **CRITICAL:** Total Income and Total Expense in Analytics remain unchanged.

### **TC-FLOW-03: Edit Transaction (Revert & Apply Logic)**
*   **Steps:**
    1.  Find an existing Expense of ₹100 in the Daily list.
    2.  Tap to Edit.
    3.  Change amount to ₹150.
    4.  Tap "Save".
*   **Expected Result:**
    *   Wallet balance is adjusted by -₹50 (reverts ₹100, applies ₹150).
    *   Ledger shows the updated amount.
    *   **BUG-PROTECTION:** Verify no double-deduction occurred (wallet should not have lost ₹250).

### **TC-FLOW-04: Delete Transaction**
*   **Steps:**
    1.  Long-press an Income transaction of ₹1000.
    2.  Select "Delete".
*   **Expected Result:**
    *   Transaction is removed from the list.
    *   Wallet balance decreases by exactly ₹1000 (reversion).

---

## 3. Position Layer: Wallets & Debt

### **TC-POS-01: Wallet Management**
*   **Steps:**
    1.  Go to Settings -> Wallets.
    2.  Add a new "BANK" wallet with initial balance ₹0.
    3.  Edit the wallet name to "Savings".
*   **Expected Result:** Wallet name updates correctly; balance remains ₹0.

### **TC-POS-02: Safe Deletion Check**
*   **Steps:**
    1.  Attempt to delete a wallet that has 5 transactions associated with it.
*   **Expected Result:** 
    *   System blocks deletion.
    *   Snackbar message: "Cannot delete wallet with transactions. Please archive instead."

### **TC-POS-03: Wallet Statement (Filtered Ledger)**
*   **Steps:**
    1.  Tap on a specific Wallet (e.g., "Cash").
    2.  Select a Date Range (e.g., 1st Oct to 15th Oct).
*   **Expected Result:**
    *   Only transactions for "Cash" within that range are shown.
    *   "Opening Balance" and "Closing Balance" are calculated correctly based on the ledger.

---

## 4. Intent Layer: Planning & Automation

### **TC-INTENT-01: Subcategory Budgeting**
*   **Steps:**
    1.  Go to Planning -> Budgets.
    2.  Add a budget for "Food > Swiggy" for ₹2000.
    3.  Log an expense of ₹500 under "Food > Swiggy".
*   **Expected Result:**
    *   Budget progress bar fills to 25%.
    *   Progress bar animates smoothly.

### **TC-INTENT-02: Loan EMI Auto-Deduction**
*   **Steps:**
    1.  Add a Loan (Borrowed) with EMI Date set to "Today".
    2.  Link it to "Bank Wallet".
    3.  Restart the App.
*   **Expected Result:**
    *   A split transaction is generated:
        *   Interest (Expense).
        *   Principal (Transfer/Repayment).
    *   Loan "Outstanding Balance" decreases by the principal amount.
    *   Bank Wallet decreases by the full EMI.

---

## 5. Governance Layer: Security & Audit

### **TC-GOV-01: Biometric Security**
*   **Steps:**
    1.  Go to Settings -> Security.
    2.  Enable "Use Biometrics".
    3.  Force close and restart the app.
*   **Expected Result:** 
    *   System Biometric Prompt appears.
    *   Dashboard is hidden until successful authentication.

### **TC-GOV-02: Ledger Reconciliation (The Audit)**
*   **Steps:**
    1.  Go to Settings -> App Diagnostics.
    2.  Tap "Run Ledger Integrity Check".
*   **Expected Result:**
    *   System calculates `SUM(Transactions)` vs `Stored Balance` for all wallets.
    *   If a mismatch is found, it reports the exact discrepancy and offers a "Repair" (Adjustment Transaction).

---

## 6. Edge Case Matrix

| ID | Scenario | Expected Behavior |
| :--- | :--- | :--- |
| **TC-EDGE-01** | Zero Amount Transaction | Blocked by UI validation. |
| **TC-EDGE-02** | Deleting a Transfer | Both source and destination wallets are reverted correctly. |
| **TC-EDGE-03** | Future Date Budget | Budget remains at 0% until the start date is reached. |
| **TC-EDGE-04** | Negative Wallet Balance | Allowed (Overdraft), amount displayed in Red. |
