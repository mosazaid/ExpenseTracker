# ExpenseTracker — Documentation

A modern Android expense tracking app built with **Kotlin**, **Jetpack Compose**, **Room**, **Hilt**, and **MVVM**.

## Overview

ExpenseTracker helps you track personal money with salary-aware months, Cash/Bank/Wallet accounts, reimbursements, budgets, and local backup via import/export.

## Features (Current)

### Transactions
- **Add / edit income and expense** — streamlined bottom nav **Add** tab (+ icon) focused purely on Income/Expense
- **Optional sub-description** — e.g. main “Supermarket X”, sub “coffee, chips…”
- **Subcategories** — associate transactions with subcategories; select or add custom subcategories
- **Usage-based Category Sorting** — categories in dropdowns/pickers dynamically sorted by usage frequency
- **Transfer** Cash ↔ Bank — dedicated screen accessible from Overview/History
- **Wallet moves** Wallet ↔ Cash/Bank — dedicated screen for salary-month scoped pocket money
- **Categories** — predefined + custom + hierarchical subcategories (Categories tab)
- **Account types** — Cash, Bank; Wallet for monthly allowance moves
- **3-Decimal Currency Formatting** — all amounts formatted to 3 fractional decimal places (`#,##0.000 JOD`) with thousands separators
- **Harden Form Inputs** — single-line text fields with decimal keyboards and Next/Done IME actions

### History
- **Three tabs**: Overview | Transactions | Filters
- **Overview**:
  - Detailed balance cards: Cash, Bank, and total available (centered amounts and currency)
  - **Remaining Breakdown**: Clarifies **Remaining This Month** vs **Remaining Carried Over from Last Month** totaling **Total Remaining**
  - Collapsible month summary, salary reminder, wallet year timeline (salary mode)
- **Transactions**:
  - Grouped list, swipe-left to delete (trash icon hidden to prevent visual clutter), tap to edit
  - **Structured Item Rows**: Row 1 (description + Cash/Bank badge), Row 2 (category in darker grey `onSurfaceVariant` / Medium + dot • + date in lighter grey `outline`), Row 3 (subcategory chip + formatted amount)
- **Filters**:
  - Calendar/salary mode, period (Day/Week/Month/Year), type, category + budget progress
  - **Subcategory filter**: filter by specific subcategories or choose **"Other"** for transactions without assigned subcategories
- Account badge (Cash/Bank) and subcategory chip on each row

### Statistics
- Period filters: Day, Week, Month, Year
- **Interactive Multi-Chart (Last 3 Months)**:
  - Toggle between **Combined (Line + Bar)**, **Line Chart**, and **Bar Chart** views
  - Side-by-side monthly spending and income trends
- **3-Month Category Expense Pie Charts**:
  - Independent interactive donut/pie charts for the last 3 months
  - Top category spend comparison with percentage labels and legends
- Totals: Income, Expense, **Wallet**, Balance

### Reminders & Notifications
- **Daily 9:00 PM Expense Reminder**: Dual scheduling (`AlarmManager` exact/inexact fallback + `WorkManager` backup) prompts the user to log expenses if no expenses were added today.
- **Category Budget Alerts**: Real-time threshold monitoring with in-app banner warnings (85%) and push notifications (100% budget reached).
- **In-App Notification Center (`app_alerts`)**: Notification bell icon with live badge counter on `AppTopBar` linking to `AlertsScreen`.

### Debts & Loans
- **Debt Tracking & Directional Separation**:
  - Expense + Debt: Money lent to someone (debtor owes user).
  - Income + Debt: Repayment to user OR borrowed debt from someone.
  - Dedicated `DebtsScreen` with debtor grouping, settle actions, and month rollover.
- **Configured Recurring Loans**: Track car, mortgage, and personal loans in `LoansScreen`; once-per-day prompt bottom sheet (`LoanReminderBottomSheet`) on salary month rollover with salary balance preservation.

### Settings & More Hub
- **"More" Hub**: 4th bottom nav tab grouping Debts, Loans, Categories, Database Browser, and Settings.
- **Language**: English / Arabic (RTL)
- **App lock**: Biometric / device PIN required on open (toggle in Settings)
- **Balances**: recalibrate current cash/bank to match reality
- **Export**: filter (all / current month) + format (**CSV** styled spreadsheet or **PDF** report)
- **Import CSV**: documented format + download import template
- **Database browser**: inspect Room tables with column filters

## Navigation

| Bottom tab | Route | Purpose |
|------------|-------|---------|
| Overview | `overview` | Balance cards, monthly overview, and quick actions |
| History | `history` | Transaction list, overview breakdown & filters |
| **Add** (FAB) | `addTransaction?...` | Add/edit income & expense with optional time picker |
| Stats | `statistics` | Interactive multi-charts & 3-month category pie charts |
| More | `more` | Hub for Debts, Loans, Categories, Database Browser, Settings |

Additional routes: `debts`, `loans`, `alerts`, `settings`, `databaseBrowser`, `transfer`, `wallet`, `editTransfer/{id}`, `editWallet/{id}`.

Universal `AppTopBar` present across screens with live unread alert badge count.

## Security

- **Biometric app lock** (enabled by default): fingerprint or device PIN/pattern required before any financial data is shown.
- Failed or cancelled authentication shows an error on the lock screen; data stays hidden.
- App re-locks when sent to background.
- Screenshots blocked while locked (`FLAG_SECURE`).
- Disable in **Settings → App lock** if needed.

## Architecture

- **MVVM** — Compose UI + ViewModels + StateFlow
- **Repository pattern** — Room DAOs behind repository abstractions
- **Hilt** — dependency injection
- **Domain calculators** — `PeriodCalculator`, `BalanceCalculator`, `WalletCalculator`, `BudgetProgressCalculator`
- **Offline-first** — all data in local Room DB (version **11**)

### Project structure

```
app/src/main/java/com/example/expensetracker/
├── core/
│   ├── locale/LocaleHelper.kt
│   ├── security/BiometricAuthManager.kt
│   └── time/DateUtils.kt
├── data/
│   ├── database/ (entities, dao, Migrations.kt, AppDatabase.kt)
│   ├── preferences/UserPreferences.kt
│   └── repository/
├── domain/
│   ├── BalanceCalculator.kt, WalletCalculator.kt, PeriodCalculator.kt, BudgetProgressCalculator.kt
│   ├── CsvExporter.kt, CsvImporter.kt, CsvImportFormat (in TransactionExportRow.kt)
│   ├── PdfTransactionExporter.kt, StyledExcelExporter.kt
│   ├── TransactionExportLoader.kt, DatabaseInspector.kt
│   └── ...
├── presentation/
│   ├── navigation/AppRoutes.kt
│   ├── screens/ (History, Add, Stats, Categories, Settings, Wallet, Transfer, DatabaseBrowser)
│   ├── components/ (TransactionItem, MultiTypeChart, CategoryMonthPieChart, BiometricGate)
│   └── viewModel/
└── MainActivity.kt, App.kt
```

## Database (Room v11)

### Entities
- `Transaction` — amount, description, **subDescription**, date, type, categoryId, **subCategoryId**, accountType, toAccountType, startsNewPeriod, **carriedForwardBalance**, reimbursement fields, etc.
- `Category` — name, icon, color, isExpense, isDefault
- `SubCategory` — name, categoryId, isDefault
- `Budget`, `RecurringTransaction`

### Transaction types
`INCOME`, `EXPENSE`, `TRANSFER`, `WALLET_MOVE`

### Account types
`CASH`, `BANK`, `WALLET`

### Key migrations
| Version | Change |
|---------|--------|
| 6→7 | Default existing transactions to BANK |
| 7→8 | Extra default expense categories |
| 8→9 | `carriedForwardBalance` |
| 9→10 | `subDescription` |
| 10→11 | `SubCategory` table + `subCategoryId` column on `Transaction` + category consolidations |

## Unit Testing

The project includes a test suite covering domain financial calculation logic, transaction grouping, filter edge cases, 3-month statistics aggregation, and overview balance reconciliation.

Run all unit tests via Gradle:
```bash
./gradlew testDebugUnitTest
```

### Test Suites (`app/src/test/java/com/example/expensetracker/`)
1. **`domain/PeriodCalculatorTest`**: Day, week, calendar month, custom salary day intervals, and boundary roll-overs.
2. **`domain/BalanceCalculatorTest`**: Total income/expense, internal transfer neutrality, wallet moves, and carried forward balance.
3. **`domain/WalletCalculatorTest`**: Validates moves between CASH/BANK and WALLET, preventing invalid wallet-to-wallet moves.
4. **`domain/BudgetProgressCalculatorTest`**: Budget limits, percentage calculation, near-limit flags, and over-budget detection.
5. **`presentation/HistoryGroupingComprehensiveTest`**: Filter combinations (Income, Expense, Transfer, All), ASC/DESC date sorting, and category filtering.
6. **`HistoryGroupingOtherSubcategoryTest`**: Subcategory filtering with the dedicated "Other" option for unassigned subcategory transactions.
7. **`MonthSummaryRemainingTest`**: Overview balance breakdown reconciliation (Remaining This Month vs Carried Over vs Total Remaining).
8. **`presentation/StatisticsCalculationsTest`**: 3-month stats aggregation, MoM spending change, and category expense share percentages.
9. **`core/CoreUtilsTest`**: Currency formatting with 3 decimals and thousands grouping (`#,##0.000 JOD`), DateUtils custom pattern formatting (`PATTERN_FULL_DATE`, `PATTERN_ISO`), safe parsing, invalid rejection, and start/end bounds.

## Import / Export

### Export (Settings → Data)
- **Scope**: All transactions | Current salary/calendar month
- **CSV**: Styled `.xls` (HTML Excel) — app icon, title, blue headers, type-colored values
- **PDF**: Styled report — same branding, paginated table
- **Import template**: Plain `.csv` for editing and re-import

### Import (Settings → Import CSV)
Required columns: `date`, `type`, `amount`, `account`, `description`  
Date format: `yyyy-MM-dd HH:mm:ss`  
Types: `INCOME`, `EXPENSE`, `TRANSFER`, `WALLET_MOVE`  
Accounts: `CASH`, `BANK`, `WALLET`  
Lines starting with `#` are ignored.

Full spec shown in Settings → **Accepted CSV import format** (expandable card).

## Localization

- `res/values/strings.xml` — English
- `res/values-ar/strings.xml` — Arabic
- Preference stored in DataStore + SharedPreferences; `App.attachBaseContext` applies locale via `LocaleHelper`

## Build

- **Min SDK**: 26 | **Target SDK**: 35
- Open in Android Studio, sync Gradle, run `assembleDebug`

## Related docs

- **[PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md](PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md)** — comprehensive architecture evaluation, business rules, test catalog, and file map
- **[RELEASE_NOTES.md](RELEASE_NOTES.md)** — version changelog
- **[QA_CHECKLIST.md](QA_CHECKLIST.md)** — manual test checklist

---

*For architecture decisions and edge cases, update `PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md` first, then code.*
