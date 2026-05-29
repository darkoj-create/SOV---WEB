USE THIS SQL FOR ARHIVAR SUBMISSIONS:
SUPABASE_SOV_ARHIVAR_SUBMISSIONS_v5_58_1_SAFE_PROFILE_FIX.sql

v5.58.1 fixes the v5.58.0 SQL error:
ERROR: relation "public.sov_user_profiles" does not exist

It uses safe profile helpers and works with public.profiles / public.sov_profiles / public.sov_user_profiles when present.
