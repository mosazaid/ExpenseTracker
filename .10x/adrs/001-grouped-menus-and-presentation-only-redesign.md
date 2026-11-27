# ADR-001: Grouped menus, with no navigation or Room change

**Status:** Accepted
**Date:** 2026-11-27
**Feature:** more-settings-redesign
**Author:** 10x-Team (Architect + Staff Engineer)

## Context
More is one long column of 18 tappable entries and Settings is one 1095-line screen. The user's real data is on their phone. Spec: `.10x/specs/2026-11-27-more-settings-redesign-design.md`.

## Decision
More becomes grouped tool tiles plus two app rows, drawn from one menu list. Settings becomes an index with one page and one type-safe route per topic. Bottom tabs stay. New state (last backup time, database viewer unlock) goes in DataStore; Room stays at version 23. Rules that were inside composables or workers move into plain classes with unit tests.

## Alternatives Considered
| Alternative | Pros | Cons | Why Not |
|-------------|------|------|---------|
| Tidy the two screens in place | Smallest change | Settings stays long | Does not fix the main complaint |
| New navigation: Tools tab, gear, favorites, search | Most flexible | Largest change | More than the app needs now |

## Consequences
### Positive
- Each Settings topic can be changed and tested alone.
### Negative
- Some settings take one more tap. More files and routes.
### Risks
- A setting could be lost in the split. Mitigation: the plan lists every current control against its new page.
- Installing a differently signed build needs an uninstall, which deletes data. Mitigation: back up first; never uninstall to force an install.

## Dependencies
- Depends on branch `fix/recurring-auto-add`.
- ADR-002 uses this Settings layout.
