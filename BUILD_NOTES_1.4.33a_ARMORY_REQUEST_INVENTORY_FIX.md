# SOV Admin v1.4.33a — Oružarstvo request/inventory fix

Base: v1.4.32b-lake-buildfix
Date: 2026-07-11

## Scope
UI-only patch in `HomeAndToolsScreens.kt` plus version bump in `app/build.gradle.kts`.
No changes to `EquipmentSupabaseRepository.kt`, Supabase REST calls, request status model, inventory offline queue, or sync logic.

## Changes

### 1) Member requester visible to armorer
- Updated `EquipmentRequestCard(...)`.
- When `canManage == true`, the card now shows a prominent requester chip directly under the item title.
- Uses existing fields already present on `EquipmentMobileRequest` / `EquipmentCloudRequest`:
  - `requesterName`
  - `requesterEmail`
- Fallback display: requester name → requester email → localized Unknown/Nepoznato.
- My Requests (`canManage = false`) remain unchanged and do not show the extra requester chip.

### 2) Inventory bulk-confirm safety
- Added confirmation dialog before the bulk action:
  - freezes the exact currently visible inventory item snapshot
  - shows item count
  - shows active search/filter description
  - warns user to cancel if filter is wrong
- Added simple one-step undo:
  - stores previous `inventoryCounts` and `inventoryDone`
  - shows `Poništi zadnju bulk potvrdu` / `Undo last bulk confirm`
  - reset clears pending confirm and undo state

### 3) Dead Oružarstvo UI cleanup
- Removed unused composables after project-wide grep confirmed no call sites:
  - `EquipmentPremiumHero`
  - `EquipmentSyncStatusCard`
  - `EquipmentRoleStrip`
- Active header remains `EquipmentArmoryTopBar`.

## Version
- `versionCode = 900134`
- `versionName = "1.4.33a-armory-request-inventory-fix"`

## Build status
The sandbox cannot download Gradle wrapper (`services.gradle.org` DNS/network unavailable), so local compile could not be completed here. Static checks performed:
- dead component grep returns no references
- brace/parenthesis balance OK
- changed file patch generated successfully

## Manual test checklist
1. Open Oružarstvo as Oružar/Admin.
2. Go to `Zahtjevi članova`.
3. Verify each managed request card shows requester name/email near the top.
4. Go to `Inventura`.
5. Tap bulk confirm; verify dialog shows count and active filter/search.
6. Confirm; verify items are marked OK.
7. Tap undo; verify previous `inventoryCounts` and `inventoryDone` are restored.
8. Reset inventory; verify undo state disappears.
9. Confirm request status buttons still work: `Zatraženo → Izdano → Vraćeno/Djelomično vraćeno`.
