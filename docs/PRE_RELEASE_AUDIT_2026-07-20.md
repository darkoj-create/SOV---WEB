# SOV web pre-release audit — 2026-07-20

## Release status

**READY FOR PRODUCTION MERGE.**

Audit branch: `audit/pre-release-2026-07-20`  
Pull request: `#1`  
Release: `v6.1.45av-pre-release-audit`

## Final release gate

The complete automated gate passed on the exact normalized source:

- static audit of **279 HTML pages**;
- **0 static errors**;
- Playwright browser smoke over all pages;
- **0 browser errors and 0 browser warnings**;
- JavaScript syntax audit passed;
- external script-tag integrity passed;
- Trips refresh button regression test passed;
- mobile pull-to-refresh regression test passed;
- live Supabase is isolated from browser smoke tests.

## Fixed defects

### Trips

- force refresh no longer reuses the normal in-flight request;
- the page has exactly one functional **Osvježi** button;
- mobile pull-to-refresh works only at the top of the page and outside forms/modals;
- stale request/cache paths are bypassed;
- RPC-first loading keeps the existing direct-table fallback.

### Public pages and legacy content

- repaired nested article asset paths and root `update.json` loading;
- removed missing Speleoškola local media references;
- repaired missing TopoDroid Supabase loader;
- repaired the malformed dashboard external script tag;
- normalized legacy login, status, Trips, Map and minutes aliases;
- removed conflicting Vercel redirect/rewrite mechanisms;
- added compatibility shims for legacy shared assets.

### Primary domain

The selected authoritative hostname is:

`https://www.so-velebit.hr`

- apex `https://so-velebit.hr` redirects to `www`;
- canonical, Open Graph, Twitter, JSON-LD, sitemap and other absolute public URLs were normalized to `www`;
- DNS/HTTPS remain managed by Vercel.

## Production Supabase migration

Applied as migration:

`sov_release_v6145av_pre_release_audit`

Source file:

`sql/sov_release_v6145av.sql`

The migration:

- creates the private `sov_map_objects_light` feed;
- replaces the expensive Map RPC path;
- adds the missing Arhivar submission-review RPC;
- aligns reviewer roles to `webmaster`, `admin`, `arhivar`;
- converts the Spelo Runner leaderboard to `security_invoker`;
- removes anonymous score write grants while preserving public leaderboard reads.

### Post-migration verification

- map view and both Map RPCs exist;
- Arhivar review RPC exists;
- authenticated approved webmaster receives Map rows;
- approved webmaster is recognized as a reviewer;
- `anon` cannot execute Map RPCs or read the private Map view;
- public leaderboard remains readable;
- anonymous insert/update/delete on runner scores is denied;
- leaderboard still contains **139** valid rows, high score **1043**;
- measured 1000-row Map query: approximately **2.1 s** and **2995 shared buffer hits**, down from the old timeout-prone worklist path at approximately **6.1 s / 916k hits**.

## Historical links

Old external `http://` links inside archived articles were intentionally not rewritten blindly. They remain content-maintenance warnings rather than runtime or release blockers.

## Android/APK item outside this web release

The Android cleanup RPC still attempts a direct `storage.objects` deletion and receives the expected Supabase 403. Do not solve this by opening Storage table permissions. The correct fix requires the actual Android source or a narrowly scoped Storage API Edge Function.

This does not block the audited web release because the web source, public pages, member modules and database migration are independent of that APK cleanup path.

## Production sequence

1. Run the final CI on this audit record commit.
2. Merge PR #1 to `main`.
3. Wait for Vercel production deployment to become READY.
4. Verify `www`, apex redirect, release manifest, public pages and critical member routes.
5. Keep the automated release gate as the minimum standard for future deployments.
