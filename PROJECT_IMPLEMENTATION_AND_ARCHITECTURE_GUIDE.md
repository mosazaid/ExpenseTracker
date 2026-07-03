# ExpenseTracker Implementation and Architecture Guide

This document is the technical and business reference for the current app state after the recent refactors and feature expansion work.

## 1. Project Purpose (Business View)

ExpenseTracker helps a user manage personal money with:

- income and expense tracking by category and account (Cash/Bank)
- transfer tracking between Cash and Bank
- salary-cycle aware history (not only calendar month)
- reimbursement tracking ("someone owes me")
- category budgets with progress feedback
- recurring salary reminders
- CSV export for backup/share

### Core business rules

1. **Two month modes**
   - `CALENDAR`: month is 1st to last day
   - `SALARY`: month starts at the latest salary-period anchor and ends before next anchor

2. **Account-aware balances**
   - balances are computed separately for Cash and Bank
   - transfer affects both accounts (out from source, in to target)
   - opening balances are configurable and included in totals

3. **Reimbursement ("owed")**
   - an expense is owed only when explicitly marked `awaitingReimbursement = true`
   - "Dept" income can link to original expense
   - linked expenses are treated as reimbursed

4. **Budget period follows active month mode**
   - budget progress reflects the same period bounds shown in History

## 2. Current Architecture

The codebase uses Android MVVM with Compose + Room + Hilt, with recent decomposition to reduce coupling.

### 2.1 Layers

- **Presentation**: Compose screens + ViewModels
- **Domain**: calculators/business helpers (`PeriodCalculator`, `BalanceCalculator`, `BudgetProgressCalculator`, `CsvExporter`)
- **Data**: Room entities/DAO + repositories + preferences
- **Core utilities**: shared date/time helpers in `core/time`

### 2.2 ViewModel split (important improvement)

Previously one large `TransactionViewModel` handled many unrelated features. It has been split into:

- `HistoryViewModel`  
  History filters, period bounds, balances, reminder visibility inputs, grouped UI data fetches.

- `AddEditTransactionViewModel`  
  Add/edit expense/income form state, validation dependencies, salary prompt decisions, reimbursement linking.

- `TransferViewModel`  
  Transfer-only state and persistence, isolated from add/edit transaction form concerns.

- `BudgetViewModel`  
  Budget upsert operations from Categories screen based on active month period bounds.

- `SettingsViewModel`  
  Opening balances, CSV export trigger, recurring reminder settings controls.

## 3. Compose Design and Patterns Applied

### Applied patterns

- **Repository Pattern** for DB access abstraction
- **MVVM + state flows** for reactive screen state
- **Single source of truth** in Room + DataStore
- **Use-case style domain services** (calculators/exporter)
- **Navigation route centralization** via `presentation/navigation/AppRoutes.kt`
- **One-shot UI event streams** for navigation side-effects in Add/Edit and Transfer flows

### Compose-oriented improvements completed

- Removed nested scrolling conflicts (History and Categories are single scroll containers)
- Extracted/isolated feature state by screen
- Reduced route string duplication
- Introduced one-shot UI events to avoid direct imperative navigation scattered across callbacks

## 4. Major Features Implemented

## 4.1 Salary month vs calendar month

- Toggle in History
- Salary period uses salary anchors (`startsNewPeriod`)
- Salary period fallback messaging if anchors are missing

## 4.2 Week boundaries

- Week is Saturday to Friday

## 4.3 Transfers and balance checks

- dedicated transfer flow and transaction type
- insufficient funds handling with "allow negative" option

## 4.4 Reimbursement tracking

- `awaitingReimbursement` explicit flag on expense
- dept income optional note + optional linked expense
- owed filter and owed visual style in history item rendering

## 4.5 Budgets

- budget storage by exact period bounds (`periodStart`, `periodEnd`)
- budget progress displayed in History category dropdown
- budget set action from Categories screen

## 4.6 Recurring reminders

- recurring salary reminders in DB
- worker-based due checks + notification helper
- history banner reminder and dismiss persistence

## 4.7 Settings and export

- opening balances
- recurring reminder controls
- CSV export using FileProvider

## 5. Problems Encountered and Resolutions

This section documents real incidents and how they were fixed.

### Problem A: History scroll/layout issues

- **Symptom**: screen not scrolling correctly
- **Cause**: mixed fixed column + nested lazy list
- **Fix**: single scroll container strategy (single `LazyColumn`)

### Problem B: June/July salary-cycle split

- **Symptom**: transactions separated incorrectly across salary cycle
- **Cause**: anchor logic + grouping behavior mismatch
- **Fix**:
  - corrected salary anchor handling logic
  - fixed migration behavior around salary anchor defaults
  - aligned grouping behavior to expected weekly sectioning inside selected period

### Problem C: Startup crash on migration validation

- **Symptom**: Room migration validation crash on app start
- **Cause**: schema/entity mismatch (foreign key expectation mismatch during migration)
- **Fix**: aligned entity schema with what migration can safely produce, rebuilt migration path

### Problem D: All expenses marked as owed

- **Symptom**: every unreimbursed expense looked "owed"
- **Cause**: owed visual logic inferred from "not linked" instead of explicit business intent
- **Fix**:
  - added `awaitingReimbursement` DB field with migration
  - added explicit form toggle ("Someone owes me")
  - owed filter and styling now depend on explicit flag

### Problem E: Destructive budget migration

- **Symptom/risk**: migration dropped budgets table (possible data loss)
- **Fix**: replaced with copy-forward migration (`budgets_new` + transform + rename)

### Problem F: Recurring advance no-op

- **Symptom**: recurring due date not advanced in some save scenarios
- **Cause**: lookup only among currently due reminders
- **Fix**: fetch recurring by ID and advance deterministically

## 6. Database and Migration State

Current database version: **6**

High-level migration chain:

- `1 -> 2`: transfer/salary anchor/negative-balance columns + default category updates
- `2 -> 3`: salary anchor mark adjustments
- `3 -> 4`: salary anchor correction
- `4 -> 5`: reimbursement-link columns + budget schema transition
- `5 -> 6`: explicit `awaitingReimbursement`

## 7. How the Code Works (Runtime Flow)

## 7.1 Add/Edit transaction flow

1. Screen collects `AddEditTransactionUiState`
2. User edits fields and business toggles
3. ViewModel validates dependencies (balance, category semantics, salary prompts)
4. Save writes via repository
5. Optional recurring update and one-shot navigation event

## 7.2 Transfer flow

1. Screen collects `TransferUiState`
2. User selects from/to accounts, amount, date
3. ViewModel computes available balance (edit-aware)
4. Save creates/updates transfer transaction
5. One-shot back navigation event

## 7.3 History flow

1. User chooses period/filter mode
2. ViewModel computes bounds and summary state (`HistoryUiState`)
3. Transaction stream loaded for period
4. Grouping and rendering done with filter state + reimbursement map
5. Reminder card actions call ViewModel persist helpers

## 7.4 Settings flow

1. Opening balances read/write via DataStore
2. Export requests CSV from domain exporter and shares URI
3. Reminder controls update recurring entries

## 8. File-Level Source of Truth (Key Files)

- **Navigation**: `presentation/screens/MainScreen.kt`, `presentation/navigation/AppRoutes.kt`
- **History**: `presentation/screens/HistoryScreen.kt`, `presentation/viewModel/HistoryViewModel.kt`
- **Add/Edit**: `presentation/screens/AddTransactionScreen.kt`, `presentation/viewModel/AddEditTransactionViewModel.kt`
- **Transfer**: `presentation/screens/TransferScreen.kt`, `presentation/viewModel/TransferViewModel.kt`
- **Budgets**: `presentation/viewModel/BudgetViewModel.kt`, `domain/BudgetProgressCalculator.kt`
- **Settings/export**: `presentation/screens/SettingsScreen.kt`, `presentation/viewModel/SettingsViewModel.kt`, `domain/CsvExporter.kt`
- **Period/balance**: `domain/PeriodCalculator.kt`, `domain/BalanceCalculator.kt`, `core/time/DateUtils.kt`
- **Data model**: `data/database/entities/*`, `data/database/dao/*`, `data/database/Migrations.kt`

## 9. Remaining Architectural Opportunities

The code is substantially improved, but these are still worthwhile:

1. Move more screen callback orchestration into explicit use-cases
2. Replace display-name based system category detection with persistent category system key column
3. Expand automated tests around migration and period logic
4. Further split very large composables into smaller, testable UI units
5. Introduce immutable screen contract objects for each screen (already started with `HistoryUiState`)

## 10. Business Glossary

- **Salary month**: spending period anchored by salary start event
- **Dept income**: reimbursement income returned by other people
- **Owed expense**: expense marked as awaiting reimbursement
- **Reimbursed expense**: owed expense linked by a returning dept income
- **Period change**: net delta during selected period
- **Current balance**: all-time current amount including opening balances

---

If features evolve, update this document first, then implementation. It is the operational reference for architecture decisions, business rules, and known edge cases.
