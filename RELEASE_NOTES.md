# Release Notes

## Version: Feature Expansion Phase 4 (September 2026)

### Highlights
- **Usage-based Category Sorting**: Categories across dropdowns and selection pickers automatically sort by frequency of usage across all transactions.
- **Hierarchical Subcategories & "Other" Filter**:
  - Subcategories supported in Room DB (`sub_categories` table).
  - Transaction history includes an **"Other"** subcategory filter option displaying exact spending totals for unassigned transactions.
- **Advanced 3-Month Multi-Chart & Category Donut/Pie Charts**:
  - `ThreeMonthMultiChart`: Custom Compose Canvas with switchable **Grouped Bar**, **Smooth Spline Trend Line**, and **Combined** modes.
  - `CategoryPieChart`: Interactive donut chart with touch selection, center details, and month-by-month filtering tabs.
  - Horizontal chip scrolling for clutter-free navigation across statistics screens.
- **Overview Remaining Balance Clarification**:
  - Explicit breakdown for **Remaining Income** vs **Total Remaining (Cash + Bank)** vs **Carried Over from Last Month**.
  - Centered balance numbers and labels with currency ("JOD") centered under amounts.
- **Streamlined Add Transaction**:
  - Add form restricted to **Expense** and **Income** types only.
- **3-Decimal Currency Formatting with Thousands Separator**:
  - Standardized formatting to `#,##0.000 JOD` with `Double.formatCurrency()` and `Double.formatAmount()` extension functions.
- **Form Input Hardening**:
  - Enforced `singleLine = true`, decimal keyboards with sanitization (`cleanDecimalInput`), and appropriate `ImeAction.Next` / `ImeAction.Done`.
- **Transaction History Item Visual Hierarchy**:
  - Clean 3-row layout: Row 1 (description + account badge), Row 2 (category in primary/Medium + dot • + date in outline), Row 3 (subcategory + amount).
  - Redundant trash icon hidden in favor of swipe-to-delete.
- **Universal Date Formatting (`DateUtils`)**:
  - Standardized format constants (`PATTERN_DEFAULT`, `PATTERN_FULL_DATE`, `PATTERN_ISO`, etc.), `DateUtils.format()`, `DateUtils.parse()`, and extension functions on `Date`.
- **Automated JVM Unit Test Suite**:
  - Comprehensive 28-test suite across 9 test classes covering domain calculators, grouping/filtering, overview reconciliation, and formatting.

### Data and Migration Changes
- Database version **11**.
- `10 -> 11`: Added `sub_categories` table, added `subCategoryId` column to `transactions`, and consolidated overlapping default categories.

---

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
