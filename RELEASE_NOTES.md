# Release Notes

## Version: Feature Expansion Phase 6 - UI/UX Pro Max, Stretch Features & Resilience (September 2026)

### Highlights
- **Crash Resolution & Key Uniqueness in Lists**:
  - Fixed `java.lang.IllegalArgumentException: Key was already used` in `LoansScreen` and `DebtsScreen` by introducing distinct string key namespaces (`"monthly_$id"` vs `"configured_$id"`, `"debt_rollover_$id"`).
  - Enforced `.distinctBy { it.id }` safety guards against duplicate database keys in combined lazy lists.
- **Overview Financial Summary Restructuring (Zero Truncation)**:
  - Redesigned `MonthlyFinancialSummaryCard` to eliminate text clipping on compact screens.
  - Divided metrics into a spacious top 3-column row for **Income**, **Expense**, and **Wallet** (dynamically hidden when 0.000).
  - Promoted **Net Remaining Income** into a dedicated full-width card with dynamic surplus/deficit subtitle (`net_surplus_label` / `net_deficit_label`), trending vector indicators, and high visual contrast.
  - Formatted all financial labels with soft wrapping and tabular numeric values (`font-variant-numeric: tabular-nums`).
- **Categories & Budgets Cleanup**:
  - Purged internal system types (`TRANSFER` and `WALLET_MOVE`) from category management; categories are strictly scoped to `EXPENSE` and `INCOME`.
  - Redesigned `CategoryRow` to UI/UX Pro Max standards with rounded surface elevation, circular colored iconography, clear typography hierarchy, and explicit accessible action buttons (`Savings` for budget, `Edit`, and `Delete`).
- **Notifications & Empty State Centering**:
  - Centered empty state titles and descriptive copy in `AlertsScreen` and `LoansScreen` with `TextAlign.Center` and proper horizontal padding for consistent visual rhythm.
- **Full Stretch Features Implementation**:
  - **Interactive 3-Step Onboarding Walkthrough**:
    - `OnboardingScreen` featuring a responsive `HorizontalPager`, vector illustrations, animated indicator pills, tactile feedback, and direct persistence via `UserPreferences.onboardingCompleted`.
    - Integrated into initial app launch and launchable on-demand via the *App Guide & Walkthrough* tile in `MoreScreen`.
  - **Tactile Haptic Feedback**:
    - Integrated `HapticFeedbackType.LongPress` feedback into swipe-to-delete in transaction lists and successful transaction creation.
  - **Monthly Comparison & Delta Analytics**:
    - Added `MonthlyComparisonCard` in `StatisticsScreen` comparing Current Month vs Previous Month with absolute difference, percentage badge, and 3-month average benchmark.
- **Export & Import Automated Testing**:
  - Added unit test suite `StyledExcelExporterTest` validating HTML table and styling structure.
  - Added unit test suite `CsvImporterTest` testing validation, header checks, and empty dataset edge cases.
- **Full Localization Parity**:
  - Updated English and Arabic resource strings for onboarding, comparison analytics, and net summary labels.

---

## Version: Feature Expansion Phase 5 (September 2026)

### Highlights
- **Daily 9:00 PM Expense Reminder (Dual Engine: AlarmManager + WorkManager)**:
  - Intelligently checks if any expense was logged today before notifying; skips if user already logged expenses or was already reminded.
  - **AlarmManager Engine**: Uses `setExactAndAllowWhileIdle()` when exact alarm permission is granted.
  - **Graceful Inexact Fallback & Doze Mode Handling**: When `SCHEDULE_EXACT_ALARM` is denied or restricted (Android 13/14+), seamlessly falls back to `setAndAllowWhileIdle()`. In standard standby, alerts fire within ~5–15 minutes; during deep battery Doze mode, notifications fire at the OS maintenance window (15m to 2+ hours).
  - **WorkManager Secondary Guard**: `DailyExpenseReminderWorker` operates as a concurrent failsafe against aggressive OEM background task killers.
  - Android 13/14 runtime permission flow (`POST_NOTIFICATIONS`) integrated into startup.
- **Category Budget Limits & In-App Alerts**:
  - Live budget threshold evaluation during transaction recording with in-app banner warnings at 85% and 100% capacity.
  - Persistent alert history in Room database (`app_alerts` table) with dismissed status tracking (`isDismissed`).
  - System tray notification automatically triggered if a newly added expense breaches or reaches category limits.
- **Universal Top Bar with Unread Alert Badge (`AppTopBar`)**:
  - Replaces disparate screen headers with a unified top app bar.
  - Includes a real-time reactive notification bell showing unread badge count, directly navigating to the new `AlertsScreen`.
- **Debt Tracking & Directional Separation**:
  - Clarified model: **Expense + Debt** = money lent to someone (debtor owes user); **Income + Debt** = repayment to user OR borrowed debt from someone.
  - Dedicated `DebtsScreen` with outstanding balances, debtor filter chips, settlement dialogs, and month rollover support.
- **Configured Recurring Loans & Preserved Salary**:
  - `ConfiguredLoan` and `MonthlyLoanPayment` entities for recurring liabilities (car, mortgage, personal loans).
  - Once-per-day prompt bottom sheet (`LoanReminderBottomSheet`) on month rollover / salary arrival.
  - Preserves base salary from double-deduction while maintaining clean accounting records.
- **"More" Hub Navigation Tab**:
  - 4th bottom navigation tab consolidating Loans, Debts, Categories, Database Browser, and Settings into a unified launchpad.
- **Optional Time Picker in Transaction Creation**:
  - Material 3 time picker adjacent to date picker in `AddTransactionScreen` for exact timestamp logging.
- **Full Arabic & English Localization Parity**:
  - 100% parity across `values/strings.xml` and `values-ar/strings.xml` for all alerts, reminders, debt labels, and loan actions.
- **Export Template Enhancements**:
  - Updated PDF and styled Excel exporters to represent loan repayments, debt settlements, and subcategories.

### Data and Migration Changes
- Database version **13**.
- `11 -> 12`: Subcategory seed optimizations and enhancements.
- `12 -> 13`: Added `debtType` and `isDebtSettled` to `transactions`; created `configured_loans`, `monthly_loan_payments`, and `app_alerts` tables with indexes; seeded default `Loan` and `Dept` categories.

---

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
