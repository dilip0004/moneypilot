# ============================================================
# EXTENDED PERSONAL FINANCE APP — PRODUCT BLUEPRINT
# ============================================================
# Purpose:
# Build a reflection-first personal finance app that helps
# users understand how money flows across time, not just totals.
# ============================================================


# ------------------------------------------------------------
# CORE PHILOSOPHY
# ------------------------------------------------------------

# This app is NOT just an expense tracker.
# It is a reflection tool built on truthful financial data.

# The primary goal is:
# "Help a person reflect on how their money behaves over time
#  so they can make better future decisions."

# Tracking exists only to serve reflection.
# Reflection exists only if data is trustworthy.


# ------------------------------------------------------------
# TRANSACTION TRUTH CONTRACT (NON-NEGOTIABLE)
# ------------------------------------------------------------

# A transaction is considered valid ONLY if:
# 1. Amount is exact and intentional
# 2. Date is explicitly chosen or consciously accepted
# 3. Category is intentional (not silently guessed)
# 4. The transaction can always be edited or deleted
# 5. No transaction is irreversible

# Any feature that violates this contract must be rejected.


# ------------------------------------------------------------
# CORE DEFINITIONS (VERY IMPORTANT)
# ------------------------------------------------------------

# Expense:
# - Represents consumption
# - Something that reduces net worth
# - Happens on a specific day

# Money Movement:
# - Transfer between wallets/accounts
# - Does NOT represent consumption

# Payment:
# - Settlement of a past expense
# - Must NOT be double-counted as a new expense

# These distinctions must be preserved across all phases.


# ------------------------------------------------------------
# TIME AS A FIRST-CLASS CONCEPT
# ------------------------------------------------------------

# Time is NOT metadata.
# Time is the backbone of the app.

# Priority of time views:
# 1. Day    -> Awareness & capture
# 2. Month  -> Burden & behavior
# 3. Year   -> Patterns & reflection
# 4. Future -> Intent & planning

# Any UI or feature must respect this priority.


# ------------------------------------------------------------
# INPUT PHILOSOPHY
# ------------------------------------------------------------

# Input should be:
# - Fast enough to not feel like a chore
# - Intentional enough to be meaningful

# Rule:
# Speed is allowed, but correction must be effortless.

# This means:
# - Defaults are acceptable
# - Editing must be extremely easy
# - No hidden automation without user confirmation


# ------------------------------------------------------------
# REFLECTION TRIGGERS (INTENT, NOT IMPLEMENTATION)
# ------------------------------------------------------------

# Reflection does not happen automatically.
# It must be triggered.

# Examples of valid reflection triggers:
# - Spending more on a category than last month
# - Repeated spending patterns across days
# - Unusual spikes in daily or monthly totals
# - Category dominance over time

# Charts without reflection triggers are noise.


# ------------------------------------------------------------
# PHASED DEVELOPMENT MODEL (STRICT)
# ------------------------------------------------------------

# The app MUST be built in phases.
# No phase may be skipped.
# No future-phase feature may pollute earlier phases.


# -------------------------
# PHASE 1: TRANSACTION TRUTH
# -------------------------

# Focus:
# - Daily expenses only
# - Correctness over intelligence

# Scope:
# - Add expense with explicit date
# - Edit expense
# - Delete expense (with confirmation)
# - Category selection (basic)
# - Daily and monthly totals
# - Simple list views (no calendar)

# Explicitly excluded:
# - Wallets
# - Credit cards
# - Transfers
# - Analytics
# - Planning
# - Predictions

# Exit Criteria:
# - User trusts monthly totals without external validation
# - Any mistake can be corrected in under 10 seconds
# - Dates are visible and understandable everywhere
# - No double counting exists


# -------------------------
# PHASE 2: MONEY CONTAINERS
# -------------------------

# Focus:
# - Where money comes from and goes to

# Scope:
# - Wallets (cash, bank, credit)
# - Transfers between wallets
# - Credit card expense vs payment separation

# Rule:
# - Expense truth from Phase 1 must remain untouched

# Exit Criteria:
# - Credit card usage does not distort expense totals
# - Wallet balances are trustworthy


# -------------------------
# PHASE 3: TIME-BASED VIEWS
# -------------------------

# Focus:
# - Visualizing time meaningfully

# Scope:
# - Calendar views
# - Monthly burden visualization
# - Yearly snapshots

# Rule:
# - No new data creation, only new perspectives

# Exit Criteria:
# - User can visually understand spending patterns over time


# -------------------------
# PHASE 4: REFLECTION & INSIGHTS
# -------------------------

# Focus:
# - Behavioral understanding

# Scope:
# - Trends
# - Anomalies
# - Category dominance
# - Reflection prompts

# Rule:
# - Insights must be explainable and transparent

# Exit Criteria:
# - User gains awareness, not confusion


# -------------------------
# PHASE 5: PLANNING & FUTURE INTENT
# -------------------------

# Focus:
# - Forward-looking decisions

# Scope:
# - Budgeting
# - Forecasting
# - What-if simulations

# Rule:
# - Planning is advisory, never authoritative

# Exit Criteria:
# - User feels empowered, not restricted


# ------------------------------------------------------------
# MEASURES OF SUCCESS (HUMAN, NOT JUST TECHNICAL)
# ------------------------------------------------------------

# - User checks the app daily without anxiety
# - User notices overspending within the same month
# - User trusts the numbers instinctively
# - App feels like a mirror, not a spreadsheet


# ------------------------------------------------------------
# FINAL RULE
# ------------------------------------------------------------

# If a feature makes data less truthful,
# less editable, or less understandable,
# it does not belong in this app.

# ============================================================
