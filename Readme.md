# ExpenseTracker — Documentation

A modern Android expense tracking app built with **Kotlin**, **Jetpack Compose**, **Room**, **Hilt**, and **MVVM**.

## Overview

ExpenseTracker helps you track personal money with salary-aware months, Cash/Bank/Wallet accounts, reimbursements, budgets, and local backup via import/export.

## Features (Current)

### Transactions
- **Add / edit income and expense** — bottom nav **Add** tab (+ icon)
- **Optional sub-description** — e.g. main “Supermarket X”, sub “coffee, chips…”
- **Transfer** Cash ↔ Bank — from Add screen
- **Wallet moves** Wallet ↔ Cash/Bank — salary-month scoped pocket money
- **Categories** — predefined + custom (Categories tab)
- **Account types** — Cash, Bank; Wallet for monthly allowance moves

### History
- **Three tabs**: Overview | Transactions | Filters
- **Overview**: collapsible month summary, salary reminder, wallet year timeline (salary mode)
- **Transactions**: grouped list, swipe-left to delete
- **Filters**: calendar/salary mode, period (Day/Week/Month/Year), type, category + budget progress
- Tap row to **edit**; account badge (Cash/Bank) on each row

### Statistics
- Period filters: Day, Week, Month, Year
- Totals: Income, Expense, **Wallet**, Balance
- Bar chart: Income (green), Expense (red), Wallet (purple)

### Settings
- **Language**: English / Arabic (RTL)
- **App lock**: Biometric / device PIN required on open (toggle in Settings)
- **Balances**: recalibrate current cash/bank to match reality
- **Export**: filter (all / current month) + format (**CSV** styled spreadsheet or **PDF** report)
- **Import CSV**: documented format + download import template
- **Database browser**: inspect Room tables with column filters
- Salary reminder management

## Navigation

| Bottom tab | Route | Purpose |
|------------|-------|---------|
| History | `history` | Transaction list & summaries |
| **Add** | `addTransaction?...` | Add/edit income & expense |
| Stats | `statistics` | Charts |
| Categories | `categories` | Category & budget management |

Additional routes (from History / Add): `settings`, `databaseBrowser`, `transfer`, `wallet`, `editTransfer/{id}`, `editWallet/{id}`.

After **Save Transaction**, app navigates to **History**.

## Security

- **Biometric app lock** (enabled by default): fingerprint or device PIN/pattern required before any financial data is shown.
- Failed or cancelled authentication shows an error on the lock screen; data stays hidden.
- App re-locks when sent to background.
- Screenshots blocked while locked (`FLAG_SECURE`).
- Disable in **Settings → App lock** if needed.

## Architecture

- **MVVM** — Compose UI + ViewModels + StateFlow
- **Repository pattern** — Room DAOs behind repositories
- **Hilt** — dependency injection
- **Domain calculators** — `PeriodCalculator`, `BalanceCalculator`, `WalletCalculator`, `BudgetProgressCalculator`
- **Offline-first** — all data in local Room DB (version **10**)

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
│   ├── BalanceCalculator.kt, WalletCalculator.kt, PeriodCalculator.kt
│   ├── CsvExporter.kt, CsvImporter.kt, CsvImportFormat (in TransactionExportRow.kt)
│   ├── PdfTransactionExporter.kt, StyledExcelExporter.kt
│   ├── TransactionExportLoader.kt, DatabaseInspector.kt
│   └── ...
├── presentation/
│   ├── navigation/AppRoutes.kt
│   ├── screens/ (History, Add, Stats, Categories, Settings, Wallet, Transfer, DatabaseBrowser)
│   ├── components/ (TransactionItem, SwipeableTransactionItem, StatisticsBarChart, BiometricGate)
│   └── viewModel/
└── MainActivity.kt, App.kt
```

## Database (Room v10)

### Entities
- `Transaction` — amount, description, **subDescription**, date, type, categoryId, accountType, toAccountType, startsNewPeriod, **carriedForwardBalance**, reimbursement fields, etc.
- `Category`, `Budget`, `RecurringTransaction`

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

- **[PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md](PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md)** — business rules, flows, file map
- **[RELEASE_NOTES.md](RELEASE_NOTES.md)** — version changelog
- **[QA_CHECKLIST.md](QA_CHECKLIST.md)** — manual test checklist

---

*For architecture decisions and edge cases, update `PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md` first, then code.*
