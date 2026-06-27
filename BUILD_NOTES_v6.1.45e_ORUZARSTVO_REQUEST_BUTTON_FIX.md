# SOV web v6.1.45e — Oružarstvo request button hotfix

## Scope
Hotfix for `oruzarstvo.html` inside `createRequestFromItem`.

## Fixed
- Fixed crash when clicking green `Zatraži` button caused by reading `CURRENT_USER.full_name` from local null variable instead of `window.CURRENT_USER`.
- Fixed silent undefined item values caused by using `item._id` / `item._name` instead of normalized `item.id` / `item.name`.
- Also aligned the request email field to use `window.CURRENT_USER.email` for the same reason.

## Expected result
Clicking green `Zatraži` opens the request/cart drawer and adds the selected equipment item to the request.
