
# MoneyPilot – Product & Architecture Improvement Recommendations

## 1. Financial Integrity Improvements

### Ledger Integrity Indicator
Display wallet integrity status to users.

Example:
- ✔ Ledger Verified
- ⚠ Reconciliation Needed

Reason:
Users should see whether balances are fully reconciled with ledger entries.

---

### Historical Transaction Edit Warning

When editing older transactions show warning:

This transaction belongs to a past period.
Editing it will recalculate:
- Wallet balances
- Analytics
- Budgets
- Goals

---

### Transfer Visualization

Instead of displaying generic transfers:

Bank → Cash
₹500

Users understand the flow of money better.

---

### Refund Behaviour

Refunds should appear as:

Refund – Food > Restaurant
+₹250

Instead of negative expense values.

---

### EMI Transparency

Display EMI breakdown:

EMI Payment
Principal: ₹1800
Interest: ₹300
Total: ₹2100

This matches the ledger rule where EMI splits into transfer + expense.

---

## 2. UX Improvements

### Transaction Entry Layout

Recommended order:

1. Amount (with calculator)
2. Transaction Type
3. Wallet
4. Category/Subcategory
5. Date
6. Notes

Money entry should always start with the amount.

---

### Daily Tab Grouping

Use natural grouping:

Today
Yesterday
May 12
May 11

Instead of weekday labels.

---

### Calendar Heatmap Tooltip

When user taps a day:

May 14
Total spent: ₹2450
Transactions: 5

---

### Monthly Dashboard Improvements

Add:

- Savings Rate
- Net Surplus

These metrics help users understand progress.

---

## 3. Data Safety Improvements

### Backup Reminder

Show reminder:

Last backup: 23 days ago

Encourage regular backups.

---

### Backup Integrity Validation

Before exporting backup verify:

- Ledger integrity
- Foreign key validity
- Wallet balance reconciliation

---

### Restore Preview Screen

Before restoring backup display summary:

Transactions: 1245
Wallets: 5
Loans: 2
Goals: 3

---

## 4. Analytics Improvements

### Explicit Transfer Exclusion

Show note:

Transfers excluded from analytics.

---

### Zero Income Month Handling

Instead of showing "Efficiency: N/A"

Display:

Efficiency unavailable – No income recorded.

---

### Category Dominance Insight

Display top expense category:

Top Expense Category
Food – 32%

---

## 5. Architecture Safety Improvements

### Ledger Hash Chain (Optional Advanced Feature)

Each transaction stores:

hash(previous_hash + transaction_data)

Benefits:

- Tamper detection
- Data corruption detection
- Strong audit trail

---

### Transaction Source Tagging

All transactions should include a source type:

- USER
- TRANSFER
- GOAL
- EMI
- SYSTEM
- ADJUSTMENT

---

### Migration Validation Step

After DB migration:

- Recompute balances
- Run reconciliation engine

---

### Worker Idempotency

Workers must be safe if executed twice:

- DailySummaryWorker
- LoanAutoDeduction
- MonthlyRollover

Running twice should produce identical results.

---

## 6. UX Polish

### Undo Snackbar

For delete operations show:

Transaction deleted – UNDO

---

### Quick Category Creation

Allow category creation during transaction entry.

---

### Color Coding

Income → Green  
Expense → Red  
Transfer → Blue
