# Debt review

Date: 2026-11-27
Status: design approved in chat; this written spec is waiting for review. No app code has been written.
Base branch: `fix/recurring-auto-add`.
Build after: `2026-11-27-more-settings-redesign-design.md` (it uses that Settings layout).

## 1. Goal

Make sure the user does not forget open debts. Each new salary or calendar month, the app asks about them. It only reminds: carrying a debt to the next month changes no balance, total or budget. Everything stays inside the app, with no phone notifications. There is no Room database change.

## 2. How it works today

- The carry-forward window opens by itself only when the Debts screen is opened in a new calendar month (`DebtsViewModel.checkMonthChangeRolloverAlert`). It does not follow salary months.
- Confirming it only adds one inbox entry per selected debt, using the original amount even after a part payment (`carryOverDebtsToNewMonth`).
- A part payment also adds an inbox entry for that debt.

## 3. Setting

One setting for all debts, under Settings > Alerts > Debt review, with a shortcut on the Debts screen.

| Value | Automatic reviews |
|---|---|
| Each new month (default) | When a new month starts |
| Also every week | New month, and every 7 days |
| Also every 2 weeks | New month, and every 14 days |
| Also every chosen number of days | New month, and every 1 to 90 days |
| Off | None |

## 4. When a review is due

A review is due when the setting is not Off, at least one debt is open, and one of these is true:

- No review has ever been completed.
- The start of the current month period is later than the one saved at the last completed review. The period comes from `PeriodCalculator.getBounds` with the app's month mode. This includes its existing fallbacks in salary mode.
- An extra period is chosen, and at least that many calendar days have passed since the last completed review.

It is checked when the app is opened, after app lock. It does not interrupt the user while the app is in use. If the app was closed for several months, one review is shown.

## 5. The panel

It lists the same open debts as the Debts screen, in two groups: money the user owes and money owed to the user. Each line shows the person's name, the unpaid amount and the debt's date.

| Control | Result |
|---|---|
| Record payment | Opens the existing payment form for that debt, with today's input rules. |
| Mark as settled | Asks for confirmation, then closes the debt and adds no transaction (as `forgiveDebt` does today). |
| Keep the rest open | Completes the review. Changes no financial data. |
| Later | Closes the panel without completing. It appears again the next time the app is opened. |

- Closing the panel by swiping or Back is the same as Later.
- If no debt stays open after payments or settling, the review completes by itself.
- If recurring items are also due, their panel comes first. Only one panel is shown at a time.
- If the app was opened from a notification that leads to a form, the panel does not cover that form.
- **Review now** on the Debts screen opens the panel at any time, including when the setting is Off. Keep the rest open then counts as a completed review. With no open debts, it shows a short "no open debts" message.

## 6. What it replaces

- The carry-forward window, the button that opens it, its calendar-month check and `carryOverDebtsToNewMonth` are removed.
- The inbox gets one entry when a review becomes due, not one per debt. Completing the review clears it. Tapping it opens the Debts screen, as today.
- A part payment no longer adds its own inbox entry. A full payment or settling still clears old entries for that debt.
- When a review completes, old per-debt reminder entries are cleared too.

## 7. Storage

| Value | In backups |
|---|---|
| Chosen period and number of days | Yes |
| Period start and time of the last completed review | No |

Both are DataStore settings. A missing or out-of-range value in a backup keeps the default. The old `last_debt_rollover_month` key is no longer used; backups that contain it must still restore.

## 8. Code structure

- A plain class decides whether a review is due. It takes the setting, the current period start, the last review, today's date and the open debt count, so it can be tested without Android.
- A ViewModel loads the open debts with unpaid amounts and completes reviews. The Debts screen uses the same loading logic.
- The panel is one composable. The payment form moves out of `DebtsScreen` so both can use it.
- One place decides which opening panel is shown: recurring first, then debt review.

## 9. Text, layout and errors

- All text, including the inbox entry, is in English and Arabic, and the layout mirrors in Arabic.
- The panel scrolls with many debts and works with large text.
- If saving a payment or settling fails, show an error and keep the debt and the review open. Coroutine cancellation is rethrown.

## 10. Testing

Write each test before the code it covers.

- Due check: never reviewed; new period in both month modes; same period; 7, 14 and chosen days, one day before and on the day; Off; no open debts; several missed months.
- Chosen days: 1 and 90 accepted; 0 and 91 rejected.
- Panel: unpaid amount for a part-paid debt; both groups; Later keeps the review due; Keep the rest open changes no transaction or debt; settling adds no transaction.
- Inbox: one entry per due review; cleared on completion; none added by a part payment.
- Backup: period is restored; last review is not; old and invalid values restore safely.
- On a device, in English, Arabic and large text: app lock, both panels in order, opening from a notification, Review now.

## 11. Not included

- Phone notifications for debts.
- A different period for each debt.
- Counting carried debts in monthly totals or budgets.
- Shared-expense split debts, which the Debts screen's loading logic does not use.
