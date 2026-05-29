# SOV Admin APK 1.3.6 — Oružarstvo Supabase Sync

## Scope
This build connects the mobile Oružarstvo module to the web/Supabase equipment backend.

## Added
- Reads equipment catalog from `public.equipment_items`
- Reads user's equipment requests from `public.equipment_requests` + `public.equipment_request_items`
- Armorer/Admin queue reads all visible requests allowed by RLS
- User can create an equipment request from Android
- Armorer/Admin can update request status: approved / issued / returned
- Local equipment cache fallback when offline
- Visible sync status card and manual refresh

## Not added intentionally
- No inventory editing from APK
- No delete from APK
- No mass import from APK
- Web remains source of truth for catalog/inventory management

## Required SQL
Requires the already applied web SQL:
`SUPABASE_ORUZARSTVO_v5_36_3_SCHEMA_TOLERANT_FIX.sql`

## Build metadata
- versionCode: 900017
- versionName: 1.3.6-oruzarstvo-supabase-sync
