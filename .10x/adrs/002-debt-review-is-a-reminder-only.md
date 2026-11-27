# ADR-002: The debt review is a reminder and stores no per-debt data

**Status:** Accepted
**Date:** 2026-11-27
**Feature:** debt-review
**Author:** 10x-Team (Architect + Staff Engineer)

## Context
The user wants to be asked about open debts each salary or calendar month, with a chosen period and no phone notification. Spec: `.10x/specs/2026-11-27-debt-review-design.md`.

## Decision
Carrying a debt over changes no money, total or budget. One period applies to all debts. The period and last completed review are DataStore values. A plain class decides when a review is due.

## Alternatives Considered
| Alternative | Pros | Cons | Why Not |
|-------------|------|------|---------|
| Count carried debts in the new month | Totals show debts | Changes calculations, may need a migration | User chose reminder only |
| A period per debt | Finer control | Room migration, more setup | User chose one period |
| Phone notifications | Reaches a closed app | Not wanted | User asked for in-app only |

## Consequences
### Positive
- No migration and no change to financial data.
### Negative
- No reminder while the app stays closed.
### Risks
- Several pop-ups on opening. Mitigation: one coordinator shows one at a time.

## Dependencies
- Depends on ADR-001 and the opening-panel order built for recurring items.
