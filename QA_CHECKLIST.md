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

## Navigation (Type-Safe Routes)
- [ ] Bottom tabs (`Overview`, `History`, `Statistics`, `More`) switch instantly with state restoration and single-top launch.
- [ ] Floating Action Button (+) navigates to `AddTransaction()` type-safe destination with default parameters.
- [ ] Edit actions pass typed parameters: `AddTransaction(transactionId)`, `EditTransfer(transactionId)`, `EditWallet(transactionId)` correctly load target records.
- [ ] Push screens (`Loans`, `Alerts`, `Debts`, `Transfer`, `Wallet`, `Categories`, `Settings`, `DatabaseBrowser`, `Onboarding`) open smoothly and back navigation pops correctly.

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

## Daily Reminder & Notification Flow
- [ ] Daily 9 PM reminder registered via `AlarmScheduler` on startup and after boot.
- [ ] With `SCHEDULE_EXACT_ALARM` granted: Notification triggers at 9:00 PM.
- [ ] Without exact alarm permission (fallback): Notification still triggers during next OS maintenance window; no crashes.
- [ ] If user already logged an expense today: 9 PM check runs and suppresses notification.
- [ ] If user has 0 expenses today: Notification is posted asking the user to log expenses.
- [ ] `POST_NOTIFICATIONS` permission prompt displays on Android 13+ devices.
- [ ] WorkManager failsafe (`DailyExpenseReminderWorker`) scheduled properly.

## Budget Alerts & Notification Center
- [ ] Adding an expense that brings category to >=85% triggers warning banner and persistent `AppAlert`.
- [ ] Adding an expense that exceeds 100% of budget triggers critical alert and system tray notification.
- [ ] `AppTopBar` bell icon shows live badge count matching unread `app_alerts`.
- [ ] Tapping bell icon navigates to `AlertsScreen`.
- [ ] Dismissing an alert sets `isDismissed = true` and decrements badge count.

## Debt Tracking & Directional Separation
- [ ] Adding Expense with category "Dept": Form labels as "Money Lent" (debtor owes user).
- [ ] Adding Income with category "Dept": Form labels as "Repayment" / "Money Borrowed".
- [ ] `DebtsScreen` accessible from More tab: shows list of active debts grouped by debtor.
- [ ] "Settle Debt" button marks debt as settled (`isDebtSettled = true`).
- [ ] Month rollover prompts to carry over unsettled debts to new month.

## Configured Loans & Salary Preservation
- [ ] `LoansScreen` accessible from More tab: allows creating recurring loans (name, default amount, account, and calculate in expenses toggle).
- [ ] "Calculate as monthly expense" toggle defaults to OFF (`deductFromIncome = true`).
- [ ] First app launch of a new month shows `LoanReminderBottomSheet` once per day.
- [ ] Recording a loan payment creates a transaction in history, while deducting directly from monthly income and excluding it from monthly expense totals and statistics.
- [ ] Paid loans show in `OverviewScreen` inside dedicated `PaidLoanCard` regardless of whether deducted directly from income or calculated as monthly expense.
- [ ] Overview screen reactively refreshes on resume when returning from adding transactions, paying loans, or adding salary.
- [ ] Dismissing loan reminder for the month dismisses the sheet until next month.
- [ ] History icon on each recurring item opens `RecurringHistoryBottomSheet` displaying chronological past transactions with date, exact time, and amount.
- [ ] History icon on each loan opens `LoanHistoryBottomSheet` displaying monthly payment history with status (paid date & time vs pending) and amount.

## More Screen Hub & Settings
- [ ] 4th bottom nav tab opens `MoreScreen` with navigation tiles for Loans, Debts, Categories, Database Browser, and Settings.
- [ ] Settings allows switching between 8 palettes (Emerald Green, Blue & Gold Luxury, Warm Earth, Cool Slate & Teal, Amethyst & Violet, Crimson & Burgundy, Midnight Ocean, Forest & Sage Mint).
- [ ] Time picker in `AddTransactionScreen` allows specifying custom hour/minute.

## Regression Checks
- [ ] Categories and Subcategories CRUD still works.
- [ ] Existing historical data remains readable after v14 → v15 migration.
- [ ] Currency numbers formatted with 3 decimal places across all screens.
- [ ] Database browser includes `configured_loans`, `monthly_loan_payments`, and `app_alerts`.

