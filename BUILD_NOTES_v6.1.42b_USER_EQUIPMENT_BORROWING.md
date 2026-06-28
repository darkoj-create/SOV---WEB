# SOV web v6.1.42b — user equipment borrowing

## Problem

The hard role-hide build correctly removed internal tools from ordinary users, but it also hid the member equipment borrowing entry. Ordinary members should be able to open the equipment catalog and request loans.

## Changes

- `dashboard.html` shows `Oprema` to ordinary `user` again.
- Shared shell navigation shows `Oprema` under the normal user group.
- `auth.js` now protects `oruzarstvo.html` with approved-user access, not armory-only access.
- `oruzarstvo-import.html` and all `oruzar-master*` pages remain armory/admin/webmaster only.
- Archive, editor, admin, tracking and webmaster dashboard areas remain hidden from ordinary users.

## SQL

No SQL changes required.
