# SOV web pre-release audit — 2026-07-20

## Release status

**NOT READY FOR PRODUCTION MERGE.**

This audit runs on branch `audit/pre-release-2026-07-20` and draft PR #7. Production code and the live Supabase schema have not been changed by these fixes. SQL changes were installed only inside transactions that ended with `ROLLBACK`.

## Release gate

A release may move to `main` only when all items below pass:

1. Static repository audit: local links/assets, HTML structure, auth registry, routes, versions and secret scan.
2. JavaScript syntax audit for every `.js` and `.mjs` file.
3. External script tag integrity audit.
4. Browser smoke test for every HTML page, isolated from live Supabase.
5. Functional Trips hard-refresh test for button and mobile pull-to-refresh.
6. Supabase schema/RPC migrations reviewed and applied in one controlled release step.
7. Production smoke test for public pages and authenticated critical flows.
8. Domain canonical and redirect direction aligned.
9. Version contract synchronized across `VERSION.txt`, `update.json`, `assets/sov-version.js` and README.

## Confirmed active defects

### 1. Trips refresh did not guarantee a fresh database read

**Cause:** `izleti-cloud.html` called `loadTrips({force:true})`, but the data layer reused its normal in-flight request. A user could tap Refresh or pull down and still receive the old request/cache.

**Branch fix:**
- true hard-refresh path that bypasses the normal in-flight request;
- RPC first, direct table fallback;
- button busy/error state;
- mobile pull-to-refresh only at the top of the page and outside forms/modals;
- dedicated Playwright regression test for both button and pull gesture.

**Production:** not yet changed.

### 2. Map feed times out

**Evidence:** production client/Postgres logs show HTTP 500 and statement timeouts for `sov_map_objects` and `sov_map_objects_page`.

**Measured current path:**
- detailed Arhivar worklist query: approximately **6.1 seconds**;
- approximately **916,000 shared buffer hits** for a 1000-row request.

**Branch fix:** `sql/sov_map_fast_feed_v6145av.sql`
- separates the map feed from the detailed Arhivar worklist;
- creates a compact private `sov_map_objects_light` view;
- keeps authorization at SECURITY DEFINER RPC boundaries;
- direct view access revoked from `anon` and `authenticated`.

**Rollback validation:**
- exact migration installed in a transaction;
- called as an authenticated approved webmaster;
- returned 1000 rows with expected map fields in approximately **1.7 seconds**;
- transaction rolled back;
- verified live RPC still uses the old path.

**Production:** migration not applied.

### 3. Arhivar submission review uses a missing RPC and excludes webmaster

**Cause:**
- current frontend calls `sov_update_speleo_submission_review`, which does not exist in the live database, then falls back to a direct table update;
- central helper `sov_submissions_is_reviewer()` allows only `admin` and `arhivar`, while web permissions allow `webmaster`, `admin` and `arhivar`.

**Branch fix:** `sql/sov_submission_review_rpc_v6145av.sql`
- aligns reviewer roles to `webmaster/admin/arhivar`;
- adds one reviewer-only RPC for submitted/needs_changes/approved/rejected updates;
- revokes public/anon execution.

**Rollback validation:**
- inserted a temporary submission inside a transaction;
- called RPC as authenticated webmaster;
- verified status, missing categories, note and reviewer ID;
- rolled back and verified temporary row and RPC are absent from live schema.

**Production:** migration not applied.

### 4. Dashboard inline repair code was swallowed by an external script tag

**Cause:** `assets/sov-client-logger.js` script tag was not closed before the inline topbar repair. Browsers ignore inline code in a script tag that also has `src`.

**Branch fix:** external script is closed before the inline block.

**Permanent prevention:** `tools/script_tag_audit.py` now blocks any external `<script src>` tag that contains inline code.

### 5. Routing contained duplicate mechanisms

**Cause:** status routes existed in both Vercel redirects and rewrites. Trips and the old minutes archive also used HTML meta refreshes in addition to JavaScript/client routing.

**Branch fix:**
- one Vercel redirect per alias;
- conflicting rewrites removed;
- Trips aliases point to `izleti-cloud.html`;
- legacy minutes archive points to `zapisnici-native.html`;
- meta refresh removed from the audited alias pages.

## Historical errors verified as already resolved

### Minutes announcement approval

Older telemetry showed 400/404 errors. The current frontend RPC signatures match the live database. A rollback test of `sov_approve_trip_announcement(uuid)` succeeded as the authenticated webmaster. The trigger normalizes legacy visibility `members` to valid value `club`.

### Member armory request UI

Older telemetry referenced `profile.full_name` and removed variables. The current page safely falls back from full name to email to `Član`; the old variables/functions are no longer present.

These historical events remain useful regression evidence but are not current release blockers.

## Android/APK blocker outside this web repository

Android repeatedly calls `sov_trip_assets_cleanup_expired`, whose implementation tries to delete directly from `storage.objects`. Supabase correctly rejects this with HTTP 403 and instructs use of the Storage API.

**Do not solve this by granting direct delete on Storage tables.** Correct solutions are:
- change the Android source to use Storage API deletion; or
- move authenticated cleanup to a narrowly scoped Edge Function using the Storage API.

The connected `SOV-APP` repository is only a release channel and does not contain the actual Android source, so this blocker cannot be safely fixed from the current repository access.

## Supabase security and performance review

Advisors report items that require triage rather than blind automatic changes:

- security-definer leaderboard views;
- mutable function `search_path` warnings;
- leaked-password protection disabled;
- anonymous sign-ins enabled;
- available database patch update;
- unindexed foreign keys;
- repeated RLS init-plan and multiple-policy warnings;
- duplicate/unused indexes.

Important nuance: broad-looking RLS policies are not automatically proof of public exposure. Direct `anon` reads of tested armory tables were denied by grants. Each advisor item must be evaluated against grants, RLS and intended product behavior before change.

## Domain and SEO launch item

At audit time:

- `https://www.so-velebit.hr` returns 200 through Vercel with valid HTTPS;
- `https://so-velebit.hr` redirects to `www`;
- most page canonical metadata points to the apex domain `so-velebit.hr`.

Before launch, choose one authoritative hostname and align both Vercel redirect and canonical metadata. Preferred current code direction is apex `so-velebit.hr`, but changing the Vercel domain redirect requires a dashboard action not exposed by the current connector.

## Audit isolation

The first browser smoke prototype touched live Supabase and produced expected local 401 telemetry. This was corrected. Current browser smoke tests intercept every Supabase project request and never read from or write to the live database.

## Final release sequence

1. Finish green CI and inspect generated audit artifacts.
2. Resolve every static/browser blocker.
3. Review and apply the two SQL migrations in one controlled database step.
4. Run Supabase advisors again.
5. Run production authenticated smoke tests for Trips, Map, Arhivar, Armory, Documents, Users and Tracking.
6. Align hostname/canonical direction.
7. Bump all version files consistently to the final release version.
8. Merge draft PR only after the release gate is green.
