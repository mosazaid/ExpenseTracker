# QA Checklist

## Build and Startup
- [ ] Clean build succeeds (`assembleDebug`).
- [ ] App launches without migration crash on existing install.
- [ ] Fresh install creates default categories correctly.

## History and Period Logic
- [ ] Month mode toggle works (Calendar <-> Salary).
- [ ] Salary month period label matches expected range.
- [ ] Week grouping in month view is shown correctly.
- [ ] History list remains scrollable with all controls.

## Add/Edit Transactions
- [ ] Add income and expense works with validation.
- [ ] Tap history item opens edit screen with prefilled data.
- [ ] Edit saves and updates existing row (not duplicate insert).
- [ ] Delete flow still works from history.

## Owed / Dept Flow
- [ ] Expense is only owed when `Someone owes me` is enabled.
- [ ] Owed filter shows only owed/unreimbursed expenses.
- [ ] Dept income can link to original owed expense.
- [ ] Linked expense displays reimbursed state afterwards.

## Transfer Flow
- [ ] Add transfer Cash -> Bank and Bank -> Cash.
- [ ] Edit transfer works and persists correctly.
- [ ] Insufficient funds dialog appears and allow-negative path works.

## Budget Flow
- [ ] Set budget from Categories screen.
- [ ] Budget progress appears in History category dropdown.
- [ ] Over-budget and near-limit coloring behaves correctly.

## Recurring Reminder Flow
- [ ] Salary reminder can be created from salary save dialog.
- [ ] History reminder banner appears when due.
- [ ] Dismiss reminder hides banner until next cycle.
- [ ] Recording from reminder path advances next due date.

## Settings and Export
- [ ] Opening cash/bank balances save and reflect in totals.
- [ ] CSV export triggers share intent and file opens.

## Regression Checks
- [ ] Statistics screen loads correctly for all periods.
- [ ] Categories CRUD still works for income/expense.
- [ ] Existing historical data remains readable after migration.
