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
- [ ] Settings allows switching between 12 curated palettes (Emerald Green, Blue & Gold Luxury, Warm Earth, Cool Slate & Teal, Amethyst & Violet, Crimson & Burgundy, Midnight Ocean, Forest & Sage Mint, Rose Gold & Champagne, Obsidian & Amber Gold, Nordic Frost & Glacier, Espresso & Warm Mocha) with full WCAG contrast and visible text on selected chips.
- [ ] Time picker in `AddTransactionScreen` allows specifying custom hour/minute.

## Multi-Split Transactions & Receipts
- [ ] In `AddTransactionScreen`, enabling "Split Transaction into Items" allows adding multiple line items.
- [ ] In `AddTransactionScreen`, the top single-category subcategory dropdown is hidden when Split Mode is active.
- [ ] In split items list, subcategory chips wrap on multiple lines (`FlowRow`) with selection checkmarks and no horizontal clipping.
- [ ] Each split line item tracks subcategory, amount, note, and optional debtor name (`LENT`).
- [ ] Real-time remaining balance recalculates dynamically and "Auto-Fill Balance" button works.
- [ ] Tapping "Attach Receipt" presents bottom sheet choice: "Take Photo" (Camera) vs "Choose from Gallery" (Gallery).
- [ ] Camera launch checks runtime `CAMERA` permission; shows `PermissionRationaleDialog` if rationale needed or directs to App Settings if permanently denied.
- [ ] Photos taken in vertical/portrait orientation save upright (EXIF orientation properly respected, avoiding horizontal sideways orientation).
- [ ] Receipt file is securely saved to private app storage (`files/receipts/`).
- [ ] Tapping a transaction in `HistoryScreen` opens `TransactionDetailBottomSheet`.
- [ ] `TransactionDetailBottomSheet` displays all split line items, debtor details, and receipt thumbnail.
- [ ] Tapping receipt thumbnail in `TransactionDetailBottomSheet` or `ReceiptAttachmentSection` opens `ZoomableImageDialog`.
- [ ] `ZoomableImageDialog` supports finger pinch-to-zoom (1x to 5x), viewport-bounded panning, double-tap zoom toggle, zoom % readout with tabular numbers, and manual 90° rotation button.
- [ ] Centralized `PermissionHelper` handles Camera, Location (Settings & Prayer/Qiblah), Notifications, and Alarms consistently.

## AI Spending Pacing & Financial Insights
- [ ] `FinancialInsightsCard` on `OverviewScreen` displays day progress, budget consumed, velocity status, and safe daily spend.
- [ ] Early in the cycle (days 1–5), large upfront payments (rent, bills) engage early-cycle damping; safe daily allowance adapts without absurd linear multipliers.
- [ ] Tapping info (`i`) icon opens `FinancialInsightsInfoDialog`.
- [ ] Info dialog clearly explains Financial Cycle, Spending Velocity, Safe Daily Spend, and Projected Surplus/Deficit.
- [ ] `FinancialHealthScoreCard` shows 0–100 score, tier badge, and expandable financial advice.

## Offline Islamic Finance & Location Intelligence
- [ ] `ZakahCalculatorScreen` calculates 2.5% Zakah obligation with live gold Nisab comparison (85 grams of gold).
- [ ] `PrayerQiblahScreen` calculates 5 daily prayer times purely offline with next prayer countdown.
- [ ] Qiblah compass points accurately to the Kaaba with bearing degrees.
- [ ] Auto-detect location prompts for `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` runtime permissions.
- [ ] Location successfully resolves city name and auto-selects local currency (e.g. Jordan → JOD, Saudi Arabia → SAR, UAE → AED, Egypt → EGP, Turkey → TRY).

## Category Budgets & Categories Screen
- [ ] Categories screen features Segmented tabs: **Categories** vs **Monthly Budgets**.
- [ ] Categories in both tabs are sorted from most used to least used.
- [ ] Setting a category budget persists across salary/calendar month rollovers without disappearing.
- [ ] `CategoryBudgetsCard` on `OverviewScreen` remains permanently visible with progress bars and quick CTA.

## Notification Settings Screen
- [ ] Dedicated `NotificationSettingsScreen` accessible from Settings and More tab.
- [ ] Toggles work for Loan Reminders, Debt Reminders, Salary & Recurring, Budget Warnings, and Daily 9 PM Reminder.
- [ ] "Send Test Notification" dispatches test notification successfully.

## Phase 9 Enhancements & Bug Fixes
- [ ] **Split Subcategory Spend Attribution & History Display**:
  - [ ] Add a split transaction (e.g. Category: Shopping, Split 1: Grocery 5.00 JOD, Split 2: Sweets 7.00 JOD).
  - [ ] In `HistoryScreen`, verify the transaction displays `Grocery 5.000 JOD • Sweets 7.000 JOD` badges instead of a generic subcategory.
  - [ ] In Category Filter / Breakdown, verify 5.00 JOD is counted under Grocery and 7.00 JOD is counted under Sweets (not under "Others").
- [ ] **Financial Insights & Salary Cycle Verification**:
  - [ ] Configure total balance 540 JOD and wallet 1160 JOD; verify wallet funds are treated as reserved savings and NOT added to total expenses in `FinancialInsightsCard`.
  - [ ] Verify safe daily spend matches `(Remaining Budget) / (Days Left in Cycle)`.
  - [ ] Receive a salary 8 days ago in salary cycle mode; verify the cycle period does not show "1 day remaining", correctly computing days remaining to the next monthly cycle anchor.
- [ ] **Statistics Screen Subcategory Comparison**:
  - [ ] Open `StatisticsScreen` and locate `SubcategoryComparisonCard` below the category pie chart.
  - [ ] Select a category from the dropdown (e.g. Shopping).
  - [ ] Verify "This Period" section displays subcategories with exact spent amounts, tabular figures, and proportional percentage progress bars.
  - [ ] Verify "3-Month Trend" section displays the current month, previous month, and 2 months ago in a clean tabular view.
  - [ ] Verify Month-over-Month (MoM) delta percentages (+/- %) and color badges display correctly.
- [ ] **Real-Time Sensor-Driven Qiblah Compass**:
  - [ ] Open `PrayerQiblahScreen` and rotate the phone physically.
  - [ ] Verify the compass dial rotates smoothly in real-time matching the device's physical heading.
  - [ ] Verify True North (Red line) remains pointing towards real North.
  - [ ] Turn phone toward the Kaaba needle direction.
  - [ ] When aligned within ±3.5°, verify:
    - [ ] Outer ring illuminates with emerald green glow (`#10B981`).
    - [ ] Status banner displays "Facing Al-Kaaba (Qiblah)!".
    - [ ] Tactile haptic vibration triggers upon entering alignment.
  - [ ] When unaligned, verify turn guidance arrow and degree text (e.g. "Turn phone right by 24°").
  - [ ] Holding phone tilted vertically displays "Hold phone flat for highest precision" warning.
- [ ] **Prayer Calculation Methods**:
  - [ ] Verify active calculation method card displays method name and sun angles.
  - [ ] Tap "Change Method" and select from 12 global calculation methods (Jordan, Umm al-Qura, Egypt, Turkey, Karachi, ISNA, MWL, Dubai, Kuwait, Qatar, Singapore, Tehran).
  - [ ] Verify prayer times recalculate immediately upon selecting a different method.
  - [ ] Select "Auto-detect by Location" and verify it maps country code to local authority (e.g. JO -> Jordan, SA -> Umm al-Qura).
- [ ] **Salah Reminders & Overview GPS Sync**:
  - [ ] Tap bell toggle icons for Fajr, Dhuhr, Asr, Maghrib, and Isha in `PrayerQiblahScreen` to enable/disable specific reminders.
  - [ ] Open `OverviewScreen` and tap the GPS Location Sync icon button in the top bar.
  - [ ] Verify runtime location permission prompt is triggered (if not granted) or location is refreshed with a confirmation snackbar.

## Complete App Backup & Device Migration (JSON)
- [ ] Go to **Settings -> Data Management -> Complete App Backup (Transfer to Another Device)**.
- [ ] Tap **Export All Data & Settings (JSON)**:
  - [ ] System file creator prompts for save location with default name `expense_tracker_full_backup_*.json`.
  - [ ] File contains valid JSON with `version`, `database` (all 9 tables), and `preferences` (all DataStore keys).
- [ ] Tap **Restore Complete Backup (JSON)**:
  - [ ] Confirmation warning dialog appears informing user that existing data will be replaced.
  - [ ] Selecting a backup JSON file restores transactions, splits, categories, subcategories, budgets, loans, alerts, and settings.
  - [ ] Snackbar/toast confirms exact restored counts (e.g. `Restored: X transactions, Y categories, Z preferences`).
  - [ ] All settings (currency, language, theme, 12h/24h, opening balances) update immediately.

## Debts & Loans Partial Payments
- [ ] Open `DebtsScreen` and select a debt to pay.
- [ ] Verify partial payment option allows specifying amount less than total.
- [ ] Progress reflects partial payment (e.g. paid $50 of $100).
- [ ] Transaction history records corresponding payment item with proper transaction type and debtor reference.
- [ ] Paying full balance removes debt reminder alert from notification list.

## Regression Checks
- [ ] Categories and Subcategories CRUD still works.
- [ ] Existing historical data remains readable after v16 → v17 migration.
- [ ] Currency numbers formatted with proper decimal places across all screens.
- [ ] Database browser includes `transaction_splits`, `configured_loans`, `monthly_loan_payments`, and `app_alerts`.


