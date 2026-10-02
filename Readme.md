# ExpenseTracker — Documentation

A modern Android expense tracking app built with **Kotlin**, **Jetpack Compose**, **Room**, **Hilt**, and **MVVM**.

## Overview

ExpenseTracker helps you track personal money with salary-aware months, Cash/Bank/Wallet accounts, reimbursements, multi-split transactions, category budgets, AI spending pacing, offline Islamic finance tools (Zakah & Prayer/Qiblah), and local backup via import/export.

---

## What's New (Phase 0 – Phase 5 Releases)

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
