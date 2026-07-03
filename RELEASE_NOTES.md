# Release Notes

## Version: Feature Expansion Phase 2

### Highlights
- Added salary-based month mode with stable anchor handling and period labeling.
- Added reimbursement workflow (owed/dept) with explicit `awaitingReimbursement` flag.
- Added transfer flow with edit support and balance validation.
- Added category budgets with period-aligned progress display.
- Added recurring salary reminders (banner + notification + worker path).
- Added settings features for opening balances and CSV export.

### Architecture Improvements
- Split monolithic transaction logic into feature-specific ViewModels:
  - `HistoryViewModel`
  - `AddEditTransactionViewModel`
  - `TransferViewModel`
  - `BudgetViewModel`
  - `SettingsViewModel`
- Introduced centralized route definitions in `AppRoutes`.
- Added one-shot UI event streams for safer Compose side-effects.
- Moved shared date utilities to `core/time/DateUtils` and removed domain -> presentation dependency.

### Data and Migration Changes
- Added migration path up to DB version 6.
- Added transaction fields for reimbursement linking and explicit owed state.
- Converted budgets to period-bound schema (`periodStart`, `periodEnd`) and used safe copy-forward migration.
- Improved recurring reminder advancement by ID lookup.

### UI/UX Updates
- Improved History summary and section behavior.
- Added owed visual treatment and filter semantics.
- Fixed categories screen scroll behavior.
- Added settings entry from history.

### Reliability Fixes
- Fixed Room migration mismatch crash on startup.
- Fixed false-positive owed marking (all expenses no longer auto-owed).
- Fixed salary period boundary handling regressions.

### Documentation
- Added full implementation and architecture guide:
  - `PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md`

