# AI Rules for Expense App

These rules MUST be followed by any AI agent modifying this project.

---

## 1. Source of Truth
- The existing codebase is the primary source of truth.
- Do NOT assume missing behavior.
- If unclear, ask before changing.

---

## 2. Project Awareness
Before making any change, the AI MUST read:
- PROJECT_STATUS.md
- APP_BLUEPRINT.md
- KNOWN_ISSUES_AND_FIXES.md

---

## 3. Bug Fix Protection
- Fixed bugs MUST NOT be reintroduced.
- Any code marked with FIX-XXX comments is protected.
- If a requested change conflicts with a fix, STOP and explain.

---

## 4. Tests Are Law
- Unit tests and CT/UI tests define correct behavior.
- Tests MUST NOT be changed unless explicitly requested.
- New features require at least one unit test.

---

## 5. Status Tracking
- If a feature is implemented, mark it in PROJECT_STATUS.md.
- If a bug is fixed, move it to “Fixed Bugs” with:
  - ID
  - Date
  - Guarding test (if any)

---

## 6. Change Discipline
- Do NOT refactor unrelated code.
- One logical change per request.
- Preserve public APIs unless approved.

---

## 7. Failure Handling
If unsure about:
- Architecture
- Bug history
- Feature status

The AI MUST ask before proceeding.

---

## 8. Output Rules
- Explain what was changed and why.
- List impacted files.
- Mention which rules were applied.

---

## 9. Rule Updates
- These rules may be updated as the project evolves.
- The AI should suggest rule updates if patterns emerge.