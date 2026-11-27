# More and Settings Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Group More and Settings, fix the misleading behaviour on those pages, and ask about due recurring items when the app opens.

**Architecture:** Rules move out of composables and workers into plain classes with JVM tests. Settings pages are added one at a time as new routes; the old screen is replaced only when every control has a new home (Task 8), so the app keeps working after each task.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), type-safe Navigation, Hilt, DataStore, Room 23, WorkManager, JUnit 4, mockito-core, kotlinx-coroutines-test.

**Spec:** `.10x/specs/2026-11-27-more-settings-redesign-design.md` (read it with this plan).

## Global Constraints

- Branch `fix/recurring-auto-add`. Room stays at version 23: no entity or migration change.
- Commit only when the user has asked. Each "Checkpoint" below is a commit only in that case, with the message shown. Never stage unrelated files.
- Every new user-visible string goes in both `res/values/strings.xml` and `res/values-ar/strings.xml` with the same key. Use start/end, never left/right.
- Touch targets at least 48 dp. New source files stay under 800 lines.
- Never catch `CancellationException` without rethrowing it.
- Lint must report no new errors (the baseline is 12).
- Paths below are relative to `app/src/main/java/com/mousa/mizani/`; test paths to `app/src/test/java/com/mousa/mizani/`.
- Test command (PowerShell, repo root): `.\gradlew.bat testDebugUnitTest --tests "<class>"`. Build: `.\gradlew.bat assembleDebug`. Run both in the background.
- Before Task 1: run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat lintDebug` and record the results. Report any failure that already exists; do not fix it here.

## Review Focus

1. A backup restored on another phone carries this phone's "last backup" time or viewer unlock: neither may be written into the backup. (Test in Task 4.)
2. Balance input such as `.`, an empty string or a very long number: an error, never a silent change. (Task 5.)
3. Correcting an account whose balance is negative: the difference is correct and the other account is untouched. (Task 5.)
4. **Skip this time** tapped twice quickly: the item advances one period only. (Task 10.)
5. A recurring item paused, deleted or already recorded while the sheet is open: Skip does nothing. (Task 10.)

## Where each current Settings control goes

| Control today | New place | Task |
|---|---|---|
| Language, time format, currency, GPS detect | Language and region | 6 |
| Theme mode, color theme | Appearance | 6 |
| Cash and bank balance | Accounts and balances | 7 |
| Calculation rules, vault transfers, month mode (new here) | Month and calculations | 7 |
| Notifications | Index row | 8 |
| App lock | Index switch | 8 |
| Full backup and restore, restore result | Backup and restore | 8 |
| Report export, CSV import, format help, template | Import and export | 8 |
| Database browser, version | Index bottom | 8 |

---

### Task 1: Alert switches

**Files:**
- Create: `domain/usecase/RecurringReminderUseCase.kt`
- Modify: `domain/usecase/BudgetAlertUseCase.kt`, `data/worker/SalaryReminderWorker.kt`, `presentation/feature/settings/NotificationSettingsScreen.kt`, both `strings.xml`
- Test: `domain/usecase/AlertSwitchTest.kt`

**Interfaces:**
- Produces: `class RecurringReminderUseCase @Inject constructor(recurringRepository: IRecurringRepository, userPreferences: UserPreferences, notificationHelper: NotificationHelper) { suspend operator fun invoke(now: Date = Date()) }`

- [ ] **Step 1: Write failing tests** with mocked dependencies: `recurring switch off posts no reminder` (verify `showSalaryReminderNotification` never called), `recurring switch on reminds only reminder-only items` (one auto and one manual due item: exactly one call, for the manual one), `budget switch off keeps the inbox entry but posts no notification` (`showBudgetNotification` never; `postBudgetAlert` once), `budget switch on posts both`.
- [ ] **Step 2:** Run `AlertSwitchTest`. Expected: FAIL, `RecurringReminderUseCase` unresolved.
- [ ] **Step 3:** Implement the use case from the worker's current body, returning early when `salaryAlertsEnabled.first()` is false. The worker only calls it, keeps `Result.retry()` on failure and rethrows cancellation. In `BudgetAlertUseCase`, guard only `showBudgetNotification` with `budgetAlertsEnabled.first()`. Do not change `NotificationHelper`: the blocked auto-add warning reuses the salary notification.
- [ ] **Step 4:** Remove the debt row and its collected state from the screen; keep the preference and ViewModel storage. Change `sticky_loan_title`/`sticky_loan_desc` to mention loans only. Make `alert_salary_title`/`alert_salary_desc` say that the switch controls phone notifications for all recurring items and that the app still asks inside the app. Both languages.
- [ ] **Step 5:** Run `AlertSwitchTest` and `UseCaseAutoRecurringTest`. Expected: PASS.
- [ ] **Checkpoint:** `fix(alerts): make budget and recurring switches work and hide debt switch`

### Task 2: Blank import template

**Files:**
- Modify: `core/export/TransactionExportRow.kt` (`CsvImportFormat`), `presentation/feature/settings/SettingsViewModel.kt` (`exportImportTemplate`)
- Test: `domain/CsvImportTemplateTest.kt`

**Interfaces:**
- Produces: `fun CsvImportFormat.template(): String` (a member of the object; pure, so it cannot read the database).

- [ ] **Step 1: Write failing tests:** `template has exactly one non-comment line and it is the header` (`assertEquals(listOf(CsvImportFormat.HEADER), lines.filter { it.isNotBlank() && !it.startsWith("#") })`), `example row is present as a comment` (a line equals `"# " + CsvImportFormat.EXAMPLE_ROW`), `importing the unchanged template adds nothing` (real `CsvImporter` with mocked repositories: `inserted == 0`, `updated == 0`, `errors` empty, `insertTransaction` never called).
- [ ] **Step 2:** Run. Expected: FAIL, `template` unresolved.
- [ ] **Step 3:** Implement `template()`. `exportImportTemplate()` writes it with the same BOM, file name and share flow, and no longer calls `csvExporter`.
- [ ] **Step 4:** Run the new test and `CsvImporterTest`. Expected: PASS.
- [ ] **Checkpoint:** `fix(export): make the import template blank instead of exporting all data`

### Task 3: BuildConfig and database viewer unlock rules

**Files:**
- Create: `core/debug/DatabaseViewerAccess.kt`
- Modify: `app/build.gradle.kts` (`buildFeatures { buildConfig = true }`), `data/preferences/UserPreferences.kt`
- Test: `core/DatabaseViewerAccessTest.kt`

**Interfaces:**
- Produces: `const val VERSION_TAPS_REQUIRED = 7`; `class VersionTapCounter { fun tap(): Boolean }` (true on every 7th tap, then restarts); `fun isDatabaseViewerVisible(isDebugBuild: Boolean, unlocked: Boolean): Boolean`; `UserPreferences.databaseViewerUnlocked: Flow<Boolean>` (default false, key `database_viewer_unlocked`); `suspend fun setDatabaseViewerUnlocked(unlocked: Boolean)`.

- [ ] **Step 1: Write failing tests:** `six taps do nothing`, `seventh tap fires`, `fourteenth tap fires again`, `debug build always shows the viewer`, `release shows it only when unlocked`.
- [ ] **Step 2:** Run. Expected: FAIL, unresolved.
- [ ] **Step 3:** Implement. The UI arrives in Task 8.
- [ ] **Step 4:** Run the test and `assembleDebug`. Expected: PASS, `BuildConfig` generated.
- [ ] **Checkpoint:** `feat(settings): add database viewer unlock rules and BuildConfig`

### Task 4: Last backup time and backup errors

**Files:**
- Create: `data/preferences/DeviceOnlyPreferences.kt`, `presentation/feature/settings/backup/BackupViewModel.kt`
- Modify: `data/preferences/UserPreferences.kt`, `presentation/feature/settings/SettingsViewModel.kt` (only `ExportShareRequest`), both `strings.xml`
- Test: `data/DeviceOnlyPreferencesTest.kt`, `presentation/BackupViewModelTest.kt`

**Interfaces:**
- Produces: `object DeviceOnlyPreferences { val KEYS: Set<String> = setOf("last_backup_created_at", "database_viewer_unlocked"); fun withoutDeviceOnly(all: Map<String, String>): Map<String, String> }`; `UserPreferences.lastBackupCreatedAt: Flow<Long?>`; `suspend fun setLastBackupCreatedAt(millis: Long)`.
- Produces: `ExportShareRequest.isFullBackup: Boolean = false`.
- Produces: `BackupViewModel` with `val lastBackupCreatedAt: StateFlow<Long?>`, `val exportRequest: StateFlow<ExportShareRequest?>`, `val errors: SharedFlow<Int>` (string resource), `val restoreResult: SharedFlow<FullBackupRestoreResult>`, `fun createBackup()`, `fun onDelivered(request: ExportShareRequest, now: Long = System.currentTimeMillis())`, `fun restore(uri: Uri)`, `fun clearExportRequest()`.

`exportAllPreferencesMap()` currently copies every key into the backup, so it must return `withoutDeviceOnly(result)`. `importAllPreferencesMap` restores only listed keys; do not add these two. The old screen keeps using `SettingsViewModel`'s backup methods until Task 8 deletes them.

- [ ] **Step 1: Write failing tests:** `backup map excludes device-only keys and keeps others` (input has both keys plus `theme_mode`; result keys equal `setOf("theme_mode")`), `delivering a full backup records the time` (`setLastBackupCreatedAt(1000L)` once), `delivering a report records nothing`, `creating without delivering records nothing`, `failed backup emits an error and no request` (manager throws `IOException`; one `R.string.export_failed`; `exportRequest.value == null`).
- [ ] **Step 2:** Run both. Expected: FAIL.
- [ ] **Step 3:** Implement. Add `export_failed` in both languages.
- [ ] **Step 4:** Run the new tests and `FullBackupCodecTest`. Expected: PASS.
- [ ] **Checkpoint:** `feat(backup): track last backup and report backup errors`

### Task 5: Safe balance correction

**Files:**
- Create: `domain/calculator/BalanceCorrection.kt`, `presentation/feature/settings/accounts/AccountBalancesViewModel.kt`
- Test: `domain/BalanceCorrectionTest.kt`, `presentation/AccountBalancesViewModelTest.kt`

**Interfaces:**
- Produces: `sealed interface BalanceCorrection { data object Invalid : BalanceCorrection; data object Unchanged : BalanceCorrection; data class Change(val old: Double, val new: Double, val difference: Double) : BalanceCorrection }`
- Produces: `fun evaluateBalanceCorrection(input: String, current: Double, decimals: Int): BalanceCorrection`. `input` is already filtered by `CurrencyUtils.cleanDecimalInput`; `decimals` is 3 when the currency's `decimalPlaces` is 3, else 2 (as `formatCurrency` shows); equality compares both values rounded to `decimals`; unparsable or non-finite input is `Invalid`.
- Produces: `AccountBalancesViewModel` with `val balances: StateFlow<Map<AccountType, Double>>` (CASH and BANK), `fun refresh()`, `fun apply(account: AccountType, change: BalanceCorrection.Change)`.

- [ ] **Step 1: Write failing tests:** `empty, dot and 400 nines are Invalid`; `"007" against 5.0 is Change with new 7.0`; `"10.001" against 10.0 is Unchanged at 2 decimals and Change at 3`; `current -50.0 with "20" has difference 70.0`; `apply for cash calls setCurrentBalance for CASH only`.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement. Writing still goes through `BalanceCalculator.setCurrentBalance`.
- [ ] **Step 4:** Run these and `BalanceCalculatorTest`. Expected: PASS.
- [ ] **Checkpoint:** `feat(settings): add explicit balance correction`

### Task 6: Shared rows, routes, Appearance, Language and region

**Files:**
- Create: `presentation/feature/settings/components/SettingsRows.kt`, `presentation/feature/settings/appearance/AppearanceSettingsScreen.kt`, `presentation/feature/settings/region/LanguageRegionSettingsScreen.kt`, `presentation/feature/settings/region/CurrencyLocationDetector.kt` (GPS permission and detection flow)
- Modify: `presentation/navigation/AppDestinations.kt`, `presentation/feature/main/MainScreen.kt`, both `strings.xml`
- Test: `presentation/navigation/AppDestinationsTest.kt`

**Interfaces:**
- Produces routes, all `@Serializable object`: `AppearanceSettings`, `LanguageRegionSettings`, `AccountBalancesSettings`, `MonthCalculationSettings`, `BackupSettings`, `ImportExportSettings`. Register each in the task that creates its screen.
- Produces composables: `SettingsGroupHeader(title: String)`, `SettingsNavRow(title: String, summary: String?, icon: ImageVector, onClick: () -> Unit)`, `SettingsSwitchRow(title: String, summary: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit)` (whole row toggles, `Role.Switch`), `SettingsChoiceRow<T>(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit)` (wraps with large text).

- [ ] **Step 1:** Add the six routes to `AppDestinationsTest` with `assertNotNull(json.encodeToString(...))`. Run; expected FAIL.
- [ ] **Step 2:** Add the routes. Run; expected PASS.
- [ ] **Step 3:** Build the rows and both screens using `SettingsViewModel`. Localize "System", "Light", "Dark", "Auto-detect via GPS" and both location messages. Color themes become one wrapping row of swatches, each with its name and a selected mark.
- [ ] **Step 4:** `assembleDebug`. Expected: success.
- [ ] **Checkpoint:** `feat(settings): add appearance and language pages`

### Task 7: Accounts and balances; Month and calculations

**Files:**
- Create: `presentation/feature/settings/accounts/AccountBalancesScreen.kt`, `presentation/feature/settings/accounts/CorrectBalanceDialog.kt`, `presentation/feature/settings/month/MonthCalculationSettingsScreen.kt`
- Modify: `SettingsViewModel.kt` (add `fun setMonthMode(mode: MonthMode)`), `MainScreen.kt`, both `strings.xml`

**Interfaces:** Consumes Task 5 (`evaluateBalanceCorrection`, `AccountBalancesViewModel`) and Task 6 (rows, routes).

- [ ] **Step 1:** Accounts page: two cards showing `CurrencyUtils.formatCurrency(balance)`, each with **Correct balance**. The dialog starts empty, shows old, new and difference, shows an error for `Invalid` (not before the first keystroke) and enables confirm only for `Change`.
- [ ] **Step 2:** Month page: month mode choice with an "affects the whole app" note, calculation rules row, vault transfer switch. Leave the History switch as it is.
- [ ] **Step 3:** `assembleDebug`. Expected: success.
- [ ] **Checkpoint:** `feat(settings): add accounts and month pages`

### Task 8: Backup, Import and export, and the new Settings index

**Files:**
- Create (under `presentation/feature/settings/`): `backup/BackupSettingsScreen.kt`, `transfer/ImportExportSettingsScreen.kt`, `transfer/ImportExportViewModel.kt`, `transfer/ImportFormatCard.kt`, `transfer/ExportModels.kt` (`ExportScope`, `ExportFormat`, `ExportShareRequest`), `components/ExportDeliveryHost.kt`, `SettingsIndexViewModel.kt`
- Rewrite: `presentation/feature/settings/SettingsScreen.kt`
- Modify: `SettingsViewModel.kt` (delete members with no caller left), `MainScreen.kt`, both `strings.xml`
- Test: `presentation/ImportExportViewModelTest.kt`

**Interfaces:**
- Produces: `@Composable fun ExportDeliveryHost(request: ExportShareRequest?, snackbarHostState: SnackbarHostState, onDelivered: (ExportShareRequest) -> Unit, onClear: () -> Unit)`: the ready dialog, save to Downloads with document-picker fallback, view and share. `onDelivered` fires after a successful save or copy, or after the share window starts; not for View, dismiss or failure.
- Produces: `ImportExportViewModel` with `exportRequest`, `errors: SharedFlow<Int>`, `importResult`, `fun export(scope: ExportScope, format: ExportFormat)`, `fun exportTemplate()`, `fun importCsv(uri: Uri)`, `fun clearExportRequest()`.
- Consumes: Tasks 2 to 7.

- [ ] **Step 1: Write failing tests:** `failed report export emits export_failed and no request`, `failed template export emits export_failed and no request`. Run; expected FAIL.
- [ ] **Step 2:** Implement `ImportExportViewModel` from the current report, import and template code, with the error handling. Run; expected PASS.
- [ ] **Step 3:** Build `ExportDeliveryHost` from the current dialog code; localize "Share File" and the share chooser title. Build the Backup page: last backup date formatted for the locale or "No backup yet", receipt photo warning, current restore confirmation and result messages, localized "Unknown error". Build Import and export.
- [ ] **Step 4:** Rewrite the index exactly as spec 3.1. Summaries: theme mode and palette; language and currency code; month mode; last backup date. At the bottom: `BuildConfig.VERSION_NAME` (clickable, feeding `VersionTapCounter`, with a message when shown or hidden) and the database viewer row when `isDatabaseViewerVisible(BuildConfig.DEBUG, unlocked)`.
- [ ] **Step 5:** Check every row of the "Where each current Settings control goes" table. Search the settings folder for `Text("` and move remaining literal text to resources. Check that no Notifications text on More or Settings promises debt reminders.
- [ ] **Step 6:** Run `testDebugUnitTest` and `assembleDebug`. Expected: both succeed.
- [ ] **Checkpoint:** `feat(settings): replace long settings screen with grouped pages`

### Task 9: Grouped More screen

**Files:**
- Create: `presentation/feature/main/more/MoreMenu.kt`, `presentation/feature/main/more/MoreTiles.kt`
- Modify: `presentation/feature/main/MoreScreen.kt`, both `strings.xml`
- Test: `presentation/MoreMenuTest.kt`

**Interfaces:**
- Produces: `enum class MoreGroup { EVERYDAY, PLAN_TRAVEL, ISLAMIC, APP }`; `data class MoreEntry(val group: MoreGroup, @StringRes val labelRes: Int, val icon: ImageVector, val destination: Any)`; `object MoreMenu { val entries: List<MoreEntry> }`; `fun moreColumns(availableWidthDp: Float, spacingDp: Float, fontScale: Float): Int`: 2 when `fontScale >= 1.3f` or `(availableWidthDp - 2 * spacingDp) / 3 < 100f`, else 3.

- [ ] **Step 1: Write failing tests:** `destinations are in spec order per group` (compare each group with the exact list in spec section 2), `each destination appears once`, `notifications, database browser and subscription are absent`, `three columns at 328 dp with 8 dp spacing and font 1.0`, `two columns at font 1.3`, `two columns when a tile would be 99 dp`, `three columns at exactly 100 dp`.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement the list, the column rule and the tiles (theme colors, wrapping labels, merged semantics). App entries are rows. Footer: offline note and `BuildConfig.VERSION_NAME`. Remove the top card and Pro banner; keep the subscription route. Remove `app_version_label`, `more_screen_hub_title` and `more_screen_hub_desc` from both files once nothing uses them.
- [ ] **Step 4:** Run the test and `assembleDebug`. Expected: PASS.
- [ ] **Checkpoint:** `feat(more): group tools into a compact menu and hide Pro`

### Task 10: Due recurring sheet

**Files:**
- Create: `domain/usecase/RecurringAccountBalance.kt`, `domain/usecase/DueRecurringAttentionUseCase.kt`, `domain/usecase/SkipRecurringOccurrenceUseCase.kt`, `core/startup/OpeningPromptSession.kt`, `presentation/feature/recurring/due/DueRecurringViewModel.kt`, `presentation/feature/recurring/due/DueRecurringSheet.kt`
- Modify: `domain/usecase/ExecuteAutoRecurringUseCase.kt` (and its test setup), `MainActivity.kt`, `MainScreen.kt`, `presentation/feature/overview/OverviewScreen.kt`, `OverviewViewModel.kt`, both `strings.xml`
- Test: `domain/usecase/DueRecurringTest.kt`, `core/OpeningPromptSessionTest.kt`

**Interfaces:**
- Produces: `class RecurringAccountBalance @Inject constructor(balanceCalculator, walletCalculator, depositCalculator) { suspend operator fun invoke(item: RecurringTransaction): Double }`, moved unchanged from `ExecuteAutoRecurringUseCase.availableBalance`, which then uses it.
- Produces: `data class DueRecurringItem(val recurring: RecurringTransaction, val isBlocked: Boolean)`
- Produces: `DueRecurringAttentionUseCase.invoke(now: Date = Date()): List<DueRecurringItem>`: every reminder-only due item, plus automatic items where `planAutoAdd(item, now, balance)` has an empty `toRecord` and a non-null `blocked`. Oldest due first. The notification switch is not read.
- Produces: `SkipRecurringOccurrenceUseCase.invoke(id: Long, expectedDueMillis: Long): Boolean`: re-reads the item; returns false and writes nothing if it is missing, inactive or `nextDueDate.time != expectedDueMillis`; otherwise saves `advanced()`. Calls are serialized with a `Mutex` in a singleton.
- Produces: `@Singleton class OpeningPromptSession { val recurringDismissed: StateFlow<Boolean>; fun dismissRecurring(); fun reopenRecurring(); fun onBackgrounded(nowMillis: Long); fun onForegrounded(nowMillis: Long) }` and `const val REOPEN_AFTER_MILLIS = 30 * 60 * 1000L`.

- [ ] **Step 1: Write failing tests.** Attention: `reminder-only items are listed`, `affordable auto item is not listed`, `unaffordable auto expense is listed as blocked`, `auto income is not listed`, `recurring notification switch off does not change the list`. Skip: `advances one period and inserts no transaction`, `second call with the same expected date returns false`, `missing or inactive item writes nothing`. Session: `later stays dismissed after 29 minutes`, `returns after 30 minutes`, `foreground without a background keeps state`.
- [ ] **Step 2:** Run. Expected: FAIL.
- [ ] **Step 3:** Implement the classes. Run these tests and `UseCaseAutoRecurringTest`; expected PASS.
- [ ] **Step 4:** Sheet: name, amount, due date, a blocked explanation where needed, **Add** (navigate `AddTransaction(recurringId = id)`; saving already advances the item), **Skip this time** with confirmation, **Later**. Swipe and Back equal Later. Show it only on dashboard tabs, only when the list is not empty and not dismissed, never while the loan sheet shows, and not before a pending `initialRecurringId` navigation has happened. Reload on returning to a dashboard tab. A failed load or skip shows a translated message and leaves the item due.
- [ ] **Step 5:** `MainActivity.onStop` calls `onBackgrounded` unless `isChangingConfigurations`; `onStart` calls `onForegrounded`.
- [ ] **Step 6:** The Overview banner shows how many items need attention and calls `reopenRecurring()`. Remove `isRecurringBannerDismissed`, `dismissRecurringBanner` and the close button; keep the stored dismissal key so old backups restore. Leave the other use of `dueReminders.size` on Overview behaving as before.
- [ ] **Step 7:** Run `testDebugUnitTest` and `assembleDebug`. Expected: both succeed.
- [ ] **Checkpoint:** `feat(recurring): ask about due items when the app opens`

### Task 11: Documents and final verification

**Files:** Modify `QA_CHECKLIST.md`, `PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md` (More and Settings entries, and the biometric gate placement at line 1036), `.10x/status.md` (repo root paths).

- [ ] **Step 1:** Update the documents.
- [ ] **Step 2:** Run `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, `lintDebug`. Expected: tests and builds succeed; no Lint error beyond those recorded before Task 1.
- [ ] **Step 3:** Compare string keys: both `strings.xml` files have the same set.
- [ ] **Step 4:** Device or emulator, in English, Arabic and large text: every More item opens; every Settings page and Back; language change stays on its page; backup, restore (test data only), report export, import, template; balance correction; sheet order after the loan sheet, Add, Skip, Later, the 30-minute return, opening from a notification, app lock on. If no device is available, report this step as not done.
- [ ] **Step 5:** Version taps in a release build. The project has no release signing setup, so the release APK cannot be installed as built; without a signed build report this as not verified.
- [ ] **Step 6:** Before the user installs on their phone: ask them to export a backup and keep receipt photos. Never advise uninstalling to force an install.
- [ ] **Checkpoint:** `docs: update checklist and guide for the new More and Settings`
