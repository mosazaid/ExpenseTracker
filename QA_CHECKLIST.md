# QA Checklist

## Build and Startup
- [ ] Clean build succeeds (`assembleDebug`).
- [ ] App launches without migration crash on existing install (DB v6 → v10 path).
- [ ] Fresh install creates default categories correctly.
- [ ] Biometric prompt appears on first launch (when lock enabled).
- [ ] Wrong fingerprint / cancel shows error; no transaction data visible.
- [ ] Successful unlock shows History / main app.
- [ ] App re-locks after going to background and returning.
- [ ] Settings toggle disables lock (no prompt on next launch).

## Navigation
- [ ] Bottom **Add** tab opens Add Transaction screen.
- [ ] After save, navigates to History.
- [ ] History row tap opens edit on Add screen (income/expense).
- [ ] Transfer / Wallet buttons on Add screen navigate correctly.
- [ ] Settings and Database browser open from History / Settings and back works.

## History and Period Logic
- [ ] Month mode toggle works (Calendar ↔ Salary).
- [ ] Salary month period label matches expected range.
- [ ] **Overview** tab: month summary, salary reminder, wallet year card (salary mode).
- [ ] **Transactions** tab: grouped list, period label, swipe-left delete with confirm dialog.
- [ ] **Filters** tab: period chips, type filter, category dropdown with budget progress.
- [ ] Month summary shows activity, money now, cash/bank change, wallet (salary mode).

## Add/Edit Transactions
- [ ] Add income and expense with validation.
- [ ] Optional **sub-description** saves and displays under main description in history.
- [ ] Tap history item opens edit with prefilled data (including sub-description).
- [ ] Edit saves and updates existing row (not duplicate insert).
- [ ] Delete flow works from history (button and swipe).

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
- [ ] All four tables listed: transactions, categories, budgets, recurring_transactions.
- [ ] Column types shown via table info.
- [ ] Per-column filter returns matching rows (max 500).

## Statistics
- [ ] Income, Expense, Wallet, and Balance shown for Day/Week/Month/Year.
- [ ] Bar chart shows three bars: Income (green), Expense (red), Wallet (purple).

## Regression Checks
- [ ] Categories CRUD still works for income/expense.
- [ ] Existing historical data remains readable after migration.
- [ ] Account badges (Cash/Bank) visible on history rows.
