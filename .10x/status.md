# Status (2026-11-27)

| Feature | Phase | State |
|---|---|---|
| more-settings-redesign | Built | All tasks, the final review and its fix wave are done and committed; device testing is outstanding |
| debt-review | Design | Spec approved; not built; plan comes next |

Branch: `fix/recurring-auto-add`. Detailed task reports are in `.superpowers/sdd/` on the development machine only (not in git).

After the final fix wave: 349 unit tests, 0 failures; `assembleDebug` succeeds; the same 12 Lint errors as the baseline. The fix wave was not run on a device.

## Verified (Task 11, before the final fix wave)
- `testDebugUnitTest --rerun-tasks`: 337 tests, 0 failures, 0 errors (baseline 137).
- `assembleDebug` and `assembleRelease` succeed. The release APK is unsigned.
- `lintDebug` fails as before: 12 errors, the same as the baseline (none new). Warnings 189 -> 202, all in `values/strings.xml`: 12 newly unused strings and 4 more plural candidates, while 3 old unused-string warnings are gone. No new warnings in Kotlin or other files.
- `values` and `values-ar` have the same 705 string keys; no placeholder mismatch.
- Emulator, debug build, test data (API 36.1, read-only AVD), partial: all 15 More items open and return; Settings pages and Back; English/Arabic switch stays on its page; large text in Arabic and at 320 dp in English; Cash correction (comma rejected, one account changed); blank template and its import (0 added); CSV import; CSV/Excel/PDF files created; backup (cancel does not record, save does); restore of a real backup and rejection of non-backup files; app lock stays off without a screen lock; due sheet after the loan sheet, Add, Skip, Later, short background return; notification launch simulated with intent extras.

## Not verified
- Anything on a physical phone.
- Signed release build: version taps and any R8 effect (no release signing setup).
- App lock on (including file pickers and the lock before the due sheet).
- The 30-minute background return (unit tests only), a real tapped notification, posted notifications and alert switches (permission declined).
- GPS currency detection, Share and View targets, opening the exported files, screen reader, Arabic-digit typing, restore messages about app lock or failed settings.

## Open problems found in Task 11
1. Fixed in the final fix wave (unit-tested, not on a device): a restore of a real export now resets a Cash/Bank opening balance or Sadaqah jar balance that the file lacks (it was unset then). Other absent settings still keep their current value. Rule and history check: `final-fix-report.md`.
2. Fixed in the final fix wave (not on a device): the Overview list scrolls to the due banner when it appears while the list is at the top.
3. Before a currency is chosen: Settings shows USD (English) while Overview, the due sheet and the form show JOD; the Recurring page showed either. Switching language changes the Settings and balance-page currency. (Not fixed.)
4. Fixed in the final fix wave (unit-tested): `DateUtils` display formatters follow the current language.

## Final fix wave (2026-11-27)
- Done: `alert_salary_desc` and `recurring_form_already_handled` (en, ar); `RecurringScreen` passes the expected due date. Report: `.superpowers/sdd/2026-11-27-more-settings-redesign-plan/final-fix-report.md`.

## Still waiting (from the ledger)
- Balance dialog: check too many decimals before Unchanged, on the typed text; restore the error color for "balance unavailable".
- Unused old strings.
- A Save cancelled after commit shows the "already handled" message; its text now points to History, but the case itself is unchanged.
- Minor items listed in `progress.md`.

## Before installing on the phone
Export a backup first and keep receipt photos separately (backups do not include them). If Android refuses to install over the old app, stop; do not uninstall to force it.
