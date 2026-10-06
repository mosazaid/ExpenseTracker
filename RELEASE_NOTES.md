# Release Notes

## Version: Feature Expansion Phase 11 — Complete App Backup & Device Migration (JSON), Debt Partial Payments & History, Wallet Balance Correction, and Elevation-Calibrated Prayer Engine (October 2026)

### Highlights
- **Complete App Backup & Device Migration (Full JSON)**:
  - Created `FullBackupManager` supporting comprehensive JSON export and atomic restore of 100% of the application state.
  - Backs up all Room database entities across 9 tables: Categories, Subcategories, Budgets, Configured Loans, Recurring Transactions, Transactions, Transaction Splits, Monthly Loan Payments, and App Alerts.
  - Exports and restores all user configurations and preferences (`UserPreferences` / DataStore / SharedPreferences) including selected currency, theme mode, language, 12h/24h time format, account opening balances, alert master switches, prayer methods, notification preferences, and backup timestamps.
  - Strict Foreign Key order management: atomic deletion (`splits` -> `transactions` -> `payments` -> `budgets` -> `subcategories` -> `recurring` -> `loans` -> `categories` -> `alerts`) and insertion (`categories` -> `subcategories` -> `budgets` -> `loans` -> `recurring` -> `transactions` -> `splits` -> `payments` -> `alerts`) inside `AppDatabase.withTransaction`.
  - Accessible UI in `SettingsScreen`: dedicated **"Complete App Backup (Transfer to Another Device)"** card with system file picker (`CreateDocument` / `OpenDocument`), destructive confirmation dialog, and granular toast/snackbar reporting exact item counts restored.
- **Enhanced CSV Import & Export**:
  - Upgraded `CsvImporter` to parse `debtType`, `isDebtSettled`, `recurringId`, and `receiptImagePath` fields from CSV rows if present.
- **Debts & Loans Modernization (Partial Payments & Transaction History)**:
  - Added support for partial and full debt repayments with progress tracking (`x/y` paid).
  - Automatically records payment transactions in history with type-aware linking (`EXPENSE` for debts you owe, `INCOME` for debts owed to you).
  - Dynamically updates active debt alerts and clears resolved notifications once remaining balance reaches zero.
- **Wallet Balance Fix**:
  - Corrected balance accounting in `WalletCalculator` and `WalletViewModel` to ensure held wallet balances correctly display positive funds without inverted signs.
- **Elevation Horizon Dip Correction in Solar Prayer Calculations**:
  - Incorporated astronomical elevation horizon dip ($\text{dip} = 0.0347^\circ \times \sqrt{h}$) into `PrayerTimesCalculator.calculateDaySchedule`.
  - Fixed discrepancy where Maghrib was calculated ~4 minutes early in high-elevation locations like Amman (~780m above sea level), achieving exact minute alignment with official Ministry of Awqaf and TimesPrayer schedules.

---

## Version: Feature Expansion Phase 10 — 12H/24H Time Format, App Icon Notifications, High-Precision Solar Prayer Engine, Expanded Global Cities & Financial Insights Info Fix (October 2026)

### Highlights
- **Financial Insights Card Info Icon & Responsive Layout**:
  - Restructured `FinancialInsightsCard` header row to guarantee the Info `IconButton` is always rendered, accessible, and never pushed off-screen or clipped by long titles on compact screens.
  - Title and period subtitle are constrained with `Modifier.weight(1f)` and `TextOverflow.Ellipsis`.
  - Positioned the Info icon (`Icons.Outlined.Info`) in the right-hand action cluster adjacent to the Status Pill with an accessible 36dp touch target.
  - Tapping opens the interactive `FinancialInsightsInfoDialog` with detailed visual breakdowns for spending velocity, daily safe allowance, early cycle damping, and projected month-end surplus/deficit.
  - Subtitle now displays the custom cycle label if configured (e.g. "Salary Cycle • Day 12/30") or falls back to "Day X of Y • Z days left".
- **12-Hour vs 24-Hour Time Format Configuration**:
  - Added user toggle in `SettingsScreen` under General settings to choose between 12-hour format (`hh:mm a` e.g. `02:30 PM`) and 24-hour format (`HH:mm` e.g. `14:30`).
  - Preference persisted via DataStore in `UserPreferences.timeFormat12H` (default: 12-hour).
  - Applied dynamically across the app including the transaction time picker in `AddTransactionScreen` and the prayer timetable on `PrayerQiblahScreen`.
  - Fully localized in English and Arabic with responsive preview chips.
- **Application Launcher Icon in Notifications**:
  - Replaced generic Android dialog icons in `NotificationHelper` with the official app launcher icon (`R.mipmap.ic_launcher`) for recurring expense reminders, salary notifications, and prayer alert notifications.
- **High-Precision Solar Prayer Engine Calibration (TimesPrayer Amman)**:
  - Upgraded solar calculation equations in `PrayerTimesCalculator` to Jean Meeus / NOAA high-precision standard solar coordinates.
  - Added elevation horizon dip correction ($\text{dip} = 0.0347^\circ \times \sqrt{h}$) calibrated to Amman's elevation (~800m), eliminating the 5-minute Maghrib and Isha discrepancy and precisely matching official times from `timesprayer.com/en/prayer-times-in-amman.html` (Fajr 5:10, Sunrise 6:27, Dhuhr 12:25, Asr 15:47, Maghrib 18:23, Isha 19:40 for Oct 3, 2026).
- **Expanded Global Cities & Countries in City Picker**:
  - Enriched the offline city catalog with comprehensive city lists across:
    - **Austria (Namsa)**: Vienna, Graz, Linz, Salzburg, Innsbruck, Klagenfurt, Villach, Wels.
    - **Philippines**: Manila, Quezon City, Davao City, Caloocan, Cebu City, Zamboanga, Taguig, Antipolo, Pasig, Cagayan de Oro, Iloilo City, General Santos.
    - **Malaysia**: Kuala Lumpur, George Town, Johor Bahru, Ipoh, Shah Alam, Melaka, Kota Kinabalu, Kuching, Petaling Jaya, Alor Setar.
    - **Europe**: Germany, France, United Kingdom, Italy, Spain, Netherlands, Switzerland, Sweden, Norway, Denmark, Poland, Belgium, Greece, Ireland, Portugal, Czech Republic, Romania, Hungary.
    - **Middle East & North Africa**: Jordan, Saudi Arabia, UAE, Egypt, Palestine, Turkey, Qatar, Kuwait, Oman, Bahrain, Morocco, Algeria, Tunisia, Lebanon, Iraq.
  - Searchable country and city picker bottom sheet with instant filtering by English or Arabic name.

---

## Version: Feature Expansion Phase 9 — Split Subcategory Attribution, Financial Pacing Fixes, Subcategory Stats Comparison, Real-Time Sensor Qiblah Compass & Multi-Method Prayer Calculation (October 2026)

### Highlights
- **Split Subcategory Breakdown & History Display**:
  - Solved split expense amounts collapsing into "Others": `subCategorySpendMap` now parses all `TransactionSplit` items, accurately mapping each split amount to its designated subcategory (e.g. Shopping -> Grocery 5.00 JOD, Sweets 7.00 JOD).
  - Enriched transaction items in `HistoryScreen` to display individual subcategory badges with exact split amounts (`Grocery 5.00 JOD • Sweets 7.00 JOD`) alongside debtor attribution chips.
- **Financial Insights Wallet Calculation & Salary Cycle Anchor Fix**:
  - **Wallet Reserved Balance**: Fixed calculation in `FinancialInsightsCard` where `totalWallet` was previously added to `totalExpense`, creating an artificial expense spike. Wallet funds are now passed as `walletReserved` and subtracted from `baselineBudget = max(0.0, totalIncome - walletReserved)` in `SpendingPacingCalculator`.
  - **Salary Cycle Period Boundary**: Fixed `PeriodCalculator` where cycles without a future salary anchor previously truncated the period end date to today, creating a false "1 day remaining" warning for a salary received 8 days prior. Natural end now extends a full month minus one day from the salary anchor.
- **Statistics Screen Subcategory Comparison & 3-Month Trends**:
  - Added interactive `SubcategoryComparisonCard` on `StatisticsScreen` below the category pie chart.
  - **Within-Period Breakdown**: Category selector dropdown displaying all subcategories with spent amounts and visual percentage progress bars.
  - **Cross-Month Trend (3-Month Comparative Table)**: Analyzes subcategory expenses across current month, previous month, and 2 months ago, displaying tabular month-over-month (MoM) delta percentage badges (+/- %).
- **Real-Time Sensor-Driven Qiblah Compass (`PrayerQiblahScreen`)**:
  - Built `CompassSensorManager` using `Sensor.TYPE_ROTATION_VECTOR` with seamless fallback to `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD`, featuring shortest-circular-path exponential smoothing for responsive and jitter-free dial rotation.
  - Interactive compass dial rotating by `-azimuth` so True North aligns with physical room North.
  - Kaaba needle pointing to Mecca relative to device orientation with golden/emerald spearhead and Kaaba emblem.
  - **Alignment State**: Outer ring glows with an animated emerald green pulse (`#10B981`) and triggers tactile haptic feedback when the user points their phone within ±3.5° of Al-Kaaba.
  - Real-time turn guidance: displays "Facing Al-Kaaba (Qiblah)!" or "Turn phone right/left by X°" with directional navigation arrows.
  - Device tilt warning (advising user to hold phone flat) and sensor accuracy badge with figure-8 calibration hint.
- **12 International Prayer Calculation Methods & Auto-Detection**:
  - Added `PrayerCalculationMethod` supporting 12 global calculation standards:
    - Jordan (General Iftaa' Department - 18.0° / 18.0°)
    - Umm al-Qura (Makkah, Saudi Arabia - 18.5° / 90 min)
    - Egyptian General Authority of Survey (19.5° / 17.5°)
    - Diyanet İşleri Başkanlığı (Turkey - 18.0° / 17.0°)
    - University of Islamic Sciences (Karachi - 18.0° / 18.0°)
    - Islamic Society of North America (ISNA - 15.0° / 15.0°)
    - Muslim World League (MWL - 18.0° / 17.0°)
    - Dubai UAE Islamic Affairs (18.2° / 18.2°)
    - Kuwait Ministry of Awqaf (18.0° / 17.5°)
    - Qatar Ministry of Awqaf (18.0° / 90 min)
    - MUIS (Singapore / Malaysia / Indonesia - 20.0° / 18.0°)
    - Institute of Geophysics (Tehran - 17.7° / 14.0°)
  - Automatic detection based on device country code with manual override dialog.
- **Salah Reminders & Overview Quick GPS Sync**:
  - Created `PrayerReminderReceiver` and updated `AlarmScheduler` with `CHANNEL_PRAYER` notification channel and per-prayer alarm scheduling.
  - Individual notification bell toggles for Fajr, Dhuhr, Asr, Maghrib, and Isha.
  - Added GPS Location Sync button to the top bar of `OverviewScreen`, detecting user location on demand and presenting immediate feedback via `SnackbarHost`.

---

## Version: Feature Expansion Phase 8 — Multi-Split Transactions, AI Financial Intelligence, Offline Islamic Finance & Persistent Budgets (October 2026)

### Highlights
- **Multi-Item Split Transactions & Debtor Allocations (Room v17)**:
  - Users can divide any expense into multiple sub-items across different subcategories and individual debtors.
  - Interactive split editor in `AddTransactionScreen` featuring real-time remaining balance calculations, quick auto-fill buttons, and per-split debtor assignment (`LENT`).
  - Added `transaction_splits` table and `TransactionSplitDao` with foreign-key cascade deletion.
- **Offline Receipt Photo Capture & Interactive Pinch-to-Zoom Viewer**:
  - Added camera photo capture via `ActivityResultContracts.TakePicture()` alongside the existing system PhotoPicker.
  - Interactive **Image Source Bottom Sheet** allowing users to choose between capturing a photo with their camera or selecting an existing image from their gallery.
  - **Automatic EXIF Orientation Correction**: Solved vertical photos appearing horizontally by reading EXIF metadata (`ExifInterface.TAG_ORIENTATION`) directly from camera/gallery streams and rotating the bitmap upright before saving to disk.
  - Added `ZoomableImageDialog` with finger pinch-to-zoom (up to 5x), pan gestures bounded to viewport, double-tap toggle, a 90° manual rotation button (`RotateRight`), and zoom percentage readout.
  - Tapping attached receipts in `TransactionDetailBottomSheet` or `ReceiptAttachmentSection` opens the full-screen zoomable viewer.
- **Centralized Runtime Permission Architecture & Rationale UI**:
  - Implemented `PermissionHelper` managing `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS` (Android 13+), and `SCHEDULE_EXACT_ALARM` (Android 12+).
  - Built `PermissionRationaleDialog` adhering to UI/UX Pro Max guidelines with visible hierarchy, clear explanations, and direct navigation to system App Settings when permanently denied.
  - Integrated across `ReceiptAttachmentSection` (Camera), `SettingsScreen` (Location), `PrayerQiblahScreen` (Location), `NotificationSettingsScreen` (Notifications), and `MainActivity`.
- **Split Transaction UI Enhancements**:
  - In `AddTransactionScreen`, the main subcategory input is automatically hidden when Split Mode is active, preventing confusion since each split item designates its own subcategory.
  - Replaced single-row subcategory chips with responsive multi-line `FlowRow` in each split item, allowing all subcategories to be visible with checkmark selection indicators.
- **AI Spending Intelligence & Interactive Info Dialog**:
  - `SpendingPacingCalculator` calculates cycle-aware velocity (`% Budget Spent ÷ % Time Elapsed`), daily burn rates, and safe daily allowances (`Remaining Budget ÷ Days Left`).
  - Implemented early-cycle damping recognizing that upfront lump-sum payments (e.g. rent or bills on days 1–5) are normal, preventing unrealistic linear multipliers.
  - Added an interactive Info (`i`) button on `FinancialInsightsCard` opening `FinancialInsightsInfoDialog` explaining the financial cycle, burn rate, safe spend, and projected outcomes in plain English and Arabic.
- **Gamified Financial Health Score**:
  - `FinancialHealthScoreCard` dynamically scores financial performance (0 to 100) across savings ratio, budget adherence, and debt load.
  - Features gamified tiers (*Financial Novice*, *Budget Builder*, *Wealth Strategist*, *Financial Master*) with expandable actionable financial tips.
- **Offline Islamic Finance & Location Intelligence**:
  - **Zakah Calculator (`ZakahCalculatorScreen`)**: Computes exact 2.5% Zakah obligation with live gold Nisab threshold comparisons (85 grams of gold).
  - **Offline Prayer Times & Qiblah Compass (`PrayerQiblahScreen`)**: Pure mathematical astronomical solar calculations (`PrayerTimesCalculator`) showing 5 daily prayer schedules with live countdowns and a sensor-driven Kaaba compass.
  - **Location Auto-Detection (`LocationHelper`)**: Requests runtime permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`), performs reverse geocoding with fallback to nearest preset cities, and automatically selects the local currency.
- **Persistent Category Budgets & Categories Screen Redesign**:
  - Solved budget disappearance on month rollover: `BudgetProgressCalculator` now falls back to the latest configured category budget limit if no entry exists for the exact period boundary.
  - Redesigned `CategoriesScreen` with Segmented tabs (**Categories** vs **Monthly Budgets**), clean modal dialogs, and usage-based sorting from most used to least used.
  - Prominent `CategoryBudgetsCard` permanently displayed on `OverviewScreen`.
- **Global Currency Support & Settings Organization**:
  - Expanded `CurrencyUtils` to support Arab nations (IQD, LBP, SYP, ILS, LYD, TND, DZD, MAD, YER, SDG), Turkey (TRY), and European/Global currencies (EUR, GBP, CHF, SEK, NOK, DKK, PLN, CAD, AUD, JPY, CNY, INR, PKR, MYR, SGD).
  - Cleanly organized `SettingsScreen` into 5 logical cards and decoupled all alert toggles into a dedicated `NotificationSettingsScreen`.

---

## Version: Feature Expansion Phase 7 - Type-Safe Navigation, Performance Architecture & Test Hardening (September 2026)

### Highlights
- **Type-Safe Compose Navigation via `kotlinx.serialization`**:
  - Replaced legacy string-based route constants with strongly typed `@Serializable` destinations in `AppDestinations.kt`.
  - Type-safe bottom tab destinations: `Overview`, `History`, `Statistics`, `More`.
  - Type-safe push destinations: `Transfer`, `Wallet`, `Loans`, `Alerts`, `Debts`, `Recurring`, `Categories`, `Settings`, `DatabaseBrowser`, `Onboarding`.
  - Parameterized destination data classes: `AddTransaction(transactionId, recurringId)`, `EditTransfer(transactionId)`, and `EditWallet(transactionId)`.
  - Full compile-time argument verification; zero string parsing or bundle casting errors.
- **Thread-Safe & Concurrency-Safe Date Engine (`DateUtils`)**:
  - Completely migrated internal formatting and parsing from legacy `SimpleDateFormat` (which is not thread-safe across coroutines) to modern `java.time.format.DateTimeFormatter`, `LocalDate`, and `LocalDateTime`.
  - Enhanced ISO date-time parsing with hierarchical fallback ensuring exact hour/minute/second preservation.
  - Corrected Saturday start-of-week calculation with calendar alignment.
- **Overview Recomposition & Performance Optimization**:
  - Converted `monthSummary` in `OverviewViewModel` into a hot, reactive `StateFlow` leveraging `combine` and `flatMapLatest`.
  - Removed UI-layer synchronous data calculation inside `LaunchedEffect(allTransactions)`, preventing frame drops and redundant background recomputations.
- **Comprehensive Test Suite & CI Hardening**:
  - **Unit Tests (`app/src/test`)**: Added `AppDestinationsTest` validating JSON serialization and deserialization of all route contracts and parameter defaults. Extended `CoreUtilsTest` to thoroughly cover monthly/yearly boundary calculations, month keys, and thread-safe date parsing.
  - **Instrumented Tests (`app/src/androidTest`)**: Added `AppDatabaseInstrumentedTest` testing in-memory Room database lifecycle, entity persistence, and `CategoryDao` queries. Added `ComposeUiInstrumentedTest` testing Compose theme rendering and UI node display.
  - All unit tests verified passing (`./gradlew testDebugUnitTest`).

---

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
