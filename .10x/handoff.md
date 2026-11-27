# Handoff: redesign built, next is device testing

- Branch: `fix/recurring-auto-add` (not merged into master, not pushed by this work).
- Specs: `specs/2026-11-27-more-settings-redesign-design.md`, `specs/2026-11-27-debt-review-design.md`.
- ADRs: `adrs/001-grouped-menus-and-presentation-only-redesign.md`, `adrs/002-debt-review-is-a-reminder-only.md`.
- Plan: `plans/2026-11-27-more-settings-redesign-plan.md` (completed).
- Decisions that changed or extended the redesign spec during implementation:
  - Balance correction rejects commas, minus signs and other symbols (spec 3.3 said "same input as today"). Arabic and Persian digits and the Arabic decimal mark are accepted; extra decimals are rejected.
  - Added: restore rejects non-backup files and reports a settings-only failure; opening balances and the Sadaqah jar follow the backup.
  - Added: the in-app language now applies; app content waits for the lock state.
  - Added: App lock cannot be turned on without device authentication.
  - Added: recurring record and due-date advance share one database transaction; the due date travels with the form.
- Next: test on a device (see `status.md`), then plan the debt review.
- Rules: tests first; commit only when the user asks; ask for a backup before the user installs a new build.
- Per-role decision files were not written: they would only repeat the specs, ADRs and plan.
