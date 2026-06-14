# SOV web v6.1.42a — dashboard hard role hide

## Problem

The first role-aware UI pass still allowed privileged dashboard areas to exist in the initial dashboard markup. Even when JavaScript later filtered them, the dashboard contract was not strict enough: users should not see modules that do not belong to their role at all.

## Changes

- Privileged dashboard entries now start with the native `hidden` attribute.
- Dashboard role filtering now writes both `hidden` and CSS state.
- The entire "Rad po ulozi" section is hidden unless at least one role-specific module is allowed.
- Ordinary `user` cannot see armory, tracking, archive, editor, admin or webmaster dashboard entries.
- Version/cache markers updated to `6.1.42a`.

## SQL

No SQL changes required.
