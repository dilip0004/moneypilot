
# MoneyPilot – Predicted Future Bugs & Risk Areas

These are issues that commonly appear in ledger‑based finance systems.

---

## 1. Historical Transaction Recalculation Bugs

Editing old transactions may break:

- Wallet balances
- Budget calculations
- Monthly analytics
- Goal allocations

Solution:
Always recompute derived values after edits.

---

## 2. Transfer Double Counting

If transfers are not excluded properly they may appear in:

- Expense totals
- Income totals
- Category charts

This creates incorrect analytics.

---

## 3. Credit Card Billing Cycle Errors

Common edge cases:

- Billing cycle across year boundary
- Refund in next billing cycle
- EMI conversion mid cycle

Ensure calendar date vs billing cycle logic is clearly separated.

---

## 4. Backup Restore Ledger Drift

During restore:

Wallet stored balances may not match ledger transactions.

Solution:
Rebuild balances from ledger during restore.

---

## 5. Worker Duplicate Execution

Android WorkManager can run workers twice.

Risk areas:

- EMI auto deduction
- Monthly rollover
- Surplus distribution

Workers must be idempotent.

---

## 6. Timezone Boundary Issues

Transactions around midnight may fall into wrong day/month.

Store timestamps in UTC and convert only for display.

---

## 7. Rounding Errors

Financial rounding errors may occur in:

- EMI interest calculations
- Budget remaining calculations
- Investment profit/loss

Use consistent decimal precision.

---

## 8. Large Dataset Performance

Analytics queries may slow down after 50k+ transactions.

Solution:

- Index transaction date
- Cache monthly summaries

---

## 9. Category Deletion Orphans

Deleting a category may leave transactions without valid references.

Prevent deletion if transactions exist.

---

## 10. Concurrent Write Conflicts

Rapid transaction entry or background workers may cause:

- Wallet balance race conditions
- Partial updates

All financial writes must use DB transactions.

---

## 11. Budget Carry Forward Errors

If months are skipped the carry forward logic may run multiple times incorrectly.

Ensure sequential month processing.

---

## 12. Goal Overshoot Logic

When contribution exceeds target:

- UI may show incorrect progress
- Net worth calculation may double count

Handle overshoot explicitly.

---

## 13. Reconciliation Loop

If mismatch repair logic is incorrect the system may create repeated adjustment transactions.

Guard against repeated repairs.

---

## 14. Import / Migration Schema Drift

Older backups may lack new fields.

Add schema version validation before restore.

---

## 15. Statement Filtering Bugs

Filtering wallet statements by date range may exclude:

- Transfers
- Adjustments
- Refunds

Ensure full transaction coverage.

---

These predicted issues should be covered in the QA test suite and automated tests where possible.
