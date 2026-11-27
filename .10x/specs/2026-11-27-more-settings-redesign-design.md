# More and Settings redesign

Date: 2026-11-27
Status: design approved in chat, including 4.1; this written spec is waiting for review. No app code has been written.
Base branch: `fix/recurring-auto-add` (not `master`).

## 1. Goal

Make More and Settings easy to scan, in English and Arabic, and fix the problems on these pages that can mislead the user.

Approach: grouped menus. The bottom tabs stay the same. There is no Room database change (version stays 23).

## 2. More screen

| Group | Items, in order | Style |
|---|---|---|
| Everyday money | Recurring, Budgets and categories, Loans, Debts, Savings vault | Tiles |
| Plan and travel | Trips, Currency exchange, Price memory, Emergency runway | Tiles |
| Islamic | Zakah, Sadaqah jar, Prayer and Qibla, Murabaha | Tiles |
| App | Settings, Help and guide | Rows |

- Tiles show an icon and a short name, with no subtitle. They use theme colors only.
- Three tiles per row. Two per row when a tile would be narrower than 100 dp or the font scale is 1.3 or more. Names must not be cut off.
- The footer keeps the offline storage note and shows the app version.
- Removed: the top card, the Mizani Pro banner, Notifications and Database browser.
- Mizani Pro is only hidden. The screen, its route and its code stay. A code search found the banner to be the only control that opens it.
- The menu is defined as one list of items (group, name, icon, destination), drawn by shared tile and row components.

## 3. Settings screen

### 3.1 Main list

| Group | Row | Summary shown on the row |
|---|---|---|
| General | Appearance | Mode and color theme |
| General | Language and region | Language and currency |
| Money | Accounts and balances | None |
| Money | Month and calculations | Salary month or calendar month |
| Alerts | Notifications | None |
| Data and security | App lock | A switch on this list; it locks the whole app |
| Data and security | Backup and restore | Last backup date, or "No backup yet" |
| Data and security | Import and export | None |

The version appears at the bottom. The guide is reached from More only.

### 3.2 Pages

- **Appearance:** System, Light or Dark; compact color theme choices with names.
- **Language and region:** language, currency with GPS detection, 12 or 24 hour time. Permission handling stays as it is today. Changing the language recreates the activity, as today; the user should stay on this page.
- **Accounts and balances:** see 3.3.
- **Month and calculations:** salary or calendar month, with a note that it affects the whole app; a row that opens the existing Calculation rules page; the wallet and vault transfer switch. The switch in History stays and changes the same setting.
- **Notifications:** the existing page, with the changes in section 4.
- **Backup and restore:** last backup date, Back up now, Restore (keeps the current confirmation), and a warning that receipt photos are not included.
- **Import and export:** report export (range and format), CSV import and its result, collapsible format help, blank template.

### 3.3 Correct balance

- Each account (Cash, Bank) shows its current balance formatted as money, with its own **Correct balance** button.
- The button opens a dialog with an empty amount field. The dialog shows the old amount, the new amount and the difference before saving.
- Accepted input is the same as today (`CurrencyUtils.cleanDecimalInput`). Blank or unreadable input shows an error and cannot be saved. It is never silently replaced by the old value.
- Confirm is disabled when the new amount equals the old one at the number of decimals shown.
- Saving changes only the chosen account. It still adjusts the opening balance and adds no transaction (`BalanceCalculator.setCurrentBalance`).

### 3.4 Last backup created

- Stored in DataStore as a per-device time. It is not part of the backup file, so a restore does not change it.
- Updated when the backup file is saved successfully, or when the share window opens for it. The app cannot confirm that sharing finished, so the label says "created".
- Creating the file and then closing the dialog does not update it.
- Report and template exports do not update it.

### 3.5 Blank template

Today "Download import template" exports every real transaction. The new template contains only the explanation comment lines, the column header, and the made-up example row written as a comment (starting with `#`). The importer already skips comment lines, so importing the unchanged template adds nothing. It must not read the transaction database.

### 3.6 Database viewer

- Debug builds: the row always appears at the bottom of Settings.
- Release builds: tapping the version 7 times shows the row. Another 7 taps hides it.
- The state is stored in DataStore on the device and is not part of the backup file. A short message confirms each change.
- This hides the viewer; it is not password protection.

### 3.7 Version

Both screens read `BuildConfig.VERSION_NAME`. This needs `buildConfig = true` in `app/build.gradle.kts`. The current `versionName` is `1.0`, so both screens will show 1.0 (today they show 1.1 and 1.0.0). This work does not change the version number.

## 4. Alert switches

All four alert preferences default to on, so only a switch the user turned off changes behavior.

| Switch | Change |
|---|---|
| Budget | When off, `BudgetAlertUseCase` posts no phone notification. The entry in the in-app alerts inbox is still created. |
| Salary and recurring | When off, `SalaryReminderWorker` posts no reminder. This covers all recurring reminders, including bills and subscriptions. |
| Debts | The row is hidden, because no code that sends a debt phone notification was found. The stored preference stays. Debt entries in the in-app alerts inbox are not changed. |
| Loans, daily | No change. |

- The warning that an automatic entry was blocked must still appear. It reuses `showSalaryReminderNotification`, so the check goes in the worker, not in `NotificationHelper`.
- The pinned-notification label "Sticky Loan & Debt Reminders" and its description change to mention loans only, in both languages.
- The Notifications row text must not promise debt reminders.
- The recurring switch controls phone notifications only. The question inside the app (4.1) appears whether the switch is on or off. Its label and description must say this.

### 4.1 Asking about due recurring items when the app opens

Already built, and kept: each recurring item has an "Add transaction automatically" switch, off by default. On: the app records the transaction itself. Off: it reminds the user, who adds it through a prefilled form.

Today the only question inside the app is a banner on Overview. It shows one item at a time, and closing it hides that item for that due date.

Change:
- When the app opens (after app lock, if on), a bottom sheet lists every due item that needs the user: reminder-only items, and automatic items that are still due because the account cannot cover them.
- Each item has **Add** (opens the prefilled form; the item advances only after saving) and **Skip this time** (asks for confirmation, advances one period, adds no transaction).
- **Later** closes the sheet. It appears again the next time the app is opened. It does not reappear while the user moves between screens.
- "Opened again" means the app starts afresh, or returns after at least 30 minutes in the background. A short switch to another app, or unlocking again, does not count. (Added during planning.)
- The app already shows a loan payment sheet on opening. It comes first, then this sheet; they are never stacked. (Added during planning.)
- The Overview banner shows how many items are due and reopens the sheet. It can no longer hide an item for good.
- Automatic items that can be recorded are not listed.
- Opening the app from a recurring phone notification goes to that item's form, as today, without the sheet on top of it.
- Several missed periods of a reminder-only item are handled one at a time.

Tests: which items are listed; Add advances only after saving; Skip advances one period and adds nothing; Later keeps items due; the recurring switch does not affect the list.

## 5. Code structure

- `MoreScreen` plus a menu list and its tile and row components.
- `SettingsScreen` becomes the main list. Each new page in 3.2 gets its own file and a type-safe route.
- Shared settings row components live in one place.
- The single settings ViewModel is split where a page has its own logic: accounts and balances, backup, import and export.
- The export-ready dialog and file saving are shared by the backup and export pages.
- GPS and file handling logic that is not UI moves out of the composables.
- Source files stay under 800 lines.

## 6. Text and layout

- All text on More, the Settings pages, their dialogs and messages is in `res/values` and `res/values-ar`, with matching keys.
- Layout uses start and end, so it mirrors in Arabic.
- Tiles, rows and switches have readable names for screen readers, and touch targets of at least 48 dp.

## 7. Errors

- Backup, report export and template export show a translated error message if they fail. The app must not crash.
- Import and restore keep their current result messages.
- Coroutine cancellation is rethrown, not reported as an error.

## 8. Testing

Write each test before the code it covers.

Unit tests:
- Every More destination appears exactly once; Notifications, Database browser and Pro are absent.
- Balance input: blank, unreadable, unchanged and valid values; only the chosen account changes.
- Blank template: header present, no real data, and importing it adds nothing.
- Last backup time: updates on successful save and on opening share, not otherwise.
- Version taps: the 7th tap shows the row; 7 more hide it.
- Budget off: no phone notification, inbox entry still created. Budget on: both.
- Recurring off: no reminder. Recurring on: reminder.
- New routes serialize (`AppDestinationsTest`).

On a device, in English, Arabic and large text: every More item opens its screen; each Settings page and Back; language change; backup, restore, export and import; balance correction; version taps in a release build.

Checks: the existing tests pass, the debug and release builds succeed, and Lint shows no new errors compared with today's 12.

Documents: update the More and Settings section of `QA_CHECKLIST.md`, and the More screen entries in `PROJECT_IMPLEMENTATION_AND_ARCHITECTURE_GUIDE.md`.

## 9. Installing on the phone

Export a backup before installing the new build, and keep receipt photos separately. If Android says the new build cannot be installed over the old one, stop. Do not uninstall to get around it without checking the backup first, because uninstalling deletes the app's data. This design adds two DataStore settings and no Room migration.

## 10. Not included

- Receipt photos in backups.
- Prayer alarm times.
- Real Pro billing, or removing the owner bypass.
- Debt phone notifications.
- The in-app debt review each salary month, with a chosen reminder period. Requested on 2026-11-27; see `2026-11-27-debt-review-design.md`.
- The "Excel" export, which writes an `.html` file.
- Fixing the 12 existing Lint errors.
- Translating screens outside More and Settings, or redesigning the Notifications and Calculation rules pages.
