# SOV web v6.1.42 — role-aware UI

## Problem

Approved users could log in, but the dashboard still showed shortcuts for modules that their role could not open. The clearest mismatch was ordinary `user` seeing armory/tracking entries in "Moje stvari" while the auth layer later blocked access.

## Changes

- `dashboard.html` now filters visible modules by role and permission-like ability.
- Ordinary `user` dashboard shows only member-facing modules: map, cave submission, trips, documents and article submission.
- Armory entries are visible only to `oruzar`, `admin`, and `webmaster`.
- Field tracking is no longer shown to ordinary `user`.
- Internal role modules are hidden unless the role can actually use them.
- Shared shell navigation now hides armory, archive, editor, admin and webmaster links from unauthorized roles.
- Added a no-flicker dashboard load state so restricted modules are not briefly visible before auth finishes.

## SQL

No SQL changes required.
