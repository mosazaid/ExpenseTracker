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
- [ ] App lock is off on a fresh install.
- [ ] With no fingerprint and no screen lock on the phone, turning App lock on leaves it off and shows why, in the app language.
- [ ] With App lock on, the Backup, Restore and Import file pickers still work after the app asks to unlock again.

## Navigation (Type-Safe Routes)
- [ ] Bottom tabs (`Overview`, `History`, `Statistics`, `More`) switch instantly with state restoration and single-top launch.
- [ ] Floating Action Button (+) navigates to `AddTransaction()` type-safe destination with default parameters.
- [ ] Edit actions pass typed parameters: `AddTransaction(transactionId)`, `EditTransfer(transactionId)`, `EditWallet(transactionId)` correctly load target records.
- [ ] Push screens (`Loans`, `Alerts`, `Debts`, `Transfer`, `Wallet`, `Categories`, `Settings`, `DatabaseBrowser`, `Onboarding`) open smoothly and back navigation pops correctly.
- [ ] Settings pages (`AppearanceSettings`, `LanguageRegionSettings`, `AccountBalancesSettings`, `MonthCalculationSettings`, `BackupSettings`, `ImportExportSettings`) open from the Settings list, and Back returns to the list.

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
  - [ ] **Year Period Month Filter**:
    - [ ] Selecting Year period reveals Month filter dropdown ("All Months" default).
    - [ ] Selecting a specific month (e.g. October 2026) scopes category totals and budget progress to that month.
    - [ ] Subcategory dropdown spend amounts match the selected month only.
    - [ ] Transactions list renders only transactions belonging to the selected month.
    - [ ] Clear 'X' button or selecting "All Months" restores full annual view.
    - [ ] Works seamlessly across both Calendar Mode (12 calendar months) and Salary Mode (salary periods).

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
- [ ] Recording from reminder path advances next due date.
- [ ] Each recurring item has "Add transaction automatically", off by default.

### Due recurring items (sheet on opening the app)
- [ ] Opening the app with due items shows the "Due recurring items" sheet, after the app lock if it is on.
- [ ] It lists every due reminder-only item, and automatic items only when the account cannot cover the oldest due period (marked as not added). Oldest due is first.
- [ ] When the loan payment sheet is also due, it comes first. The two sheets are never stacked.
- [ ] **Add** opens the prefilled form. Leaving without saving keeps the item due. Saving records one transaction and advances the item one period.
- [ ] **Skip this time** asks for confirmation, advances one period and adds no transaction. Cancel changes nothing.
- [ ] **Later** closes the sheet. Moving between tabs, a short switch to another app and unlocking again do not bring it back.
- [ ] It comes back after a fresh start, and after at least 30 minutes in the background.
- [ ] Opening from a recurring phone notification goes to that item's form without the sheet on top of it.
- [ ] Several missed periods of a reminder-only item are handled one at a time.
- [ ] A form opened for a period that was already recorded or skipped (from the due sheet, a notification or the Recurring page) saves nothing and says so, pointing to History.
- [ ] The Overview banner shows the number of due items **at the top of the page without scrolling**, and **Review** reopens the sheet. When the banner appears while the page is at the top, the page scrolls to show it; a user who has scrolled further down is not moved.
- [ ] The due sheet appears whether the "Salary & Recurring Phone Notifications" switch is on or off.

## Salary Carry-Forward
- [ ] “Start new month?” dialog offers carry-forward when previous period had savings.
- [ ] History month summary shows “Brought forward” and “Remaining total” when applicable.

## Settings List and Pages
- [ ] Groups and rows, in order: General (Appearance, Language and region), Money (Accounts and balances, Month and calculations), Alerts (Notifications), Data and security (App lock switch, Backup and restore, Import and export). The version is at the bottom.
- [ ] Row summaries are correct: mode and color theme; language and currency; salary or calendar month; last backup date or "No backup yet".
- [ ] **Appearance**: System / Light / Dark and the color themes apply.
- [ ] **Language and region**: language, currency, GPS currency detection (including permission denial), 12 / 24 hour time.
- [ ] Changing the language applies it, mirrors the layout for Arabic, stays on the same page, and still applies after a restart.
- [ ] After switching language, dates and month names use the new language without a restart (including the Recurring due date and History group titles).
- [ ] A user who never picked a currency: check the currency shown on Settings, Overview, Recurring and the due sheet after a fresh start. (Known on 2026-11-27: they can differ, and the Settings value follows the app language.)
- [ ] **Month and calculations**: salary / calendar month changes the same setting as the switch in History; "Calculation Rules" opens; the wallet-vault transfer switch saves.

### Correct balance (Accounts and balances)
- [ ] Cash and Bank each show the current balance as money, with their own **Correct balance** button.
- [ ] The dialog starts empty and shows current balance, new balance and difference before saving.
- [ ] Blank input cannot be saved. Input with a comma, Arabic comma, minus sign or letters is rejected with a message; nothing is silently removed (`12,5` must not become `125`).
- [ ] Arabic-Indic digits and the Arabic decimal separator (`٫`) are accepted. Spaces at the ends are ignored.
- [ ] Confirm is disabled when the new amount equals the old one at the decimals shown.
- [ ] Saving changes only the chosen account and adds no transaction.
- [ ] The preview stays visible above the keyboard on a small phone.

### Import and export
- [ ] Export scope: All vs Current salary/calendar month filters rows correctly.
- [ ] **CSV**: plain file that can be re-imported.
- [ ] **Excel (styled)** (`.html` file): icon, title, colored headers/values.
- [ ] **PDF**: icon, title, colored table; share intent works.
- [ ] **Download import template** (inside "Accepted CSV import format") contains only comment lines, the header, and one example row starting with `#`. No real transactions.
- [ ] Importing the unchanged template reports 0 added.
- [ ] Import CSV: valid file inserts/updates; `#` comment lines ignored.
- [ ] A failed export or template shows a translated message; no crash.

### Version and database viewer
- [ ] More and Settings show the same version (`BuildConfig.VERSION_NAME`, currently 1.0).
- [ ] Debug build: the Database browser row is always at the bottom of Settings.
- [ ] Signed release build: the row is hidden; 7 taps on the version show it and 7 more hide it, each with a message. The state survives a restart and is not in the backup file.

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
- [ ] 4th bottom nav tab opens `MoreScreen`. Every item opens its screen and Back returns:
  - [ ] Everyday money: Recurring, Budgets and categories, Loans, Debts, Savings vault (vault still asks for authentication on its own screen).
  - [ ] Plan and travel: Trips, Currency exchange, Price memory, Emergency runway.
  - [ ] Islamic: Zakah, Sadaqah jar, Prayer and Qibla, Murabaha.
  - [ ] App (rows): Settings, Help and guide.
- [ ] There is no top card, Pro banner, Notifications entry or Database browser entry. The footer shows the offline note and the version.
- [ ] Three tiles per row; two when a tile would be narrower than 100 dp or the font scale is 1.3 or more. No name is cut off (English, Arabic, large text, 320 dp).
- [ ] A screen reader reads each tile, row and switch by name.
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
- [ ] Dedicated `NotificationSettingsScreen` accessible from Settings > Notifications (not from More).
- [ ] Toggles work for Loan Reminders, Salary & Recurring Phone Notifications, Budget Warnings, and Daily 9 PM Reminder. There is no debt reminder switch.
- [ ] Budget Warnings off: no phone notification, but the entry still appears in the in-app alerts inbox.
- [ ] Salary & Recurring off: no recurring reminder notification. Notices about automatic entries (added, or blocked for low balance) still appear.
- [ ] The pinned-notification option mentions loans only ("Sticky Loan Reminders").

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
  - [ ] Verify GPS detected location inherits accurate elevation above sea level (e.g. Amman ~1000m) and does not default to sea-level (0m), ensuring Maghrib matches within 0–1 minute of the official Ministry of Awqaf timetable.

## Complete App Backup & Device Migration (JSON)
- [ ] Go to **Settings -> Backup and restore**. The page warns that receipt photos are not included.
- [ ] Tap **Back up now**:
  - [ ] An "Export ready" dialog offers View, Share file and Save to Downloads, for `mizani_full_backup_*.json`.
  - [ ] File contains valid JSON with `version` (2), `databaseVersion`, the table lists, and `preferences`.
  - [ ] `preferences` does not contain `last_backup_created_at` or `database_viewer_unlocked`.
  - [ ] "Last backup created" updates after a successful save or when the share window opens. Cancel, report export and template export do not change it.
- [ ] While a backup or restore runs, both buttons are disabled and progress is shown. Leaving the page does not stop a restore.
- [ ] Tap **Restore Complete Backup** (test data only):
  - [ ] After choosing a file, a confirmation dialog warns that existing data will be replaced.
  - [ ] A file that is not a Mizani backup (other JSON, CSV) is rejected and current data stays.
  - [ ] Selecting a backup file restores records and settings, and a dialog confirms the restored counts.
  - [ ] If settings cannot be imported after the records were restored, the message says so instead of reporting total failure.
  - [ ] On a phone with no fingerprint or screen lock, a backup with App lock on leaves the lock unchanged and says so. A backup with App lock off that turns it off says so.
  - [ ] Restoring does not change "Last backup created".
  - [ ] All settings in the backup (currency, language, theme, 12h/24h, opening balances) update immediately.
  - [ ] A Cash or Bank opening balance or Sadaqah jar balance that was never set when the backup was made returns to 0 after the restore. Other settings the backup does not contain keep their current value. A settings block not written by the app (no "onboarding_completed" key, for example `{}`) never resets a balance.

## Debts & Loans Partial Payments
- [ ] Open `DebtsScreen` and select a debt to pay.
- [ ] Verify partial payment option allows specifying amount less than total.
- [ ] Progress reflects partial payment (e.g. paid $50 of $100).
- [ ] Transaction history records corresponding payment item with proper transaction type and debtor reference.
- [ ] Paying full balance removes debt reminder alert from notification list.

## Category & Subcategory Comparison Overhaul (Version 2.0)
- [ ] Open `StatisticsScreen` and scroll to `SubcategoryComparisonCard`.
- [ ] Base period vs Comparison period pickers allow selecting any two months (e.g. Current vs Previous Month).
- [ ] Toggle **Day-Normalized Pacing**:
  - [ ] If today is Day 15, verify Day 1–15 of Month A is compared against Day 1–15 of Month B.
  - [ ] Progress bars and delta percentages reflect accurate day-pace spend.
- [ ] **Waterfall Accordion Tree**:
  - [ ] Tap parent category row: expands to show all child subcategories with spend bars and +/- % badges.
  - [ ] Tap any subcategory row: opens `SubcategoryDrillDownBottomSheet` displaying filtered transactions.
  - [ ] In drill-down sheet, tap a receipt thumbnail to open high-resolution `ZoomableImageDialog`.
- [ ] Categories jumping >30% display smart insight diagnosis chips.

## Android Home Screen Glance App Widgets
- [ ] Add `PaydayPacingWidget` (4×2) to Android home screen:
  - [ ] Shows current liquid balance (`Cash + Bank`), days until next payday, and safe daily burn rate.
  - [ ] Tap anywhere on the widget to open `MainActivity`.
- [ ] Add `QuickAddWidget` (2×1) to home screen:
  - [ ] Shows today's spend total.
  - [ ] Tap '+' icon button to immediately open `AddTransactionScreen`.
- [ ] Adding a new transaction inside the app triggers `WidgetUpdateHelper` to refresh widget data immediately.

## Multi-Currency & Travel Mode Platform
- [ ] Open **More -> Currency exchange**:
  - [ ] Live exchange rates displayed for JOD, USD, AED, SAR, EUR, GBP.
  - [ ] Record a currency exchange (e.g. 500 JOD -> 705 USD).
  - [ ] Verify Cash/Bank balances adjust accordingly and exchange is saved in `currency_exchanges`.
- [ ] Open **More -> Trips**:
  - [ ] Create a new trip (e.g. "Dubai Vacation", budget 1,000 AED, departure/return dates).
  - [ ] Log an expense assigned to the trip; verify trip-scoped daily pacing and budget remaining.

## Innovative Features & Islamic Finance
- [ ] **Item Price Memory & Inflation Tracker**:
  - [ ] Record split items with items and prices (e.g. "1kg Chicken: 2.800 JOD").
  - [ ] Open `PriceHistoryScreen` (More -> Price memory): verify price trend chart, latest price, and percentage changes.
- [ ] **Virtual Sadaqah Jar**:
  - [ ] Enable round-up in `SadaqahJarScreen` (More -> Sadaqah jar).
  - [ ] Record an expense of 4.350 JOD: verify 0.650 JOD is added to Sadaqah Jar balance.
  - [ ] Friday morning notification fires when balance > 0 JOD.
  - [ ] Tap "Donate & Empty Jar": records a charity transaction and resets jar balance to zero.
- [ ] **Financial Runway Stress-Tester**:
  - [ ] Open `FinancialRunwayScreen` (More -> Emergency runway).
  - [ ] Displays liquid survival gauge in months (`(Cash + Bank + Vault) / Monthly Burn`).
- [ ] **Murabaha & Islamic Loan Simulator**:
  - [ ] Open `MurabahaSimulatorScreen` (More -> Murabaha).
  - [ ] Enter principal, profit percentage, and tenor years; computes monthly installment and total bank profit.
  - [ ] Tap "Add to Configured Loans": creates an active loan with payment schedule.
- [ ] **Zakah Auto-Sync & Hawl Tracker**:
  - [ ] Open `ZakahCalculatorScreen`: tap "Auto-Sync App Balances" to pull liquid balances.
  - [ ] Computes Nisab threshold and calculates exact 2.5% payable Zakah.

## Monetization, 90-Day Pro Trial & Owner VIP Bypass
- [ ] Fresh app install automatically has 90 days of Pro Trial without account creation.
- [ ] Open `SubscriptionScreen` (not reachable in the app: the More banner was removed and no other control opens it; the screen and route are kept):
  - [ ] Displays days remaining in trial with feature breakdown.
- [ ] **Owner VIP Bypass**:
  - [ ] In `SubscriptionScreen`, tap the large Crown Hero icon **5 times in rapid succession**.
  - [ ] Toast confirms *"Owner VIP Access activated! All features unlocked permanently."*
  - [ ] Top banner displays "Lifetime VIP Access (Owner Bypass Active)".
  - [ ] Tap "Revoke Owner Bypass" to revert to standard user trial state for testing.

## Bank SMS Ingestion & Recurring Auto-Deductions
- [ ] In `AddTransactionScreen`, tap "Paste Bank SMS (Auto-Fill)":
  - [ ] Paste SMS from Arab Bank, Etihad, Al Rajhi, Emirates NBD, etc.
  - [ ] Auto-extracts amount, merchant name, and selects appropriate category.
- [ ] Incoming pending bank SMS items display in "Unreviewed Transactions" card on `OverviewScreen` with single-tap Confirm / Dismiss actions.
- [ ] Set a recurring transaction to "Auto-Deduct":
  - [ ] `AutoDeductRecurringWorker` runs daily via WorkManager, logs expense on due date, and dispatches notification.

## Notification Management (Clear All & Sticky Notifications)
- [ ] Open `AlertsScreen` with active alerts:
  - [ ] Tap "Clear All" in top bar or header; confirmation dialog appears.
  - [ ] Confirming dismisses all active alerts; screen immediately displays "All Caught Up" empty state.
- [ ] Open `NotificationSettingsScreen`:
  - [ ] Enable **Sticky Notifications** for Prayer, Daily Reminder, Budget Warning, Loan Due, or Salary.
  - [ ] When notification triggers, verify it is ongoing (`setOngoing(true)`) and cannot be dismissed by swiping away from the status bar.

## Regression Checks
- [ ] Categories and Subcategories CRUD still works.
- [ ] Existing historical data remains readable after v16 → v17 → v21 migration path.
- [ ] Currency numbers formatted with proper decimal places across all screens.
- [ ] Database browser includes all Room tables (`transactions`, `categories`, `sub_categories`, `budgets`, `configured_loans`, `monthly_loan_payments`, `recurring_transactions`, `app_alerts`, `transaction_splits`, `trips`, `currency_exchanges`, `item_price_history`, `pending_sms_transactions`).


