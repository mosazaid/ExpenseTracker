# QA Checklist

## Build and Startup
- [ ] Clean build succeeds (`assembleDebug`).
- [ ] Unit tests pass cleanly (`./gradlew testDebugUnitTest`).
- [ ] App launches without migration crash on existing install (DB v6 → v11 path).
- [ ] Fresh install creates default categories and subcategories correctly.
- [ ] Biometric prompt appears on first launch (when lock enabled).
- [ ] Wrong fingerprint / cancel shows error; no transaction data visible.
- [ ] Successful unlock shows History / main app.
- [ ] App re-locks after going to background and returning.
- [ ] Settings toggle disables lock (no prompt on next launch).

## Navigation
- [ ] Bottom **Add** tab opens Add Transaction screen (Income/Expense switcher only).
- [ ] After save, navigates to History.
- [ ] History row tap opens edit on Add screen (income/expense).
- [ ] Transfer / Wallet buttons on Overview navigate correctly.
- [ ] Settings and Database browser open from History / Settings and back works.

## History and Period Logic
- [ ] Month mode toggle works (Calendar ↔ Salary).
- [ ] Salary month period label matches expected range.
- [ ] **Overview** tab:
  - [ ] Balances displayed centered with currency under amount.
  - [ ] **Remaining Breakdown**: Clarifies **Remaining This Month** vs **Remaining Carried Over from Last Month** totaling **Total Remaining (Cash + Bank)**.
  - [ ] Month summary, salary reminder, wallet year card (salary mode).
- [ ] **Transactions** tab:
  - [ ] Grouped list by day/week/month/year.
  - [ ] **Item Layout**: Row 1 (description + Cash/Bank badge), Row 2 (category in primary/Medium + dot • + date in outline), Row 3 (subcategory + formatted amount at right).
  - [ ] Trash icon hidden; swipe-left delete works with confirm dialog.
- [ ] **Filters** tab:
  - [ ] Period chips, type filter, category dropdown with budget progress.
  - [ ] Category dropdowns sorted by usage frequency.
  - [ ] Subcategory filter: shows subcategories for selected category plus **"Other"** option with accurate spend total.
  - [ ] Categories without subcategories hide the subcategory dropdown.

## Add/Edit Transactions
- [ ] Add income and expense with validation (types restricted to Income & Expense).
- [ ] Amount field formatted to 3 decimals (`#,##0.000 JOD`) with thousand grouping.
- [ ] Form fields enforce `singleLine = true`, decimal keyboard on amount with input sanitization, and `Next`/`Done` IME actions.
- [ ] Optional **sub-description** saves and displays.
- [ ] Tap history item opens edit with prefilled data (including sub-description & subcategory).
- [ ] Edit saves and updates existing row (not duplicate insert).
- [ ] Delete flow works from swipe-to-delete.

## Wallet Flow
- [ ] Wallet ↔ Cash/Bank move in open salary month saves correctly.
- [ ] Wallet balance cannot go negative (max shown, save disabled when exceeded).
- [ ] Closed salary month wallet moves are read-only / delete blocked with message.
- [ ] Wallet filter and wallet section in month summary show correct values.

## Owed / Dept Flow
- [ ] Expense is only owed when `Someone owes me` is enabled.
- [ ] Owed filter shows only owed/unreimbursed expenses.
- [ ] Dept income can link to original owed expense.
- [ ] Linked expense displays reimbursed state afterwards.

## Transfer Flow
- [ ] Add transfer Cash → Bank and Bank → Cash.
- [ ] Edit transfer works and persists correctly.
- [ ] Insufficient funds dialog appears and allow-negative path works.

## Budget Flow
- [ ] Set budget from Categories screen.
- [ ] Budget progress appears in History → Filters → category dropdown.
- [ ] Over-budget and near-limit coloring behaves correctly.

## Recurring Reminder Flow
- [ ] Salary reminder can be created from salary save dialog.
- [ ] History reminder banner appears when due (Overview tab).
- [ ] Dismiss reminder hides banner until next cycle.
- [ ] Recording from reminder path advances next due date.

## Salary Carry-Forward
- [ ] “Start new month?” dialog offers carry-forward when previous period had savings.
- [ ] History month summary shows “Brought forward” and “Remaining total” when applicable.

## Settings, Language, Export, Import
- [ ] **App lock** toggle enables/disables biometric gate.
- [ ] Opening cash/bank balances save and reflect in totals.
- [ ] Language toggle (English / Arabic) recreates app with correct strings and RTL.
- [ ] Export scope: All vs Current month filters rows correctly.
- [ ] Export **CSV** (.xls styled): icon, title, colored headers/values; opens in Excel/Sheets.
- [ ] Export **PDF**: icon, title, colored table; share intent works.
- [ ] **Download import template** produces plain `.csv` with header + data.
- [ ] Import CSV: valid file inserts/updates; `#` comment lines ignored.
- [ ] Import format card shows header, required/optional columns, example row.

## Database Browser
- [ ] All Room tables listed: `transactions`, `categories`, `sub_categories`, `budgets`, `recurring_transactions`.
- [ ] Column types shown via table info.
- [ ] Per-column filter returns matching rows (max 500).

## Statistics
- [ ] Income, Expense, Wallet, and Balance shown for Day/Week/Month/Year.
- [ ] **3-Month Multi-Chart**: Switch between Bar, Spline Trend Line, and Combined views; horizontal scrolling chip selector.
- [ ] **3-Month Category Pie Chart**: Interactive donut slices, center total/category detail, month filter chips with horizontal scrolling.

## Regression Checks
- [ ] Categories and Subcategories CRUD still works.
- [ ] Existing historical data remains readable after v10 → v11 migration.
- [ ] Currency numbers formatted with 3 decimal places across all screens.

