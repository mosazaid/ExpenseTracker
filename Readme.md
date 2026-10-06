# ExpenseTracker — Documentation

A modern Android expense tracking app built with **Kotlin**, **Jetpack Compose**, **Room**, **Hilt**, and **MVVM**.

## Overview

ExpenseTracker helps you track personal money with salary-aware months, Cash/Bank/Wallet accounts, reimbursements, multi-split transactions, category budgets, AI spending pacing, offline Islamic finance tools (Zakah & Prayer/Qiblah), and local backup via import/export.

---

## What's New (Phase 11 Releases)

### 1. Complete App Backup & Device Migration (Full JSON)
- **100% Data & State Portability**: Built `FullBackupManager` to enable single-file JSON export and atomic restore when migrating to another phone or restoring your setup.
- **Preferences & SharedPreferences / DataStore**: Captures all user settings (currency, theme, language, 12h/24h format, opening balances, alert master toggles, prayer methods, notification configs, backup timestamps).
- **All 9 Room Database Tables**: Categories, Subcategories, Budgets, Loans, Recurring items, Transactions, Splits, Loan payments, and App Alerts.
- **Atomic & FK Safe**: Employs `AppDatabase.withTransaction` with strict reverse-dependency deletion and forward-dependency insertion to ensure zero data corruption.
- **Safe UI Experience**: Located in **Settings -> Data Management** under **Complete App Backup (Transfer to Another Device)** with file pickers, destructive action confirmation dialogs, and item count summaries.

### 2. Debt Partial Payments & Transaction History Integration
- Supported flexible debt repayments (pay partially or in full) with progress indicator (`x/y` paid).
- Automatically records repayment transactions in history linked to the counterparty.
- Auto-clears or updates debt notification alerts when balance is reduced or fully settled.

### 3. Wallet Balance Correction
- Rectified wallet accounting logic in `WalletCalculator` and `WalletViewModel` so held funds properly reflect positive balances.

### 4. Solar Prayer Calculation Elevation Dip
- Integrated elevation horizon depression angle ($\text{dip} = 0.0347^\circ \times \sqrt{h}$) into `PrayerTimesCalculator`, fixing the Maghrib prayer discrepancy for elevated locations like Amman.

---

## What's New (Phase 10 Releases)

### 1. 12-Hour vs 24-Hour Time Format Configuration
- Added customizable time format toggle under **Settings -> General**: switch between 12-Hour (`hh:mm a` e.g. `02:30 PM`) and 24-Hour (`HH:mm` e.g. `14:30`).
- Automatically formats time pickers in `AddTransactionScreen` and prayer timetable on `PrayerQiblahScreen`.
- Fully localized in English and Arabic with preview chips.

### 2. High-Precision Astronomical Prayer Engine (TimesPrayer Amman Calibrated)
- Powered by Jean Meeus / NOAA high-precision standard solar position algorithms with elevation horizon dip correction ($\text{dip} = 0.0347^\circ \times \sqrt{h}$) calibrated to Amman (~800m).
- Eliminates prayer discrepancies and matches official Amman timetables (`timesprayer.com/en/prayer-times-in-amman.html`): Fajr 5:10, Sunrise 6:27, Dhuhr 12:25, Asr 15:47, Maghrib 18:23, Isha 19:40 for Oct 3, 2026.

### 3. Application Launcher Icon in System Notifications
- Updated `NotificationHelper` to use the official app launcher icon (`R.mipmap.ic_launcher`) across all notification channels (prayer reminders, salary banners, recurring expense reminders).

### 4. Expanded Global Country & City Catalog
- Searchable city picker now covers comprehensive city lists for **Austria (Namsa)**, **Philippines**, **Malaysia**, **Europe** (UK, France, Germany, Spain, Italy, Switzerland, Sweden, Norway, etc.), and **Middle East / North Africa**.

### 5. Financial Insights Header Layout & Info Icon Fix
- Restructured `FinancialInsightsCard` header layout: guaranteed that the Info `IconButton` is always visible and never clipped on narrow mobile viewports.
- Info button is placed adjacent to the Status Pill with an accessible 36dp touch target, opening the comprehensive `FinancialInsightsInfoDialog` breakdown.

---

## What's New (Phase 9 Releases)

### 1. Real-Time Sensor Qiblah Compass & Multi-Method Prayer Calculations
- **Interactive Sensor-Driven Qiblah Compass (`PrayerQiblahScreen`)**:
  - Leverages Android hardware sensors (`Sensor.TYPE_ROTATION_VECTOR` with accelerometer + magnetometer fallback) and exponential shortest-path circular smoothing for a real-time, jitter-free rotating compass dial.
  - Aligns True North with physical North in real time as the device turns.
  - Kaaba indicator needle points directly to Mecca with an elegant gold/emerald spearhead and Kaaba emblem.
  - **Tactile Alignment & Glowing Feedback**: When the user rotates their phone within ±3.5° of Al-Kaaba, the dial illuminates with an animated emerald green halo (`#10B981`), provides turn guidance, and triggers a tactile haptic vibration.
  - Interactive turn guidance ("Turn phone right/left by X°") and tilt warnings if the phone is held unevenly.
- **12 Global Prayer Calculation Standards**:
  - Offline solar calculation engine supporting: Jordan General Iftaa', Umm al-Qura (Makkah), Egyptian Survey Authority, Turkey Diyanet, Karachi, ISNA, Muslim World League (MWL), Dubai, Kuwait, Qatar, Singapore MUIS, and Tehran.
  - Automatic country detection via device GPS with manual override dialog.
- **Salah Reminders & Overview Quick GPS Sync**:
  - Per-prayer notification bell toggles (Fajr, Dhuhr, Asr, Maghrib, Isha) scheduled via Android `AlarmScheduler` and `PrayerReminderReceiver`.
  - Instant GPS Location Sync button in the top bar of `OverviewScreen` with real-time feedback via `SnackbarHost`.

### 2. Statistics Subcategory Comparison & 3-Month Trends
- **Interactive Subcategory Comparison Card (`SubcategoryComparisonCard`)**:
  - Located on `StatisticsScreen` below the category pie chart.
  - **This Period Breakdown**: Pick any category to inspect all its active subcategories with exact spent amounts, tabular figures, and proportional percentage progress bars.
  - **Cross-Month Trend (3-Month Table)**: Side-by-side comparison across current month, previous month, and 2 months ago with visual Month-over-Month (MoM) delta percentages (+/- %).

### 3. Split Subcategory Fixes & History Badges
- **Accurate Subcategory Attribution**: Fixed issue where split expense items were attributed to "Others"; `subCategorySpendMap` now correctly maps each split item to its chosen subcategory.
- **Rich Badges in History**: Split transactions in `HistoryScreen` now display individual subcategory badges with exact split amounts (e.g. `Grocery 5.00 JOD • Sweets 7.00 JOD`).

### 4. Financial Insights & Cycle Anchor Fixes
- **Wallet Reserved Funds**: Fixed `FinancialInsightsCard` adding wallet balances to expenses; wallet funds are now preserved as savings and subtracted from the baseline budget.
- **Salary Cycle Period Boundary**: Fixed `PeriodCalculator` end-date truncation so active salary cycles accurately compute days remaining without showing false "1 day remaining" alerts.

---

## What's New (Phase 0 – Phase 8 Releases)

### 1. Complex Transactions & Splitting
- **Transaction Splitting**: Split any single expense into multiple items across different subcategories or debtors (e.g. at a grocery store, allocate an amount to Chicken & Meat, an amount to Snacks & Sweets, and record a debt portion for a friend).
- **Multi-Debtor Tracking on Split**: Each split line item can individually mark debt type (`LENT`) and debtor name.
- **Subcategory Auto-Hiding & Multi-Line Flow**: When Split Mode is activated, the main single-category subcategory dropdown is automatically hidden. In the split items list, subcategory chips wrap responsively using multi-line `FlowRow` with checkmark selection indicators.
- **Offline Receipt Attachments & Camera Photo Capture**: Choose between taking a photo with the camera (`TakePicture`) or selecting an image from the gallery (`PickVisualMedia`). Captured photos are processed with automatic EXIF orientation correction to prevent sideways/rotated photos and saved securely in private app storage (`files/receipts/`).
- **Interactive Pinch-to-Zoom Receipt Viewer (`ZoomableImageDialog`)**: Fullscreen high-resolution receipt modal featuring smooth pinch-to-zoom (up to 5x), viewport-bounded panning gestures, double-tap zoom toggle (1x ↔ 2.5x), zoom percentage readout with tabular numbers, and a 90° manual rotation tool.
- **Centralized Runtime Permissions (`PermissionHelper` & `PermissionRationaleDialog`)**: Unified permission architecture handling Camera, Location, Notifications, and Exact Alarms with UI/UX Pro Max rationale dialogs, permanent denial detection, and direct navigation to system App Settings.
- **Transaction Details Bottom Sheet (`TransactionDetailBottomSheet`)**: Tap any transaction to open an interactive modal displaying splits, receipt previews, formatted account badges, and quick edit/delete actions.
- **Swipe-to-Delete Overlap Fix**: Fixed swipe gesture layout on Transfer and Wallet moves so delete icons only appear upon active swiping.

### 2. Shopping Categories & Arabic Subcategories
- **New Shopping Subcategories**: Added `Chicken & Meat` and unified `Snacks & Sweets` (replacing separate chips, biscuits, cookies, treats; in Arabic: "أشياء زاكية").
- **Room DB Migration 15 → 16**: Automatically populates default shopping subcategories if missing.

### 3. Category Budgets & Persistent Spending Limits
- **Budget Persistence Across Months**: Budgets no longer disappear when transitioning into a new salary or calendar month; system gracefully falls back to the latest configured category budget limit.
- **Redesigned Categories & Budgets Screen**:
  - Segmented control tabs: **Categories** vs **Monthly Budgets**.
  - Visual indicators: Progress bar with percentage and remaining balance for categories with active budgets.
  - Dedicated **+ Set Budget** outline chip for categories without limits.
  - Clean modal dialogs for adding/editing categories and budgets (no cluttered top forms).
- **Persistent Overview Budget Card (`CategoryBudgetsCard`)**: Always visible on the Overview screen, providing spending progress and a direct CTA to configure limits.

### 4. AI Financial Intelligence & Gamification
- **Spending Pacing Engine (`SpendingPacingCalculator`)**:
  - Analyzes current elapsed days vs total days in period.
  - Calculates daily burn rate, projected month-end total, and safe daily allowance.
  - Provides actionable status badges: **ON TRACK**, **MODERATE SPENDING**, and **OVERSPENDING WARNING**.
- **Smart AI Insights Card (`FinancialInsightsCard`)**: Rendered directly on the Overview dashboard with dual progress indicators (elapsed time vs spent budget).
- **Financial Health Score Card (`FinancialHealthScoreCard`)**:
  - Gamified score (0 to 100) evaluating savings ratio, budget adherence, and debt load.
  - Tiers: *Financial Novice*, *Budget Builder*, *Wealth Strategist*, *Financial Master*.
  - Expandable actionable tips for financial improvement.

### 5. Offline Islamic Finance & Location Intelligence
- **Location Auto-Detection (`LocationHelper`)**:
  - Runtime permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) with dialogs and fallback to nearest preset capital cities.
  - Auto-selects user local currency based on GPS country code (JOD, SAR, AED, KWD, QAR, BHD, OMR, EGP, USD, EUR, GBP, TRY).
- **Offline Prayer Times & Qiblah Compass (`PrayerQiblahScreen`)**:
  - Pure offline astronomical calculations (`PrayerTimesCalculator`) — Fajr, Dhuhr, Asr, Maghrib, Isha.
  - Live countdown to next prayer.
  - Sensor-driven Qiblah compass with bearing to the Kaaba in Mecca.
- **Zakah Calculator (`ZakahCalculatorScreen`)**:
  - 85g gold Nisab threshold comparison.
  - Categorized breakdown: cash/bank balances, gold/jewelry, business inventory, minus immediate deductible debts.
  - Exact 2.5% Zakah obligation calculation.

### 6. Notification Center Management
- **Notification Preferences (`NotificationSettingsScreen`)**:
  - Fine-grained channel toggles in Settings: Daily 9:00 PM Reminders, Budget Threshold Alerts, Loan Due Date Alerts, and Salary Day Rollovers.

---

## Core Features

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
  - **Financial Health Score Card** & **AI Spending Insights Card**
  - **Category Budgets Card** with interactive progress and direct navigation to budget manager
- **Transactions**:
  - Grouped list, swipe-left to delete (trash icon hidden to prevent visual clutter), tap to view full details
  - **Structured Item Rows**: Row 1 (description + Cash/Bank badge), Row 2 (category in darker grey `onSurfaceVariant` / Medium + dot • + date in lighter grey `outline`), Row 3 (subcategory chip + formatted amount)
- **Filters**:
  - Calendar/salary mode, period (Day/Week/Month/Year), type, category + budget progress
  - **Subcategory filter**: filter by specific subcategories or choose **"Other"** for transactions without assigned subcategories

### Statistics
- Period filters: Day, Week, Month, Year
- **Interactive Multi-Chart (Last 3 Months)**:
  - Toggle between **Combined (Line + Bar)**, **Line Chart**, and **Bar Chart** views
  - Side-by-side monthly spending and income trends
- **3-Month Category Expense Pie Charts**:
  - Independent interactive donut/pie charts for the last 3 months
  - Top category spend comparison with percentage labels and legends
- Totals: Income, Expense, **Wallet**, Balance

### Debts & Loans
- **Debt Tracking & Directional Separation**:
  - Expense + Debt: Money lent to someone (debtor owes user).
  - Income + Debt: Repayment to user OR borrowed debt from someone.
  - Dedicated `DebtsScreen` with debtor grouping, settle actions, and month rollover.
- **Configured Recurring Loans**: Track car, mortgage, and personal loans in `LoansScreen`; once-per-day prompt bottom sheet (`LoanReminderBottomSheet`) on salary month rollover with salary balance preservation.
- **Salary Month Periods**: Accurate loan period tracking (`"salary-yyyy-MM-dd"`) prevents early loan notification triggers.

---

## Navigation (Type-Safe Compose Navigation)

The app utilizes **Type-Safe Jetpack Compose Navigation** powered by `kotlinx.serialization`:

| Bottom tab | Destination Type | Purpose |
|------------|------------------|---------|
| Overview | `Overview` | Balance cards, monthly overview, and quick actions |
| History | `History` | Transaction list, overview breakdown & filters |
| **Add** (FAB) | `AddTransaction(...)` | Add/edit income & expense with optional params (`transactionId`, `recurringId`) |
| Stats | `Statistics` | Interactive multi-charts & 3-month category pie charts |
| More | `More` | Hub for Debts, Loans, Categories, Database Browser, Settings, Zakah & Prayer |

Secondary & Push Destinations:
- `Transfer`: Cash ↔ Bank transfer screen
- `Wallet`: Wallet pocket money screen
- `Loans`: Configured recurring loans & payment schedules
- `Alerts`: In-app notification center
- `Debts`: Debt tracking & settlements
- `Recurring`: Recurring transaction templates
- `Categories`: Category and subcategory management
- `Settings`: Preferences, themes, exports, notification toggles, and security lock
- `NotificationSettings`: Granular alert channel toggles
- `PrayerQiblah`: Offline prayer times & Qiblah compass
- `ZakahCalculator`: 85g gold Nisab & wealth calculator
- `DatabaseBrowser`: In-app Room database inspector
- `Onboarding`: Multi-step onboarding carousel

---

## Database (Room v17)

### Entities
- `Transaction` — amount, description, subDescription, date, type, categoryId, subCategoryId, accountType, toAccountType, startsNewPeriod, carriedForwardBalance, receiptImagePath, debt fields, etc.
- `TransactionSplit` — id, transactionId, subCategoryId, amount, note, debtType, debtorNote
- `Category` — name, icon, color, type, isDefault
- `SubCategory` — name, categoryId, isDefault
- `Budget` — id, categoryId, amount, periodStart, periodEnd
- `ConfiguredLoan`, `MonthlyLoanPayment`, `AppAlert`, `RecurringTransaction`

### Key migrations
| Version | Change |
|---------|--------|
| 11→12 | Added `ConfiguredLoan` and `MonthlyLoanPayment` tables |
| 12→13 | Added `AppAlert` table for persistent in-app notifications |
| 13→14 | Recurring transactions template support |
| 14→15 | Added `debtType` and `isDebtSettled` to `transactions` |
| 15→16 | Added default shopping subcategories (`Chicken & Meat`, `Snacks & Sweets`) |
| 16→17 | Added `transaction_splits` table and `receiptImagePath` column |

---

## Detailed Calculation Methods & Mathematical Formulas

### 1. Astronomical Prayer Times Engine (`PrayerTimesCalculator.kt`)
- **Astronomical Model**: Uses Jean Meeus / NOAA standard solar position equations with Julian Day ($JD$) tracking.
- **Solar Coordinates**: Calculates Solar Mean Anomaly $g$, Mean Longitude $q$, Ecliptic Longitude $l$, Obliquity $e$, Declination $\delta$, and Equation of Time $EqT$.
- **Solar Noon (Dhuhr)**: $\text{Noon} = 12.0 - EqT - (\text{Longitude}/15.0) + \text{TimezoneOffset}$.
- **Hour Angle ($\cos HA$)**: $\cos(HA) = \frac{\sin(\alpha) - \sin(\text{lat})\sin(\delta)}{\cos(\text{lat})\cos(\delta)}$.
- **Elevation Horizon Dip**: $\text{Dip} = 0.0347^\circ \cdot \sqrt{\text{elevation}_{\text{meters}}}$ (accounting for elevation above sea level, e.g. Amman at 780m, Jerusalem at 750m).
- **Asr Shadow Altitude**: $\alpha_{\text{Asr}} = \operatorname{arccot}(1.0 + \tan|\text{lat} - \delta|)$.
- **Iterative Refinement**: Performs 2-pass recalculation of solar declination and $EqT$ at the exact calculated prayer moments.
- **12 Global Juridical Standards**: Auto-detected via GPS country code or selectable manually:
  - Jordan General Iftaa' ($18.0^\circ / 18.0^\circ$)
  - Umm al-Qura, Saudi Arabia ($18.5^\circ$ / 90 min)
  - Egyptian General Authority of Survey ($19.5^\circ / 17.5^\circ$)
  - Turkey Diyanet ($18.0^\circ / 17.0^\circ$)
  - Karachi ($18.0^\circ / 18.0^\circ$)
  - ISNA North America ($15.0^\circ / 15.0^\circ$)
  - Muslim World League ($18.0^\circ / 17.0^\circ$)
  - Dubai UAE ($18.2^\circ / 18.2^\circ$)
  - Kuwait Awqaf ($18.0^\circ / 17.5^\circ$)
  - Qatar Awqaf ($18.0^\circ$ / 90 min)
  - MUIS / JAKIM Southeast Asia ($20.0^\circ / 18.0^\circ$)
  - Tehran Geophysics ($17.7^\circ / 14.0^\circ$)

### 2. Great-Circle Qiblah Direction & Compass Sensor Fusion (`CompassSensorManager.kt`)
- **Spherical Trigonometry Forward Azimuth**:
  $\theta = \operatorname{atan2}\left(\sin(\Delta\lambda), \; \cos(\phi_1)\tan(\phi_2) - \sin(\phi_1)\cos(\Delta\lambda)\right)$
  Computed toward the Holy Kaaba in Makkah ($21.4225^\circ\text{ N}, 39.8262^\circ\text{ E}$).
- **Sensor Fusion**: Hardware `Sensor.TYPE_ROTATION_VECTOR` with fallback to `ACCELEROMETER` + `MAGNETIC_FIELD`.
- **Jitter-Free Exponential Smoothing**: Shortest-path circular low-pass filter ($\alpha = 0.15$) preventing dial jumping across the $0^\circ/360^\circ$ boundary.
- **Alignment Feedback**: Tactile haptic vibration and animated glowing emerald halo trigger within $\pm 3.5^\circ$ of Mecca.

### 3. Financial Spending Pacing & Velocity (`SpendingPacingCalculator.kt`)
- **Baseline Budget**: $\text{Baseline} = \max(0.0, \; \text{TotalIncome} - \text{WalletReserved})$.
- **Velocity Ratio**: $V = \frac{\text{BudgetConsumedRatio}}{\text{TimeElapsedRatio}}$ (where $V \le 1.05$ is optimal, and $V > 1.30$ triggers warning).
- **Safe Daily Spend**: $\text{SafeDailySpend} = \max\left(0.0, \; \frac{\text{Baseline} - \text{TotalExpense}}{\text{DaysRemaining}}\right)$.
- **Projected Period Outcome**: $\text{Projected} = \text{Baseline} - (\text{DailySpendRate} \cdot \text{TotalPeriodDays})$ (positive = surplus, negative = deficit).
- **Early Cycle Damping**: Softens alerts during first 3 days of period to accommodate upfront monthly bills.

### 4. Gamified Financial Health Score (`FinancialHealthScoreCard.kt`)
- Composite 0–100 index evaluated across 3 key vectors:
  $\text{Score} = (40\% \cdot \text{SavingsRatio}) + (35\% \cdot \text{BudgetAdherence}) + (25\% \cdot \text{DebtLoanHealth})$.

### 5. Zakah Obligation Engine (`ZakahCalculator.kt`)
- Evaluates net liquid assets against **85 grams of 24k gold Nisab**.
- Implements 1 lunar year Hawl qualification and exact $2.5\%$ ($1/40$) obligation.

---

## Architecture Blueprints & Tech Stack

- **UI / Presentation Layer**: Jetpack Compose, Material 3, Clean MVVM, Kotlin Coroutines, and `StateFlow` unidirectional data flow.
- **Type-Safe Routing**: Jetpack Navigation Compose 2.8.8 using `@Serializable` Kotlin objects and classes.
- **Dependency Injection**: Google Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Singleton`).
- **Offline Persistence**: Room Database (v17) with SQLite, Kotlin Symbol Processing (KSP), foreign key cascade deletion, and AndroidX DataStore for user preferences.
- **Background Scheduling**: Dual-engine architecture with Android `AlarmManager` (`setExactAndAllowWhileIdle`) for exact time reminders and `WorkManager` for guaranteed background execution.
- **Centralized Security**: AndroidX `BiometricPrompt` supporting strong biometrics & device credentials, with automatic background re-locking (`ON_STOP`) and `FLAG_SECURE` window shielding.
- **Hardware-Accelerated Visualizations**: Native Compose `Canvas` rendering cubic Bézier curves, gradient area fills, and touch-coordinate geometry without third-party chart libraries.

---

## Testing & Verification

### Unit Tests (`app/src/test/java/com/example/expensetracker/`)
Run all unit tests via Gradle:
```bash
./gradlew testDebugUnitTest
```
- **`PeriodCalculatorTest`**: Calendar and salary-month interval calculations, salary period keys.
- **`BalanceCalculatorTest`**: Internal transfers, carried-forward balances, cash/bank balances.
- **`BudgetProgressCalculatorTest`**: Budget limits, percentages, fallback to latest budget.
- **`SpendingPacingCalculatorTest`**: Daily burn rate, safe daily allowance, and pacing status.
- **`PrayerTimesCalculatorTest`**: Astronomical solar coordinates, 5 prayer times, and Kaaba bearing.
- **`ZakahCalculatorTest`**: Nisab threshold comparisons and 2.5% wealth obligation.
- **`LocationHelperTest`**: Nearest preset city resolution and country code to currency mapping.
- **`ThemeContrastTest`**: Automated WCAG contrast across all 12 themes.

### Instrumented UI Tests (`app/src/androidTest/java/com/example/expensetracker/`)
Run on connected device/emulator:
```bash
./gradlew connectedDebugAndroidTest
```
- **`NewFeaturesUiTest`**:
  - `CategoryRow`: Verifies budget progress bar and `+ Set Budget` chip.
  - `CategoryBudgetsCard`: Verifies active category spending progress and empty state CTA.
  - `FinancialInsightsCard`: Verifies AI spending pacing badges and metrics.
  - `FinancialHealthScoreCard`: Verifies gamified tier and score calculation.
  - `TransactionDetailBottomSheet`: Verifies full transaction item details display.

---

## Build Requirements

- **Min SDK**: 26 | **Target SDK**: 35
- **Java**: 17+
- **Gradle**: 8.13+
- Compile with `./gradlew assembleDebug`
