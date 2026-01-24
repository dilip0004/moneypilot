# MoneyPilot Issue Tracker

This file tracks issues found during static code review + fixes applied.

## Status legend
- [x] Fixed
- [ ] Open / needs follow-up

---

## P0 — Data loss / crash risks

- [x] **Room destructive migration on schema change**  
  **File:** `app/src/main/java/com/yourname/moneypilot/data/local/database/MoneyPilotDatabase.kt`  
  **Fix:** Destructive migration now runs only in `BuildConfig.DEBUG`. Production builds will not wipe DB.

- [x] **Null-crash operators (`!!`) used in code**  
  **Files:** multiple (usecase + viewmodels + screens)  
  **Fix:** Replaced with `requireNotNull(...)` or safe handling.

---

## P1 — Data integrity / behavior

- [ ] **Hard delete of categories/subcategories can orphan transactions**  
  **Impact:** Transactions store `category_id`/`subcategory_id`. Deletion can cause historical entries to lose labels.  
  **Recommended approach:** Implement `isArchived` flag instead of delete; prevent delete if referenced.

- [ ] **Room foreign key enforcement**  
  **Impact:** SQLite foreign keys may not be enforced unless enabled.  
  **Recommended:** Ensure `RoomDatabase.Builder.setJournalMode` etc; optionally enable FK via callback.

---

## P2 — Security / networking

- [x] **Cleartext HTTP scan**  
  **Finding:** Only schema/documentation links present (e.g., Android XML namespace, Gradle docs). No runtime cleartext traffic found.

---

## P3 — UX / maintainability

- [ ] **Hardcoded strings**  
  **Finding:** Many UI strings are inline.  
  **Recommendation:** Move to `res/values/strings.xml` and use `stringResource(...)`.

- [ ] **Currency formatting**  
  **Finding:** Amount displayed as raw `₹{amount}`.  
  **Recommendation:** Use `NumberFormat.getCurrencyInstance(Locale("en","IN"))`.

- [ ] **Text overflow**  
  **Finding:** Long category/subcategory/description can overflow.  
  **Recommendation:** use `maxLines=1` + `TextOverflow.Ellipsis` (some already present).

---

## Completed ZIP artifacts
- `moneypilot_fixed.zip` — Category/subcategory display logic fixed.
- `moneypilot_pretty_ui.zip` — Improved transaction card UI.
- `moneypilot_all_issues_fixed.zip` — Includes all fixes applied in this review.


## Loan module upgrades (Borrower-style)

- [x] Added `LoanEventEntity` event sourcing table (rate changes, prepayment, EMI posting).
- [x] Added Loan Details screen with analytics charts.
- [x] Added Loan calculation engine with ROI timeline + prepayment support.
- [x] Added auto-deduction catch-up processor on app start.

- [x] Added Loan Details UI actions (dialogs) to record ROI change and prepayment events.

- [x] Added delete support for loan events (long-press timeline item).
- [x] Added approximate interest-saved metric after prepayment.

- [x] Added tap-to-edit loan events + UNDO delete snackbar.

- [x] Added reusable Material3 date picker field component and integrated into Loan dialogs.

- [x] Added reusable Date+Time picker field and integrated into Add/Edit Transaction screen.

- [x] Standardized date picker UI across Goals and Big Bills screens.


## Monthly cycle + history

- [x] Added monthly account snapshot table.
- [x] Added monthly rollover processor to generate immutable snapshots for previous month.
- [x] Added repository + DAO support.

- [x] Removed GlobalScope usage from Compose (Loan Details) and replaced with rememberCoroutineScope.
- [x] Improved monthly snapshot balance calculation to include additional transaction types.
- [x] Added Accounts screen month filter chips and last-month snapshot summary.
