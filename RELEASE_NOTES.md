# Release Notes

## Version: Feature Expansion Phase 3 (July 2026)

### Highlights
- **Wallet (monthly pocket money)**: `WALLET` account type, `WALLET_MOVE` transactions, dedicated Wallet screen, salary-month scoping, closed-period read-only guards.
- **History UI redesign**: Three tabs — **Overview**, **Transactions**, **Filters** — to reduce clutter; swipe-left-to-delete on transaction rows.
- **Sub-description** on income/expense (optional detail line shown under main description in history).
- **Arabic / English** language toggle in Settings (app restarts to apply locale; RTL supported).
- **Statistics**: Third chart bar for **Wallet** balance per period.
- **Database browser**: View all Room tables, column types, row values, and per-column filters (Settings).
- **Import / export**:
  - Export filter: all transactions or current salary/calendar month.
  - Export format: **CSV** (styled `.xls` spreadsheet) or **PDF** (styled report with app icon, title, colored headers/values).
  - **Import CSV** with documented format + downloadable plain import template.
- **Salary carry-forward**: Optional “bring previous balance into new month” when starting a salary period (`carriedForwardBalance`).
- **Balance clarity**: Unified month summary (activity vs money now vs cash/bank change); Add screen balances match History.
- **Navigation fix**: Bottom **Add** tab now navigates correctly to `AddTransactionScreen`.
- **Biometric app lock**: Fingerprint or device credentials required on launch/resume; lock screen on failure; toggle in Settings.

### Data and Migration Changes
- Database version **10**.
- `7 -> 8`: Additional default expense categories.
- `8 -> 9`: `carriedForwardBalance` on transactions.
- `9 -> 10`: `subDescription` on transactions.
- `6 -> 7`: Existing transactions default `accountType` to `BANK`.

### New / Updated Domain Services
- `WalletCalculator` — period wallet balance, open/closed salary period checks, year timeline summaries.
- `TransactionExportLoader`, `CsvExporter`, `CsvImporter`, `StyledExcelExporter`, `PdfTransactionExporter`.
- `DatabaseInspector` — raw table inspection via `PRAGMA table_info` + filtered queries.
- `CsvImportFormat` — canonical header, example row, required/optional column spec.
- `LocaleHelper` + `AppLanguage` — runtime locale via SharedPreferences + DataStore.
- `BiometricAuthManager` — BiometricPrompt wrapper; strong biometric + device credential.

### New Screens / ViewModels
- `WalletScreen`, `WalletViewModel`
- `DatabaseBrowserScreen`, `DatabaseBrowserViewModel`
- `BiometricGate`, `BiometricLockScreen`, `BiometricLockViewModel`

### UI/UX Updates
- Account badge (Cash/Bank) on each history transaction row.
- Month summary card: activity this month, your money now, cash & bank change, wallet this month.
- Settings: language, export format/scope, import format card, database browser entry.
- `values-ar/strings.xml` for Arabic strings on main screens.

---

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
