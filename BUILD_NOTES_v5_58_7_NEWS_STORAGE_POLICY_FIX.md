# SOV web v5.58.7 — News CMS Storage policy fix

- Fixes Supabase Storage RLS upload error for `sov-news` bucket.
- Adds `SUPABASE_SOV_NEWS_CMS_v5_58_7_STORAGE_BUCKET_POLICY_FIX.sql`.
- Keeps public read for news assets.
- Allows authenticated upload/update/delete in `sov-news`; editor UI remains role-gated.
- Updates `sync-status.html` to v5.58.7 and adds `sov_news_storage_status()` check.
- No APK change required.
