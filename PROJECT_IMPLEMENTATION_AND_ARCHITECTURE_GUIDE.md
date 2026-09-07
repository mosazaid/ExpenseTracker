# ExpenseTracker Implementation and Architecture Guide

This document is the technical and business reference for the current app state after Phase 2 and Phase 3 feature work (through DB version **10**, July 2026).

## 1. Project Purpose (Business View)

ExpenseTracker helps a user manage personal money with:

- income and expense tracking by category and account (**Cash / Bank / Wallet**)
- transfer tracking between Cash and Bank
- **monthly wallet** (pocket money) moves scoped to the open salary month
- salary-cycle aware history (not only calendar month)
- reimbursement tracking ("someone owes me")
- category budgets with progress feedback
- recurring salary reminders
- **CSV import** and **styled CSV/PDF export** for backup/share
- **Arabic and English** UI
- **Biometric app lock** on launch and resume
- developer-style **database browser** for inspecting local data

### Core business rules

1. **Two month modes**
   - `CALENDAR`: month is 1st to last day
   - `SALARY`: month starts at the latest salary-period anchor and ends before next anchor

2. **Account-aware balances**
   - balances computed separately for Cash and Bank (Wallet is separate monthly bucket)
   - transfer affects both Cash/Bank accounts
   - wallet moves affect Wallet + one liquid account (Cash or Bank)
   - opening balances are configurable; **Settings recalibrates “money now”** via `BalanceCalculator.setCurrentBalance`

3. **Wallet (salary month only)**
   - `WALLET_MOVE` increases/decreases period wallet balance
   - only the **open** salary month allows add/edit/delete of wallet moves
   - wallet balance **cannot go negative** (validated in `WalletViewModel`)

4. **Reimbursement ("owed")**
   - expense is owed only when `awaitingReimbursement = true`
   - "Dept" income can link to original expense
   - linked expenses show as reimbursed

5. **Salary period start**
   - salary income can set `startsNewPeriod = true`
   - optional `carriedForwardBalance` brings previous period net into the new month

6. **Budget period follows active month mode**
   - budget progress uses the same period bounds as History list filters

7. **Sub-description**
   - optional detail on income/expense (e.g. items bought at a store)
   - shown under main description in history (smaller, lighter text)

8. **Biometric app lock**
   - enabled by default via `UserPreferences.biometricLockEnabled`
   - `BiometricGate` wraps `MainScreen` in `MainActivity`
   - accepts **BIOMETRIC_STRONG** or **DEVICE_CREDENTIAL** (PIN/pattern)
   - on failure/cancel: lock screen + error message; no app content rendered
   - re-locks on `Lifecycle.Event.ON_STOP` (background)
   - `FLAG_SECURE` while locked (blocks screenshots)

## 2. Current Architecture

Android MVVM with Compose + Room + Hilt.

### 2.1 Layers

- **Presentation**: Compose screens + ViewModels
- **Domain**: calculators, exporters, importers, inspectors
- **Data**: Room entities/DAO + repositories + DataStore preferences
- **Core**: `core/time/DateUtils`, `core/locale/LocaleHelper`

### 2.2 ViewModels

| ViewModel | Responsibility |
|-----------|----------------|
| `HistoryViewModel` | Period bounds, month summary, filters, grouping, wallet year summary, delete guards |
| `AddEditTransactionViewModel` | Add/edit income/expense form, sub-description, salary/dept flows |
| `TransferViewModel` | Cash ↔ Bank transfers |
| `WalletViewModel` | Wallet ↔ Cash/Bank moves, validation, closed-period guards |
| `BudgetViewModel` | Category budgets from Categories screen |
| `SettingsViewModel` | Balances, language, export/import, salary reminder controls |
| `StatisticsViewModel` | Period totals including wallet bar data |
| `DatabaseBrowserViewModel` | Table listing and filtered raw queries |
| `BiometricLockViewModel` | Unlock state, error message, lock on background |

### 2.3 Domain services

| Service | Role |
|---------|------|
| `PeriodCalculator` | Day/week/month/year bounds; salary month anchors |
| `BalanceCalculator` | Cash/Bank available balance; unified month financial summary |
| `WalletCalculator` | Period wallet balance; open/closed period; salary-year timeline |
| `BudgetProgressCalculator` | Spent vs limit per category for active period |
| `TransactionExportLoader` | Filtered rows for export |
| `CsvExporter` | Plain import-compatible CSV (with `#` comment header) |
| `StyledExcelExporter` | Styled `.xls` (HTML Excel) with icon, title, colors |
| `PdfTransactionExporter` | Styled PDF report with icon, title, colored table |
| `CsvImporter` | Parse CSV, upsert by id, skip `#` lines |
| `DatabaseInspector` | `PRAGMA table_info` + filtered `SELECT` on allowed tables |
| `BiometricAuthManager` | Device capability check + `BiometricPrompt` authentication |

### 2.4 Compose patterns

- Repository pattern + MVVM + StateFlow
- Central routes: `presentation/navigation/AppRoutes.kt`
- One-shot UI events for post-save navigation (`AddEditUiEvent.NavigateHistory`)
- History organized as **TabRow**: Overview | Transactions | Filters
- `SwipeableTransactionItem` wraps `TransactionItem` with `SwipeToDismissBox`
- `BiometricGate` + `BiometricLockScreen` gate all financial UI until authenticated

### 2.5 Core security

| Component | Role |
|-----------|------|
| `BiometricAuthManager` | `BiometricManager.canAuthenticate`, show system prompt |
| `BiometricGate` | Compose wrapper; lock on stop, `FLAG_SECURE`, auto-prompt |
| `BiometricLockScreen` | Branded lock UI with error + Unlock button |

## 3. Major Features (Complete List)

### 3.1 Salary month vs calendar month
- Toggle in History → **Filters** tab
- Salary period uses anchors (`startsNewPeriod` on salary category income)
- Fallback hint when anchors missing

### 3.2 Week boundaries
- Week is **Saturday to Friday** (`DateUtils.getStartOfWeek`)

### 3.3 Transfers
- Dedicated `TransferScreen`; edit via `editTransfer/{id}`
- Insufficient balance dialog with optional `allowNegativeBalance`

### 3.4 Wallet
- Routes: `wallet`, `editWallet/{id}`
- Entry: Add screen → "Wallet ↔ Cash/Bank"
- History: wallet in month summary; year wallet timeline in Overview (salary mode)
- Type filter includes Wallet moves

### 3.5 Reimbursement
- `awaitingReimbursement`, `linkedExpenseId`, `debtorNote`
- Owed styling and filter in history

### 3.6 Budgets
- Period-bound budgets; progress in Filters → category dropdown

### 3.7 Recurring salary reminders
- Worker + notification; banner on History Overview tab

### 3.8 Settings, import/export, locale, DB browser, app lock
- **Language**: `AppLanguage` EN/AR in DataStore; `activity.recreate()` on change
- **App lock**: biometric/PIN toggle in Settings; default **on**
- **Export scope**: all | current month
- **Export format**: CSV (styled `.xls`) | PDF
- **Import**: plain CSV; format spec in UI; template download
- **Database browser**: transactions, categories, budgets, recurring_transactions

### 3.9 Statistics
- Income, Expense, **Wallet**, Balance for selected period
- Three-bar chart

### 3.10 History month summary (unified)
Single calculator pass (`BalanceCalculator.buildMonthFinancialSummary`):
- **Activity this month** — income, expense, saved (period net)
- **Your money now** — matches Add screen Cash/Bank cards
- **Cash & bank change this month**
- **Wallet this month** (salary mode)

## 4. Database and Migration State

**Current version: 10**

| Migration | Summary |
|-----------|---------|
| 1→2 | Transfer/salary anchor columns, categories |
| 2→3, 3→4 | Salary anchor corrections |
| 4→5 | Reimbursement columns; budget period schema |
| 5→6 | `awaitingReimbursement` |
| 6→7 | All transactions → `BANK` account |
| 7→8 | Additional default expense categories |
| 8→9 | `carriedForwardBalance` |
| 9→10 | `subDescription` |

### Transaction entity (key fields)

```
id, amount, description, subDescription, date, type, categoryId,
accountType, toAccountType, startsNewPeriod, carriedForwardBalance,
allowNegativeBalance, linkedExpenseId, debtorNote, awaitingReimbursement,
createdAt, updatedAt
```

## 5. Runtime Flows

### 5.1 Add / edit income or expense

1. User opens **Add** tab → `AppRoutes.addTransactionRoute()` (query args required for NavHost match)
2. Form: type, amount, description, **subDescription**, category, account, date
3. Optional: owed toggle, dept linking, salary month dialog, carry-forward
4. Save via `AddEditTransactionViewModel.saveTransaction`
5. `emitNavigateHistory()` → History tab

Edit from History → `addTransactionRoute(transactionId = id)`.

### 5.2 Transfer / wallet

- Transfer: `TransferScreen` — does not use Add form
- Wallet: `WalletScreen` — validates max amount, blocks closed periods

### 5.3 History

1. **Overview**: `buildMonthSummary`, reminder, wallet year card
2. **Transactions**: filtered stream → `HistoryGrouping` → swipe delete with confirm
3. **Filters**: period/type/category; drives same transaction query

### 5.4 Export

1. User picks scope + format in Settings
2. `TransactionExportLoader.loadRows(filter)`
3. CSV → `StyledExcelExporter` → `.xls` file
4. PDF → `PdfTransactionExporter` → `.pdf` file
5. Share via `FileProvider` + `ExportShareRequest`

Import template uses `CsvExporter.exportPlainCsv()` → plain `.csv`.

### 5.5 Import

1. User picks file → `CsvImporter.importTransactions`
2. Skip `#` lines; first data line = header
3. Upsert by `id` if exists, else insert
4. Categories matched by name + type

### 5.6 Locale

1. `UserPreferences.setLanguage` → DataStore + SharedPreferences
2. `App.attachBaseContext` → `LocaleHelper.onAttach`
3. Settings language chip → `activity.recreate()`

### 5.7 Biometric lock

1. App starts → `BiometricGate` in `MainActivity`
2. If lock enabled and not unlocked → show `BiometricLockScreen` (no `MainScreen`)
3. `LaunchedEffect` triggers `BiometricPrompt` automatically; **Unlock** button retries
4. Success → `isUnlocked = true` → `MainScreen` visible
5. Failed attempt → error on lock screen (e.g. "Not recognized. Try again.")
6. Error/cancel → error message, data remains hidden
7. `ON_STOP` → `lock()` → next resume requires auth again

## 6. Navigation Reference

```kotlin
// AppRoutes.kt
HISTORY = "history"
ADD_TRANSACTION = "addTransaction"  // use addTransactionRoute() for navigation
STATISTICS, CATEGORIES, SETTINGS, DATABASE_BROWSER
TRANSFER, WALLET
EDIT_TRANSFER = "editTransfer/{transactionId}"
EDIT_WALLET = "editWallet/{transactionId}"
```

**Important**: Bottom nav Add must call `addTransactionRoute()`, not bare `addTransaction`, because NavHost registers `ADD_TRANSACTION_WITH_ARGS`.

## 7. CSV Import Format (Canonical)

Header (single line):

```
id,date,type,category,amount,account,toAccount,description,subDescription,startsNewPeriod,carriedForwardBalance,linkedExpenseId,debtorNote,awaitingReimbursement,allowNegativeBalance,createdAt,updatedAt
```

- **Required for import**: `date`, `type`, `amount`, `account`, `description`
- **Date format**: `yyyy-MM-dd HH:mm:ss` (US locale)
- **Type**: `INCOME` | `EXPENSE` | `TRANSFER` | `WALLET_MOVE`
- **Account**: `CASH` | `BANK` | `WALLET`
- Lines starting with `#` ignored (export comment header)

Defined in `domain/TransactionExportRow.kt` → `CsvImportFormat` object.

## 8. File-Level Source of Truth

| Area | Files |
|------|-------|
| Navigation | `MainScreen.kt`, `AppRoutes.kt` |
| History | `HistoryScreen.kt`, `HistoryViewModel.kt`, `TransactionItem.kt`, `SwipeableTransactionItem` |
| Add/Edit | `AddTransactionScreen.kt`, `AddEditTransactionViewModel.kt` |
| Transfer | `TransferScreen.kt`, `TransferViewModel.kt` |
| Wallet | `WalletScreen.kt`, `WalletViewModel.kt`, `WalletCalculator.kt` |
| Statistics | `StatisticsScreen.kt`, `StatisticsViewModel.kt`, `StatisticsBarChart.kt` |
| Settings | `SettingsScreen.kt`, `SettingsViewModel.kt` |
| Export/Import | `CsvExporter.kt`, `CsvImporter.kt`, `StyledExcelExporter.kt`, `PdfTransactionExporter.kt`, `TransactionExportLoader.kt` |
| DB browser | `DatabaseBrowserScreen.kt`, `DatabaseBrowserViewModel.kt`, `DatabaseInspector.kt` |
| Locale | `LocaleHelper.kt`, `UserPreferences.kt`, `values/strings.xml`, `values-ar/strings.xml` |
| Biometric lock | `BiometricAuthManager.kt`, `BiometricGate.kt`, `BiometricLockScreen.kt`, `BiometricLockViewModel.kt`, `MainActivity.kt` |
| Period/Balance | `PeriodCalculator.kt`, `BalanceCalculator.kt`, `core/time/DateUtils.kt` |
| Data | `entities/*`, `dao/*`, `Migrations.kt`, `AppDatabase.kt` |

## 9. Problems Encountered and Resolutions (Selected)

| Issue | Fix |
|-------|-----|
| History too cluttered | Tabbed Overview / Transactions / Filters |
| Add tab blank/wrong route | Navigate with `addTransactionRoute()` |
| Balance confusion (Add vs History) | Unified `buildMonthFinancialSummary` |
| Wallet negative balance | Validation + max allowed UI |
| Closed month wallet edits | `WalletCalculator.isPeriodClosedForDate` guards |
| CSV re-import with export comments | Importer skips `#` lines |
| Styled vs importable CSV | Separate plain template export + documented format |
| Sensitive data visible without auth | `BiometricGate` blocks `MainScreen` until prompt succeeds |

## 10. Business Glossary

- **Salary month**: spending period anchored by salary start event
- **Wallet**: monthly pocket-money bucket (salary-mode); not the same as “all cash”
- **Dept income**: reimbursement income from others
- **Owed expense**: `awaitingReimbursement = true`
- **Sub-description**: optional item-level detail under main description
- **Activity this month**: income/expense/saved within period bounds
- **Your money now**: current Cash + Bank available (matches Add screen)
- **Carry-forward**: previous period net added when starting new salary month

## 11. Remaining Opportunities

1. Localize Transfer, Wallet, Categories screens (partial i18n today)
2. Persistent category system key column (vs display-name matching)
3. Automated tests for migrations 7–10 and export/import round-trip
4. Split large composables (HistoryScreen) further
5. Optional true `.xlsx` export via library if needed

---

**Maintenance rule**: When adding features, update this guide and `RELEASE_NOTES.md` first, then `QA_CHECKLIST.md` and `Readme.md`.
