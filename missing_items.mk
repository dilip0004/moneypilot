# ============================================================
# MONEYPILOT — MISSING ITEMS & COMPLIANCE GAPS
# ============================================================
# Comparison: project_status.mk vs blueprint_v2.mk
# Generated: 2025-12-29
# ============================================================

# 1. TRANSACTION TRUTH & INTENTIONALITY
# ------------------------------------------------------------
- [x] **SMS Truth Review Step**: Implemented "Review Mode" in AddEditViewModel. Users must explicitly accept parsed SMS data before saving.
- [x] **Explicit Date Acceptance**: UI now forces users to confirm or choose a date explicitly (Red highlight until confirmed).
- [x] **Irreversibility Logic Audit**: Implemented `reverseImpact` logic to perfectly undo physical and liability shifts before re-applying updates.

# 2. ADVANCED REFLECTION TRIGGERS (PHASE 4)
# ------------------------------------------------------------
- [x] **Historical Comparison**: Logic added to ReportsViewModel to compare current vs previous periods and generate growth/reduction prompts.
- [x] **Category Dominance Trigger**: Added logic to highlight categories consuming >40% of total outflow.
- [ ] **Anomaly & Spike Detection**: UI implementation pending to highlight daily anomalies in the ledger.
- [ ] **Behavioral Pattern Recognition**: Identification of repeated temporal patterns (e.g., "You tend to have high outflows on Fridays").
- [x] **Explainable Insights UI**: Implemented ReflectionCard system in ReportsScreen to display behavioral text prompts.

# 3. FUTURE INTENT & PLANNING (PHASE 5)
# ------------------------------------------------------------
- [ ] **Financial Forecasting**: Logic to project future wallet balances based on current recurring patterns and big bill schedules.
- [ ] **"What-if" Simulations**: Interactive scenario planning (e.g., "If I reduce 'Leisure' by 10%, my savings goal 'Car' will be met 3 months earlier").
- [ ] **Advisory Planning**: Transform fixed budgets into advisory systems that suggest limit adjustments based on observed behavioral drift.

# 4. TECHNICAL IMPLEMENTATION GAPS
# ------------------------------------------------------------
- [ ] **Yearly Snapshot Reflection**: Create a dedicated yearly behavior reflection view focusing on long-term patterns rather than just a bar chart.

# 5. CONTINUOUS TESTING (CT) DEBT
# ------------------------------------------------------------
- [ ] **Investments CT**: 0% test coverage for P&L calculation and gain/loss logic.
- [ ] **Big Bills CT**: 0% test coverage for scheduling and payment status logic.
- [ ] **Rural SMS Coverage**: Tests for non-standard or regional Indian bank SMS templates.

# ============================================================
# END OF GAPS
# ============================================================
