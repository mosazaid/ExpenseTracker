# ExpenseTracker Implementation and Architecture Guide

This document is the technical and business reference for the current app state through Room DB version **17** (October 2026).

## 1. Project Purpose (Business View)

ExpenseTracker helps a user manage personal money with:

- income and expense tracking by category and account (**Cash / Bank / Wallet**)
- transfer tracking between Cash and Bank
- **monthly wallet** (pocket money) moves scoped to the open salary month
- salary-cycle aware history (not only calendar month)
- reimbursement and debt tracking (**Lent vs Borrowed / Repayment**)
- **multi-item transaction splitting** across subcategories and individual debtors (`TransactionSplit`)
- **offline receipt attachments** with full image zoom viewer (`ReceiptStorageHelper`)
- **persistent category budgets** across month rollovers with visual progress bars
- **AI spending pacing analysis** (`SpendingPacingCalculator`) with daily burn rates and safe daily allowances
- **gamified financial health score** (`FinancialHealthScoreCard`) tracking savings, debt, and budget adherence
- **offline Islamic finance tools**: Zakah calculator with 85g gold Nisab comparison, 5 daily prayer times, and Qiblah compass
- **location intelligence** (`LocationHelper`) for GPS-based auto-currency and nearest preset cities
- category budgets with real-time in-app warnings and push alerts (at 85% and 100% capacity)
- **daily 9:00 PM expense reminder** with dual scheduling (**AlarmManager** + **WorkManager**)
- **configured recurring loans** with once-per-day bottom sheet reminders and salary preservation
- persistent **in-app notification center** (`app_alerts`) with real-time badge count on the top app bar
- **notification settings** with individual toggle controls for each alert channel
- **"More" hub navigation screen** consolidating Debts, Loans, Categories, DB Browser, Settings, Prayer & Zakah
- **time picker** support alongside date selection for exact timestamp logging
- **CSV import** and **styled CSV/PDF export** for backup/share
- **Arabic and English** UI with RTL layout support
- **Biometric app lock** on launch and resume
- developer-style **database browser** for inspecting local data
- **interactive multi-charts and 3-month category comparison pie charts**
- **usage-based category sorting** and **subcategory filtering with "Other"**
- **3-decimal currency formatting** (`#,##0.000 JOD`) with thousands separator
- **distinct visual styling for category and date** in transaction history items
- **universal date formatting (`DateUtils`)** with standard format pattern constants and extensions

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

4. **Reimbursement & Debt Separation**
   - **Expense with Debt category (`debtType = LENT`)**: User gave money to someone. They owe the user.
   - **Income with Debt category (`debtType = BORROWED` or Repayment)**: User received money returning a loan, or user took a debt from someone.
   - Debts can be filtered by debtor name, settled with one-click actions, and carried across month rollovers.

5. **Configured Loans & Preserved Salary Balance**
   - Recurring loans (e.g., car payments, mortgages) are configured in `configured_loans`.
   - Each month, a payment record is generated in `monthly_loan_payments`.
   - On the first app open of a new month / salary period, a bottom sheet (`LoanReminderBottomSheet`) prompts once per day to record or dismiss payments without double-counting against base salary.

6. **Category Budget Limits & In-App Alerts**
   - Evaluated during transaction entry and in background jobs.
   - **85% limit reached**: Warning alert stored in DB and displayed via notification/banner.
   - **100% limit reached**: Critical alert stored in DB and displayed via notification/banner.
   - Alerts persist in `app_alerts` until dismissed (`isDismissed = 1`).

7. **Daily 9:00 PM Reminder**
   - Scheduled every day at 21:00 local time.
   - Queries database for expenses recorded on the current day; if count > 0, skips notification silently.
   - Dual-engine fallback: `AlarmManager` for immediate execution + `WorkManager` for guaranteed background execution.

8. **Salary period start**
   - salary income can set `startsNewPeriod = true`
   - optional `carriedForwardBalance` brings previous period net into the new month

9. **Budget period follows active month mode**
   - budget progress uses the same period bounds as History list filters

10. **Sub-description & Subcategories**
    - optional detail on income/expense (e.g. items bought at a store)
    - hierarchical subcategories linked to parent categories, with an "Other" option in filters for unassigned transactions

11. **Biometric app lock**
    - enabled by default via `UserPreferences.biometricLockEnabled`
    - `BiometricGate` wraps `MainScreen` in `MainActivity`
    - accepts **BIOMETRIC_STRONG** or **DEVICE_CREDENTIAL** (PIN/pattern)
    - on failure/cancel: lock screen + error message; no app content rendered
    - re-locks on `Lifecycle.Event.ON_STOP` (background)
    - `FLAG_SECURE` while locked (blocks screenshots)

## 2. Current Architecture

Android MVVM with Compose + Room + Hilt.

### 2.1 Layers

- **Presentation**: Compose screens + ViewModels + custom Canvas charts
- **Domain**: calculators, exporters, importers, inspectors
- **Data**: Room entities/DAO + repositories + DataStore preferences
- **Core**: `core/time/DateUtils`, `core/locale/LocaleHelper`, `core/security/BiometricAuthManager`

### 2.2 ViewModels

| ViewModel | Responsibility |
|-----------|----------------|
| `HistoryViewModel` | Period bounds, month summary, filters, grouping, wallet year summary, delete guards |
| `AddEditTransactionViewModel` | Add/edit income/expense form, sub-description, salary/dept flows, budget warning evaluation |
| `TransferViewModel` | Cash ↔ Bank transfers |
| `WalletViewModel` | Wallet ↔ Cash/Bank moves, validation, closed-period guards |
| `BudgetViewModel` | Category budgets from Categories screen |
| `SettingsViewModel` | Balances, language, export/import, salary reminder controls |
| `StatisticsViewModel` | Period totals, 3-month multi-chart stats, 3-month category expense shares |
| `DatabaseBrowserViewModel` | Table listing and filtered raw queries |
| `BiometricLockViewModel` | Unlock state, error message, lock on background |
| `DebtsViewModel` | Track lent vs borrowed debts, debtor filters, settle actions, month rollover |
| `LoansViewModel` | Configured loan definitions, monthly payment schedules, salary protection |
| `AlertsViewModel` | Persistent budget alerts and reminder history, dismissal actions |

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
| `AlarmScheduler` | Dual-mode daily 9 PM alarm scheduling (`setExactAndAllowWhileIdle` vs `setAndAllowWhileIdle`) |
| `NotificationHelper` | Android 13/14 notification channel setup, reminder notifications, budget breach warnings |
| `DailyExpenseReminderReceiver` | BroadcastReceiver waking at 9 PM to inspect today's expenses and emit reminder if 0 expenses |
| `DailyExpenseReminderWorker` | WorkManager failsafe executing at 9 PM to protect against OEM background kills |
| `LoanReminderWorker` | Periodic background worker evaluating active loans on new salary/calendar periods |

### 2.4 Compose patterns

- Repository pattern + MVVM + StateFlow
- Central routes: `presentation/navigation/AppRoutes.kt`
- One-shot UI events for post-save navigation (`AddEditUiEvent.NavigateHistory`)
- History organized as **TabRow**: Overview | Transactions | Filters
- Universal Top Bar: `AppTopBar` with unread notification badge on all screens
- Navigation bar with 4 bottom destinations: **Overview**, **History**, **Statistics**, **More**
- `SwipeableTransactionItem` wraps `TransactionItem` with `SwipeToDismissBox`
- `BiometricGate` + `BiometricLockScreen` gate all financial UI until authenticated
- `LoanReminderBottomSheet` prompts once per day on month rollover / salary arrival

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

### 3.9 Statistics Overview
- Income, Expense, **Wallet**, Balance for selected period
- Periodic overview bar charts

### 3.10 History month summary (unified)
Single calculator pass (`BalanceCalculator.buildMonthFinancialSummary`):
- **Activity this month** — income, expense, saved (period net)
- **Your money now** — matches Add screen Cash/Bank cards
- **Cash & bank change this month**
- **Wallet this month** (salary mode)

### 3.11 Category Usage-Based Sorting
- Added `getCategoriesSortedByUsage()` in `CategoryDao` and `CategoryRepository`.
- Computes transaction counts per category across all history via SQL `LEFT JOIN` and orders by `COALESCE(txn_count, 0) DESC, name ASC`.
- Used in `AddEditTransactionViewModel` and `HistoryViewModel` so frequently used categories appear at the top of category dropdowns.

### 3.12 Subcategory Management & "Other" Filter
- Hierarchical subcategories stored in Room `sub_categories` table (`SubCategory.kt`).
- In `HistoryScreen`, when filtering by a category that has subcategories, an **"Other"** filter item is displayed with its accurate spending total (`Other — $XX.XX`).
- In `HistoryGrouping`, selecting `SUBCATEGORY_OTHER` filters transactions where `subDescription` is null, blank, or equals `"Other"`.
- If a category has no subcategories at all, the subcategory dropdown remains hidden.

### 3.13 3-Month Multi-Chart & Category Comparison Pie Charts
- In `StatisticsViewModel`, `calculateThreeMonthStats()` computes 3-month metrics (`MonthlyBreakdown`, `CategoryExpenseShare`, `ThreeMonthStats`) for both Calendar and Salary month modes.
- `ThreeMonthMultiChart`: Custom Compose `Canvas` supporting switchable views:
  - **Bar Chart**: Grouped bars for Income (Green), Expense (Red), and Wallet (Purple) across the 3 months.
  - **Trend Line**: Smooth cubic Bézier spline with gradient area fill and glowing indicator dots.
  - **Combined**: Side-by-side grouped bars with trend spline overlay.
  - Summary metrics: 3-month average monthly spend and Month-over-Month (MoM) spending change badges.
- `CategoryPieChart`: Interactive Donut / Pie Chart:
  - Touch-interactive slices displaying category icon, name, percentage, and exact amount in the center hole.
  - Month selection chips: `[All 3 Months]`, `[Month 1]`, `[Month 2]`, `[Month 3]` with actual month labels.
  - Detailed category legend below with color swatches, icons, names, percentage badges, and amounts.

### 3.14 Overview Remaining Balance Breakdown
- Overview clearly differentiates between:
  - **Remaining Income**: Current period net income (`monthIncome - monthExpense - walletBalance`).
  - **Total Remaining**: Total money across accounts (`currentBalances.cash + currentBalances.bank`).
  - **Last Month Remaining**: Balance remaining from prior periods (`totalRemaining - remainingIncome`).
- A dedicated **Remaining Balance Breakdown** card in Overview shows the exact addition:
  `Last month remaining` + `Current remaining income` = `Total remaining (Cash + Bank)`.

### 3.15 Add Transaction Simplified Types
- The transaction type switcher in Add Transaction is restricted to **Expense** and **Income** only.
- `WALLET_MOVE` is removed from Add Transaction because wallet movements are performed via the dedicated "Move to/from Wallet" button in Overview.

### 3.16 3-Decimal Currency Formatting & Input Controls
- `CurrencyUtils.formatCurrency(amount)` and `formatAmountOnly(amount)` format numbers with 3 decimal places (`#,##0.000 JOD`) and comma grouping for thousands (e.g. `1,250.500 JOD`).
- Added extension functions: `Double.formatCurrency()` and `Double.formatAmount()`.
- Form inputs across all screens enforce `singleLine = true`, `KeyboardType.Decimal` for amounts with input sanitization (`CurrencyUtils.cleanDecimalInput`), and appropriate `ImeAction.Next` / `ImeAction.Done`.

### 3.17 Transaction History Item Layout & Visual Differentiation
- Redesigned `TransactionItem` with a clear 3-row layout:
  - **Row 1**: Transaction description and account badge (`Cash` / `Bank`) aligned side-by-side.
  - **Row 2**: Category name in `MaterialTheme.colorScheme.onSurfaceVariant` (darker grey) with `FontWeight.Medium`, separated by a subtle dot `•`, followed by transaction date in `MaterialTheme.colorScheme.outline` (lighter grey).
  - **Row 3**: Subcategory chip (if present) on the left and the formatted amount on the right on the same line.
- The redundant trash delete icon on the card is hidden to eliminate visual clutter, while keeping full swipe-to-delete support.

### 3.18 Universal Date Formatting (`DateUtils`)
- `core/time/DateUtils` and `presentation/theme/DateUtils` provide standardized pattern constants:
  `PATTERN_DEFAULT`, `PATTERN_FULL_DATE`, `PATTERN_SHORT_DATE`, `PATTERN_MONTH_DAY`, `PATTERN_MONTH_YEAR`, `PATTERN_TIME_24H`, `PATTERN_TIME_12H`, `PATTERN_DATE_TIME`, `PATTERN_ISO`.
- `DateUtils.format(date, pattern, locale)` and `DateUtils.parse(dateString, pattern, locale)` allow formatting/parsing in any pattern needed.
- `Date` extension functions: `Date.format(pattern)`, `Date.formatDate()`, `Date.formatTime()`, `Date.formatDateTime()`, `Date.formatMonthYear()`.

---

## 4. Database and Migration State

**Current Room Database Version: 17**

| Migration | Summary |
|-----------|---------|
| 1→2 | Transfer/salary anchor columns, initial categories |
| 2→3, 3→4 | Salary anchor corrections |
| 4→5 | Reimbursement columns; budget period schema |
| 5→6 | `awaitingReimbursement` flag |
| 6→7 | Default account migration to `BANK` |
| 7→8 | Additional default expense categories |
| 8→9 | `carriedForwardBalance` support |
| 9→10 | `subDescription` item details |
| 10→11 | `SubCategory` table + `subCategoryId` column on `Transaction` |
| 11→12 | `debt_records` & `configured_loans` tables with `MonthlyLoanPayment` |
| 12→13 | `app_alerts` persistent notification center schema |
| 13→14 | `receipt_uri` column on `Transaction` for image attachments |
| 14→15 | `isDebtSettled` & `settledAt` on `Transaction` for debt management |
| 15→16 | Expanded shopping subcategories: `Chicken & Meat` and `Snacks & Sweets` |
| 16→17 | `transaction_splits` table with foreign key cascade deletion for multi-item split transactions |

### Core Entities & Key Fields

- **`Transaction` (`transactions`)**:
  `id, amount, description, subDescription, date, type, categoryId, subCategoryId, accountType, toAccountType, startsNewPeriod, carriedForwardBalance, allowNegativeBalance, linkedExpenseId, debtorNote, awaitingReimbursement, isDebtSettled, settledAt, receiptUri, createdAt, updatedAt`
- **`TransactionSplit` (`transaction_splits`)**:
  `id, transactionId (FK), subCategoryId, amount, description, debtorName, debtType, createdAt`
- **`SubCategory` (`sub_categories`)**:
  `id, categoryId (FK), nameEn, nameAr, isDefault`
- **`ConfiguredLoan` (`configured_loans`)**:
  `id, loanName, lenderName, totalPrincipal, monthlyInstallment, remainingAmount, dueDayOfMonth, startDate, endDate, isActive`
- **`MonthlyLoanPayment` (`monthly_loan_payments`)**:
  `id, loanId (FK), periodYearMonth, paymentAmount, isPaid, paidDate, transactionId`
- **`AppAlert` (`app_alerts`)**:
  `id, alertType, title, message, categoryId, thresholdPercentage, isDismissed, createdAt`

---

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

1. **Overview**: `buildMonthSummary`, reminder, wallet year card, remaining balance breakdown
2. **Transactions**: filtered stream → `HistoryGrouping` → swipe delete with confirm
3. **Filters**: period/type/category/subcategory (including "Other"); drives same transaction query

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

---

---

## 6. Deep Dive: Every Calculation Method & Mathematical Algorithm

This section outlines the exact mathematical formulas, astronomical models, and financial equations implemented across the domain layer.

### 6.1 Astronomical Solar Position & Prayer Calculation Engine (`PrayerTimesCalculator.kt`, `PrayerCalculationMethod.kt`)

The prayer calculation engine is an offline, autonomous astronomical calculator based on the **Jean Meeus / NOAA standard solar position models**:

1. **Julian Date ($JD$) Computation**:
   $$JD = 367 \cdot Y - \left\lfloor \frac{7 \cdot \left(Y + \left\lfloor \frac{M + 9}{12} \right\rfloor\right)}{4} \right\rfloor + \left\lfloor \frac{275 \cdot M}{9} \right\rfloor + D + 1721013.5$$
   Where $Y$, $M$, $D$ represent the Gregorian year, month, and day.

2. **Solar Coordinate Calculations**:
   - Elapsed Julian centuries since J2000.0: $T = \frac{JD - 2451545.0}{36525.0}$
   - Solar Mean Anomaly: $g = 357.529^\circ + 0.98560028^\circ \cdot d$
   - Solar Mean Longitude: $q = 280.459^\circ + 0.98564736^\circ \cdot d$
   - Apparent Ecliptic Longitude: $l = q + 1.915^\circ \sin(g) + 0.020^\circ \sin(2g)$
   - Obliquity of the Ecliptic: $e = 23.439^\circ - 0.00000036^\circ \cdot d$
   - Solar Declination ($\delta$): $\sin(\delta) = \sin(e) \cdot \sin(l)$
   - Right Ascension ($RA$): $\tan(RA) = \frac{\cos(e) \cdot \sin(l)}{\cos(l)}$
   - Equation of Time ($EqT$ in hours): $EqT = \frac{q}{15^\circ} - \frac{RA}{15^\circ}$

3. **Solar Noon (Midday / Dhuhr)**:
   $$\text{Solar Noon} = 12.0 - EqT - \frac{\text{Longitude}}{15^\circ} + \text{TimezoneOffsetHours}$$
   Dhuhr occurs when the sun crosses the local meridian (solar noon).

4. **Hour Angle ($\omega$ or $HA$) Formula**:
   $$\cos(HA) = \frac{\sin(\alpha) - \sin(\text{Latitude}) \cdot \sin(\delta)}{\cos(\text{Latitude}) \cdot \cos(\delta)}$$
   Where $\alpha$ is the sun's altitude angle relative to the celestial horizon.

5. **Sunrise & Sunset Elevation Horizon Dip Correction**:
   Standard solar disc semi-diameter + atmospheric refraction is $-0.8333^\circ$.
   For an observer at elevation $h$ (meters above sea level), the apparent horizon dips according to:
   $$\text{Dip} = 0.0347^\circ \cdot \sqrt{h}$$
   $$\alpha_{\text{rise/set}} = - (0.8333^\circ + \text{Dip})$$
   *Example: For Amman ($h \approx 780\text{m}$), $\text{Dip} \approx 0.97^\circ$, giving $\alpha \approx -1.80^\circ$, precisely matching official Ministry of Awqaf & TimesPrayer Jordan timings.*

6. **Asr Shadow Ratio (Shafi'i / Standard)**:
   The sun's altitude for Asr occurs when an object's shadow equals its height plus the shadow at noon:
   $$\text{Noon Shadow} = \tan|\text{Latitude} - \delta|$$
   $$\alpha_{\text{Asr}} = \operatorname{arccot}(1.0 + \text{Noon Shadow})$$

7. **Iterative Two-Pass Precision**:
   Because declination $\delta$ and Equation of Time $EqT$ continuously change throughout the day, the engine performs a two-pass calculation:
   - *Pass 1*: Initial time estimate using solar noon coordinates.
   - *Pass 2*: Recomputes exact solar coordinates ($\delta, EqT$) at the exact calculated instant of Fajr, Sunrise, Asr, Maghrib, and Isha.

8. **12 Global Juridical Standards (`PrayerCalculationMethod`)**:
   | Method | Fajr Twilight Angle ($\alpha_f$) | Isha Rule |
   |--------|----------------------------------|-----------|
   | **Jordan (Iftaa')** | $18.0^\circ$ | $18.0^\circ$ |
   | **Umm al-Qura (Makkah)** | $18.5^\circ$ | Fixed 90 min after Maghrib (120 in Ramadan) |
   | **Egyptian Survey Authority** | $19.5^\circ$ | $17.5^\circ$ |
   | **Turkey (Diyanet)** | $18.0^\circ$ | $17.0^\circ$ |
   | **Karachi (Univ. Islamic Sciences)** | $18.0^\circ$ | $18.0^\circ$ |
   | **ISNA (North America)** | $15.0^\circ$ | $15.0^\circ$ |
   | **Muslim World League (MWL)** | $18.0^\circ$ | $17.0^\circ$ |
   | **Dubai (UAE Islamic Affairs)** | $18.2^\circ$ | $18.2^\circ$ |
   | **Kuwait (Awqaf)** | $18.0^\circ$ | $17.5^\circ$ |
   | **Qatar (Awqaf)** | $18.0^\circ$ | Fixed 90 min after Maghrib |
   | **MUIS / JAKIM (Southeast Asia)** | $20.0^\circ$ | $18.0^\circ$ |
   | **Tehran (Geophysics)** | $17.7^\circ$ | $14.0^\circ$ |

---

### 6.2 Great-Circle Qiblah Bearing & Sensor Fusion (`CompassSensorManager.kt`)

1. **Spherical Trigonometry Forward Azimuth (Qiblah Bearing)**:
   Calculates the initial great-circle bearing from observer coordinates $(\phi_1, \lambda_1)$ to the Holy Kaaba in Makkah $(\phi_2 = 21.4225^\circ\text{ N}, \lambda_2 = 39.8262^\circ\text{ E})$:
   $$\Delta\lambda = \lambda_2 - \lambda_1$$
   $$\theta = \operatorname{atan2}\left(\sin(\Delta\lambda), \; \cos(\phi_1)\tan(\phi_2) - \sin(\phi_1)\cos(\Delta\lambda)\right)$$
   $$\text{Bearing}_{\text{Qiblah}} = (\theta \cdot \frac{180^\circ}{\pi} + 360^\circ) \pmod{360^\circ}$$

2. **Hardware Sensor Fusion & Fallback**:
   - **Primary**: Android `Sensor.TYPE_ROTATION_VECTOR` — hardware quaternion fusion of gyroscope, accelerometer, and magnetometer.
   - **Fallback**: Dual sensor array (`Sensor.TYPE_ACCELEROMETER` + `Sensor.TYPE_MAGNETIC_FIELD`) passed to `SensorManager.getRotationMatrix` and `SensorManager.getOrientation`.

3. **Shortest-Path Circular Low-Pass Exponential Smoothing**:
   Prevents dial jitter and handles the $0^\circ \leftrightarrow 360^\circ$ boundary seamlessly:
   $$\Delta = ((\text{TargetAzimuth} - \text{CurrentAzimuth} + 540^\circ) \pmod{360^\circ}) - 180^\circ$$
   $$\text{Azimuth}_{\text{Smoothed}} = (\text{CurrentAzimuth} + \alpha \cdot \Delta + 360^\circ) \pmod{360^\circ}$$
   With smoothing coefficient $\alpha = 0.15$.

4. **Kaaba Target Needle & Haptic Trigger**:
   - Needle angle on screen: $\theta_{\text{needle}} = (\text{Bearing}_{\text{Qiblah}} - \text{Azimuth}_{\text{device}} + 360^\circ) \pmod{360^\circ}$.
   - Tactile feedback + glowing emerald halo activates when $|\theta_{\text{needle}}| \le 3.5^\circ$ or $\ge 356.5^\circ$.

---

### 6.3 Financial Spending Pacing & Velocity Engine (`SpendingPacingCalculator.kt`)

The financial intelligence engine computes real-time pacing metrics relative to the active budget period:

1. **Active Period Boundaries**:
   - **Calendar Month**: From 1st day to last day of current month.
   - **Salary Month**: Starts on latest salary transaction date ($D_{\text{start}}$). Natural end date ($D_{\text{end}}$) is $D_{\text{start}} + 1\text{ month} - 1\text{ day}$, preventing premature cycle end when no next salary is recorded yet.

2. **Baseline Budget Formulation**:
   Preserves pocket-money / wallet reserves as untouchable savings:
   $$\text{BaselineBudget} = \max(0.0, \; \text{TotalIncome} - \text{WalletReserved})$$

3. **Temporal & Budget Progress Ratios**:
   $$\text{TimeElapsedRatio} = \frac{\text{DayOfPeriod}}{\text{TotalPeriodDays}}$$
   $$\text{BudgetConsumedRatio} = \frac{\text{TotalExpense}}{\text{BaselineBudget}}$$

4. **Spending Velocity Ratio ($V$)**:
   $$V = \frac{\text{BudgetConsumedRatio}}{\text{TimeElapsedRatio}}$$
   - $V < 0.70$: Super Saver (under-spending).
   - $0.70 \le V \le 1.05$: Optimal / On Track.
   - $1.05 < V \le 1.30$: Moderate Burn.
   - $V > 1.30$: High Velocity Warning.

5. **Safe Daily Spend Remaining**:
   $$\text{SafeDailySpend} = \max\left(0.0, \; \frac{\text{BaselineBudget} - \text{TotalExpense}}{\text{DaysRemaining}}\right)$$

6. **Projected Period Outcome**:
   $$\text{DailySpendRate} = \frac{\text{TotalExpense}}{\text{DayOfPeriod}}$$
   $$\text{ProjectedTotalSpend} = \text{DailySpendRate} \cdot \text{TotalPeriodDays}$$
   $$\text{ProjectedDifference} = \text{BaselineBudget} - \text{ProjectedTotalSpend}$$
   Positive values indicate projected surplus; negative indicates projected deficit.

7. **Early-Cycle Damping Mechanism**:
   During the first 3 days of a cycle ($\text{DayOfPeriod} \le 3$), normal upfront expenses (groceries, bills) naturally cause a temporary velocity spike. The engine activates **Early Cycle Damping**, softening deficit alerts until sufficient daily spending data accumulates.

---

### 6.4 Gamified Financial Health Score Engine (`FinancialHealthScoreCard.kt`)

Computes a composite financial health score from $0$ to $100$:
$$\text{Score} = (40 \cdot S_{\text{savings}}) + (35 \cdot S_{\text{budget}}) + (25 \cdot S_{\text{debt}})$$

Where:
- **Savings Component ($S_{\text{savings}} \in [0, 1]$)**:
  $$S_{\text{savings}} = \left(\frac{\text{TotalIncome} - \text{TotalExpense}}{\text{TotalIncome}}\right) \cdot \frac{1}{0.30}$$
  (Scores $1.0$ when saving 30% or more of income).
- **Budget Adherence Component ($S_{\text{budget}} \in [0, 1]$)**:
  Percentage of active category budgets currently under 100% capacity.
- **Debt & Loan Component ($S_{\text{debt}} \in [0, 1]$)**:
  Deducts 25 points for every overdue or unpaid loan installment in the current period.

---

### 6.5 Zakah Calculation Engine (`ZakahCalculator.kt`)

1. **Nisab Threshold**:
   Value equivalent to **85 grams of 24k gold**:
   $$\text{Nisab} = 85.0 \cdot \text{GoldPricePerGram}$$

2. **Net Zakat-Eligible Wealth ($W_{\text{net}}$)**:
   $$W_{\text{net}} = (\text{Cash} + \text{Bank} + \text{BusinessGoods} + \text{GoldSilverInvestments}) - \text{ImmediateLiabilities}$$

3. **Zakah Obligation**:
   $$\text{ZakahDue} = \begin{cases} 0.0 & \text{if } W_{\text{net}} < \text{Nisab} \\ W_{\text{net}} \cdot 0.025 & \text{if } W_{\text{net}} \ge \text{Nisab} \text{ and Hawl (1 lunar year) is met} \end{cases}$$

---

### 6.6 Multi-Account Balance State Machine (`BalanceCalculator.kt`)

1. **Liquid Balances**:
   $$\text{CashBalance} = \text{CashOpening} + \sum \text{CashIncome} - \sum \text{CashExpense} \pm \Delta\text{Transfers} \pm \Delta\text{WalletMoves}$$
   $$\text{BankBalance} = \text{BankOpening} + \sum \text{BankIncome} - \sum \text{BankExpense} \mp \Delta\text{Transfers} \mp \Delta\text{WalletMoves}$$
   $$\text{TotalAvailable} = \text{CashBalance} + \text{BankBalance}$$

2. **Transfer Neutrality Invariant**:
   For any transfer between Cash and Bank:
   $$\Delta\text{Cash} + \Delta\text{Bank} = 0$$
   Transfers never affect net income, net expense, or period savings.

---

### 6.7 Multi-Item Split Transactions & Debtor Attribution (`TransactionSplit`)

Room DB version 17 introduces parent-child transactional decomposition:
$$\text{TotalTransactionAmount} = \sum_{i=1}^{N} \text{SplitAmount}_i$$
- When calculating subcategory totals (`subCategorySpendMap`), the engine inspects each split item. If a transaction has splits, its base amount is bypassed and individual split amounts are credited to their respective `subCategoryId`.
- Split lines with `debtType = LENT` allocate debt balances to `debtorName`, preventing lent amounts from polluting personal expense categories.

---

## 7. Deep Dive: Every Architecture & Framework Pattern Used

### 7.1 Presentation Layer: Jetpack Compose + MVVM
- **Declarative Reactive UI**: Single unidirectional data flow (UDF). Screens observe immutable `StateFlow<UiState>` emitted by ViewModels.
- **Material 3 Design System**: Theme with dynamic color scheme, dark/light surface containers, and tailored typographic scales.
- **Hardware-Accelerated Canvas Visualizations**: `ThreeMonthMultiChart`, `CategoryPieChart`, and the Qiblah Compass dial use custom Jetpack Compose `Canvas` with native Bézier spline paths and touch-coordinate geometry.

### 7.2 Type-Safe Navigation Compose (Navigation 2.8.8)
- Eliminates string-based URL routes. Routes are modeled as Kotlin `@Serializable` objects and data classes:
  ```kotlin
  @Serializable object Overview
  @Serializable object History
  @Serializable data class AddTransaction(val transactionId: Long? = null, val recurringId: Long? = null)
  ```
- Compile-time argument type validation via `toRoute<T>()`.

### 7.3 Dependency Injection Architecture (Google Hilt)
- **Application Level**: `@HiltAndroidApp` initializes singletons and lifecycle containers.
- **ViewModels**: Injected via `@HiltViewModel` and `@Inject constructor`.
- **Module Provision**:
  - `DatabaseModule`: Singleton `AppDatabase`, DAOs.
  - `LocationModule`: `LocationHelper` with `@ApplicationContext`.
  - `SchedulerModule`: `AlarmScheduler` for exact alarms.

### 7.4 Offline-First Persistence Layer (Room DB v17)
- **Entities & Tables**: Normalized relational tables with SQLite indexes.
- **Foreign Key Cascade Deletion**: Deleting a parent `Transaction` automatically deletes all associated `TransactionSplit` records via SQLite foreign keys (`onDelete = ForeignKey.CASCADE`).
- **Reactive Queries**: DAOs expose Kotlin `Flow<List<T>>` for automatic UI updates when underlying tables mutate.
- **Lightweight Preferences**: AndroidX DataStore for user preferences (time format, biometric lock, selected prayer method).

### 7.5 Dual-Engine Background Scheduling Architecture
- **Primary: Android `AlarmManager`**:
  Uses `setExactAndAllowWhileIdle()` to wake the device at the exact scheduled time for:
  - 9:00 PM Daily Expense Reminders (`DailyExpenseReminderReceiver`)
  - 5 Daily Prayer Reminders (`PrayerReminderReceiver`)
- **Fallback: Android `WorkManager`**:
  `DailyExpenseReminderWorker` and `LoanReminderWorker` scheduled with battery-friendly constraints to guarantee execution even if OEM power managers delay exact alarms.

### 7.6 Centralized Runtime Permissions & Rationale Architecture
- `PermissionHelper` unifies runtime permission requests (`CAMERA`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`).
- Distinguishes initial request vs rationale vs permanent denial (directing users to system app settings via `ACTION_APPLICATION_DETAILS_SETTINGS`).

### 7.7 Hardware-Backed Biometric Security Architecture
- `BiometricAuthManager` using AndroidX `BiometricPrompt` supporting `BIOMETRIC_STRONG` and `DEVICE_CREDENTIAL`.
- `BiometricGate` Compose wrapper:
  - Automatically re-locks on `Lifecycle.Event.ON_STOP` when app enters background.
  - Sets `FLAG_SECURE` to block system screenshots and task-switcher previews while locked.

---

## 8. Architecture Evaluation & Recommendations

### 8.1 Architectural Strengths
1. **Separation of Concerns**: Domain calculators (`PeriodCalculator`, `BalanceCalculator`, `WalletCalculator`, `BudgetProgressCalculator`, `PrayerTimesCalculator`, `SpendingPacingCalculator`) encapsulate complex mathematical, astronomical, and business rules with zero Android UI dependencies.
2. **Single Source of Truth**: Balance calculations derive income, expenses, transfer impact, and available balances in a single pass over Room entity snapshots, eliminating desynchronization.
3. **Reactive UI State**: Extensive use of Kotlin `StateFlow` and Compose `produceState`/`collectAsState` ensures database mutations trigger atomic, flicker-free UI updates.
4. **Hardware-Accelerated Custom Visualizations**: Native Compose `Canvas` drawing with Bézier splines, gradient brushes, and arc trigonometry avoids bulky external charting libraries.

### 8.2 Recommended Future Enhancements
1. **Extract Domain UseCases**: Introduce domain UseCase classes (`operator fun invoke()`) to streamline ViewModel responsibilities as feature complexity expands.
2. **Feature Modularization**: Split the single `:app` module into `:core:model`, `:core:database`, `:core:astronomy`, and `:feature:*` modules for isolated compile units.

---

## 7. Unit Testing Architecture & Test Catalog

Unit tests execute on the JVM (no emulator required) using JUnit 4, Kotlin Coroutines Test (`runTest`), and Mockito.

### 7.1 Test Execution Command
```bash
./gradlew testDebugUnitTest
```

### 7.2 Test Catalog (28 Tests Across 9 Suites)

| Test Suite | Focus Area | Key Verifications |
|------------|------------|-------------------|
| `PeriodCalculatorTest` | Domain Time/Bounds | Day, week, calendar month, year boundaries; salary fallback when salary income anchor is missing. |
| `BalanceCalculatorTest` | Financial Balances | Income/expense accumulation, transfer neutrality (`transferImpact.cash + bank == 0`), wallet move accounting. |
| `WalletCalculatorTest` | Pocket Money Rules | Validates allowed moves (`CASH/BANK` ↔ `WALLET`) and rejects invalid moves (`WALLET` ↔ `WALLET`, `CASH` ↔ `BANK`). |
| `BudgetProgressCalculatorTest` | Budget Monitoring | Spending percentage, `isNearLimit` threshold (>=90%), `isOverBudget` (>100%), missing budget handling. |
| `HistoryGroupingComprehensiveTest` | Transaction Filtering & Sorting | All `TransactionTypeFilter` modes (`ALL`, `INCOME`, `EXPENSE`, `WALLET`, `OWED`), category filtering, `DESC`/`ASC` sorting. |
| `HistoryGroupingOtherSubcategoryTest` | Subcategory "Other" Filter | Matches null, blank, and `"Other"` subcategories; excludes non-matching named subcategories. |
| `MonthSummaryRemainingTest` | Overview Balance Math | Verifies `previousMonthRemaining + remainingIncome == totalRemaining` and matches `currentBalances.total`. |
| `StatisticsCalculationsTest` | 3-Month Analytics | Monthly breakdown net balance, 3-month total aggregations, MoM spending percentage calculation, category expense shares. |
| `CoreUtilsTest` | Core Formatting & Time | Currency formatting with 3 decimals (`#,##0.000 JOD`) and thousands grouping, DateUtils custom pattern formatting (`PATTERN_FULL_DATE`, `PATTERN_ISO`), safe parsing, invalid rejection, and start/end bounds. |

---

## 8. Notifications, Reminders & Background Scheduling

The application implements a resilient, dual-engine background scheduling architecture designed to operate reliably across Android 8.0 (API 26) through Android 14+ (API 34+).

### 8.1 Dual-Engine Scheduling Strategy
1. **Primary Engine — `AlarmManager`**:
   - Manages the exact daily 9:00 PM (`21:00`) reminder.
   - On device boot (`BOOT_COMPLETED`), the schedule is automatically re-registered.
   - Wakes `DailyExpenseReminderReceiver`, which queries `TransactionDao` to verify if any expenses were recorded today. If >= 1 expense exists, the notification is skipped.

2. **Secondary Engine — `WorkManager`**:
   - `DailyExpenseReminderWorker` is scheduled as an unconstrained periodic worker.
   - Acts as a safety net against aggressive OEM process termination (e.g., Xiaomi MIUI, Samsung OneUI, Huawei) where standard alarms might be blocked.
   - Checks both expense counts and daily notification dispatch state to prevent duplicate notifications.

3. **Loan Reminder Worker**:
   - `LoanReminderWorker` runs periodically to ensure monthly payment schedules are generated in Room for all active configured loans.

### 8.2 AlarmManager Behavior: Exact vs Inexact & Doze Mode Delay

Understanding how `AlarmManager` behaves across Android versions and device states is critical:

| Scenario / State | Granted Permission (`SCHEDULE_EXACT_ALARM`) | Denied / Revoked Permission (Fallback) |
|---|---|---|
| **API Mechanism** | `setExactAndAllowWhileIdle()` | `setAndAllowWhileIdle()` |
| **Delivery Accuracy** | Exact at `21:00:00` local time | Batched / Deferred by OS power manager |
| **Active Screen / Foreground** | Instant (0 delay) | ~0 to 5 minutes delay |
| **Screen Off / Normal Standby** | Instant (0 delay) | ~5 to 15 minutes delay |
| **Light Doze Mode** | Instant (0 delay, breaks through idle) | Deferred to next Light Doze maintenance window (~15 to 30 min) |
| **Deep Doze Mode** *(stationary, screen off, on battery)* | Instant (0 delay, breaks through deep idle) | **Batched into Deep Doze maintenance windows**. Maintenance intervals start at 15 minutes and exponentially back off to 1 hour, 2 hours, 4 hours, or until the user moves/unlocks the phone. |
| **Android 13/14+ Behavior** | In Android 14, `SCHEDULE_EXACT_ALARM` is restricted. If revoked, app automatically falls back to `setAndAllowWhileIdle()` without crashing. | `POST_NOTIFICATIONS` runtime permission is requested at startup. If denied, notifications are suppressed at the OS level while internal scheduling continues harmlessly. |

---

## 9. Navigation Reference (Type-Safe Compose Navigation)

```kotlin
// presentation/navigation/AppDestinations.kt
// Bottom-tab destinations (singletons)
@Serializable object Overview
@Serializable object History
@Serializable object Statistics
@Serializable object More                        // 4th bottom nav tab: hub for settings & tools

// Secondary / push destinations (singletons)
@Serializable object Transfer
@Serializable object Wallet
@Serializable object Loans
@Serializable object Alerts
@Serializable object Debts
@Serializable object Recurring
@Serializable object Categories
@Serializable object Settings
@Serializable object DatabaseBrowser
@Serializable object Onboarding

// Parameterized destinations (data classes)
@Serializable
data class AddTransaction(
    val transactionId: Long = -1L,
    val recurringId: Long = -1L
)

@Serializable
data class EditTransfer(val transactionId: Long)

@Serializable
data class EditWallet(val transactionId: Long)
```

**Architecture**:
- Powered by `kotlinx.serialization` with `androidx.navigation:navigation-compose:2.8.8`.
- In `NavHost`: `composable<T> { ... }`.
- Extracting route arguments: `val route = backStackEntry.toRoute<AddTransaction>()`.
- Navigation: `navController.navigate(AddTransaction(transactionId = 5L))` or `navController.navigate(Overview)`.
- Tab state check: `currentBackStack?.destination?.hasRoute<Overview>()`.
- Compile-time safety guarantees zero routing typos or missing argument bundle crashes.

---

## 10. CSV Import Format (Canonical)

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

---

## 11. File-Level Source of Truth

| Area | Files |
|------|-------|
| Navigation | `MainScreen.kt`, `presentation/navigation/AppDestinations.kt`, `MoreScreen.kt` |
| Top Bar & Alerts | `AppTopBar.kt`, `AlertsScreen.kt`, `AlertsViewModel.kt`, `AppAlert.kt`, `AppAlertDao.kt`, `AlertRepository.kt` |
| History | `HistoryScreen.kt`, `HistoryViewModel.kt`, `HistoryGrouping.kt`, `TransactionItem.kt`, `SwipeableTransactionItem` |
| Overview | `OverviewScreen.kt`, `HistoryViewModel.kt`, `AccountBalanceCards.kt` |
| Add/Edit | `AddTransactionScreen.kt`, `AddEditTransactionViewModel.kt` |
| Debts | `DebtsScreen.kt`, `DebtsViewModel.kt`, `Transaction.debtType`, `Transaction.isDebtSettled` |
| Loans | `LoansScreen.kt`, `LoansViewModel.kt`, `LoanReminderBottomSheet.kt`, `ConfiguredLoan.kt`, `MonthlyLoanPayment.kt`, `ConfiguredLoanDao.kt`, `MonthlyLoanPaymentDao.kt`, `LoanRepository.kt` |
| Scheduling & Notifications | `AlarmScheduler.kt`, `DailyExpenseReminderReceiver.kt`, `DailyExpenseReminderWorker.kt`, `LoanReminderWorker.kt`, `NotificationHelper.kt` |
| Transfer | `TransferScreen.kt`, `TransferViewModel.kt` |
| Wallet | `WalletScreen.kt`, `WalletViewModel.kt`, `WalletCalculator.kt` |
| Statistics | `StatisticsScreen.kt`, `StatisticsViewModel.kt`, `ThreeMonthMultiChart.kt`, `CategoryPieChart.kt`, `StatisticsBarChart.kt` |
| Settings | `SettingsScreen.kt`, `SettingsViewModel.kt` |
| Export/Import | `CsvExporter.kt`, `CsvImporter.kt`, `StyledExcelExporter.kt`, `PdfTransactionExporter.kt`, `TransactionExportLoader.kt` |
| DB browser | `DatabaseBrowserScreen.kt`, `DatabaseBrowserViewModel.kt`, `DatabaseInspector.kt` |
| Locale | `LocaleHelper.kt`, `UserPreferences.kt`, `values/strings.xml`, `values-ar/strings.xml` |
| Biometric lock | `BiometricAuthManager.kt`, `BiometricGate.kt`, `BiometricLockScreen.kt`, `BiometricLockViewModel.kt`, `MainActivity.kt` |
| Period/Balance | `PeriodCalculator.kt`, `BalanceCalculator.kt`, `core/time/DateUtils.kt` |
| Data | `entities/*`, `dao/*`, `Migrations.kt` (v13), `AppDatabase.kt` |

---

## 12. Business Glossary

- **Salary month**: spending period anchored by salary start event
- **Wallet**: monthly pocket-money bucket (salary-mode); not the same as “all cash”
- **Debt (Lent)**: `debtType = LENT` expense where money was lent to someone and is owed to user
- **Debt (Borrowed / Repayment)**: `debtType = BORROWED` or debt income repaid to user
- **Configured Loan**: recurring liability (mortgage, auto, personal) tracked across months
- **Monthly Loan Payment**: month-specific instance of a configured loan (`monthly_loan_payments`)
- **App Alert**: persistent notification stored in `app_alerts` with unread/dismissed state
- **Owed expense**: `awaitingReimbursement = true`
- **Sub-description**: optional item-level detail under main description
- **Remaining income**: net savings generated during the active period (`income - expense - wallet`)
- **Total remaining**: available cash + bank across all accounts right now
- **Carry-forward**: previous period net added when starting new salary month

---

**Maintenance rule**: When adding features, update this guide and `RELEASE_NOTES.md` first, then `QA_CHECKLIST.md` and `Readme.md`.
