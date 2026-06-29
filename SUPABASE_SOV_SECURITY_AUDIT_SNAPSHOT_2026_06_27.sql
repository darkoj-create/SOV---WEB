-- SOV Security Audit Snapshot - READ ONLY
-- Run this before any hardening. It does not change schema or data.

-- RLS policies
select
  schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
from pg_policies
where schemaname in ('public','storage')
order by schemaname, tablename, policyname;

-- Public exposed functions and security definer status
select
  n.nspname as schema,
  p.proname as function_name,
  pg_get_function_identity_arguments(p.oid) as arguments,
  p.prosecdef as security_definer,
  p.provolatile as volatility,
  array_to_string(p.proacl, ',') as acl
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
order by p.prosecdef desc, p.proname;

-- Views that may bypass expected RLS behaviour or expose auth data
select
  schemaname,
  viewname,
  definition
from pg_views
where schemaname = 'public'
  and (
    definition ilike '%auth.users%'
    or definition ilike '%security definer%'
    or viewname ilike 'sov_%'
    or viewname ilike 'speleo_%'
  )
order by viewname;

-- Public tables without RLS
select
  n.nspname as schema,
  c.relname as table_name,
  c.relrowsecurity as rls_enabled
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relkind = 'r'
  and c.relrowsecurity = false
order by c.relname;
