-- SOV web v6.1.0 DATABASE CLEANUP PHASE 1
-- Safe/idempotent cleanup foundation.
-- IMPORTANT: This patch does NOT drop, rename, or destructively alter existing application tables.

create extension if not exists pgcrypto;

create table if not exists public.sov_release_registry (
  build_version text primary key,
  build_name text not null,
  released_at timestamptz not null default now(),
  base_build text,
  sql_files text[] not null default '{}',
  notes text,
  created_at timestamptz not null default now()
);

create table if not exists public.sov_schema_registry (
  object_name text primary key,
  object_type text not null default 'table',
  module text not null default 'core',
  lifecycle_status text not null default 'active' check (lifecycle_status in ('active','compat','legacy','deprecated','unknown')),
  source_build text,
  notes text,
  reviewed_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.sov_audit_log (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  actor_id uuid,
  actor_email text,
  module text not null default 'system',
  action text not null,
  entity_table text,
  entity_id text,
  details jsonb not null default '{}'::jsonb
);

create index if not exists sov_audit_log_created_at_idx on public.sov_audit_log(created_at desc);
create index if not exists sov_audit_log_module_idx on public.sov_audit_log(module);
create index if not exists sov_audit_log_entity_idx on public.sov_audit_log(entity_table, entity_id);

create table if not exists public.sov_armory_taxonomy (
  id uuid primary key default gen_random_uuid(),
  main_category text not null,
  subcategory text not null default 'Općenito',
  sort_order integer not null default 1000,
  aliases text[] not null default '{}',
  is_active boolean not null default true,
  source_build text not null default 'v6.1.0',
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(main_category, subcategory)
);

insert into public.sov_release_registry(build_version, build_name, base_build, sql_files, notes)
values (
  '6.1.0',
  'sov-web-build-v6.1.0-database-cleanup-phase-1',
  'sov-web-build-v6.0.9-predane-jame-workflow-hardening.zip',
  array['SUPABASE_SOV_DATABASE_CLEANUP_PHASE_1_v6_1_0.sql'],
  'Database cleanup phase 1: registry, taxonomy, audit log and health RPC. Safe additive patch only.'
)
on conflict (build_version) do update set
  build_name=excluded.build_name,
  base_build=excluded.base_build,
  sql_files=excluded.sql_files,
  notes=excluded.notes,
  released_at=now();

insert into public.sov_schema_registry(object_name, object_type, module, lifecycle_status, source_build, notes) values
('profiles','table','identity','active','v5.59.8','User profiles / approval / role source for web UI.'),
('sov_user_permissions','view_or_table','identity','active','v5.59.x','Role/permissions compatibility object used by role manager and dashboards.'),
('sov_system_health','rpc','health','active','v6.0.5','Central health RPC for sync-status.'),
('sov_database_cleanup_health','rpc','health','active','v6.1.0','Database cleanup phase 1 health RPC.'),
('sov_schema_registry','table','health','active','v6.1.0','Human/system readable registry of SOV database objects.'),
('sov_release_registry','table','health','active','v6.1.0','Release/build registry.'),
('sov_audit_log','table','audit','active','v6.1.0','Generic audit/event log for future module actions.'),
('sov_armory_taxonomy','table','oruzarstvo','active','v6.1.0','Canonical Oružarstvo category/subcategory dictionary.'),
('sov_oruzarstvo_grouped_catalog','view_or_table','oruzarstvo','active','v5.59.x','Preferred Oružarstvo catalog source for web display.'),
('sov_oruzarstvo_health','rpc','oruzarstvo','active','v5.59.11','Module health check for Oružarstvo.'),
('speleo_object_submissions','table','arhivar','active','v6.0.3','Predane jame submissions inbox source.'),
('speleo_object_submission_files','table','arhivar','active','v6.0.3','Predane jame attachments metadata.'),
('sov_speleo_submissions_health','rpc','arhivar','active','v6.0.9','Predane jame workflow health.'),
('sov_update_speleo_submission_review','rpc','arhivar','active','v6.0.9','Safe review/status update RPC.'),
('speleo_objects_staging','table','baza','active','v5.x','Primary fallback table for speleo objects/map/archive.'),
('speleo_objects_live_sql','view_or_table','baza','compat','v5.x','Live SQL/view object source when available.'),
('sov_map_objects_page','rpc','baza','active','v5.58.x','Paged map object source used to avoid statement timeouts.'),
('sov_news','table','news','active','v5.58.3','News CMS table.'),
('sov_news_storage_status','rpc','news','active','v5.58.x','News storage bucket policy check.'),
('sov_tracking_sync_status','view_or_table','tracking','active','v5.59.x','Tracking status view for sync-status.'),
('sov_trip_assets_active','view_or_table','izleti','active','v5.59.6','Trip asset/package manifest for field use.'),
('sov_trip_assets','table','izleti','active','v5.59.6','Trip asset/package metadata.'),
('sov_trip_files','storage_bucket','izleti','active','v5.59.6','Trip files storage bucket.'),
('speleo-submissions','storage_bucket','arhivar','active','v6.0.3','Submission attachments storage bucket.'),
('sov-news','storage_bucket','news','active','v5.58.x','News images storage bucket.')
on conflict (object_name) do update set
  object_type=excluded.object_type,
  module=excluded.module,
  lifecycle_status=excluded.lifecycle_status,
  source_build=excluded.source_build,
  notes=excluded.notes,
  reviewed_at=now(),
  updated_at=now();

insert into public.sov_armory_taxonomy(main_category, subcategory, sort_order, aliases, notes) values
('Užad i užetna oprema','Užad',10,array['uzad','uze','rope'],'Canonical rope grouping.'),
('Užad i užetna oprema','Karabineri i spojnice',20,array['karabiner','spojnica','mailon'],'Connectors.'),
('Užad i užetna oprema','Pojasevi i oprema za kretanje',30,array['pojas','croll','stop','shunt','basic'],'Vertical gear.'),
('Bušilice i svrdla','Bušilice',100,array['busilica','bosch','hilti','makita'],'Drills only.'),
('Bušilice i svrdla','Baterije za bušilice',110,array['baterija bosch','baterija hilti','baterija makita','aku bosch'],'Power-tool batteries only.'),
('Bušilice i svrdla','Svrdla i pribor',120,array['svrdlo','sds','borer'],'Drill bits and drill accessories.'),
('Elektro, rasvjeta i foto','Baterije, punjači i powerbankovi',200,array['aa','aaa','9v','usb','punjac','powerbank','solarni punjac'],'General batteries and chargers; not drill batteries.'),
('Elektro, rasvjeta i foto','Lampe i rasvjeta',210,array['lampa','naglavna','rasvjeta'],'Lighting.'),
('Elektro, rasvjeta i foto','Foto i video',220,array['foto','kamera','gopro'],'Photo/video equipment.'),
('Oprema za crtanje','Busole / kompasi / Suunto',300,array['busola','kompas','suunto'],'Survey compasses.'),
('Oprema za crtanje','DistoX / TopoDroid',310,array['distox','topodroid','disto'],'Survey electronics.'),
('Dronovi','Dronovi i pribor',400,array['dron','drone','dji'],'Drone equipment.'),
('Alat i radionica','Ručni alat',500,array['alat','klijesta','cekic','odvijac'],'General tools.'),
('Medicinska oprema','Prva pomoć',600,array['medicinska','prva pomoc','first aid'],'Medical equipment.'),
('Oprema za logor','Logor i bivak',700,array['logor','sator','bivak'],'Camp equipment.'),
('Razno','Nerazvrstano',999,array['razno','ostalo'],'Temporary fallback only; should shrink over time.')
on conflict (main_category, subcategory) do update set
  sort_order=excluded.sort_order,
  aliases=excluded.aliases,
  is_active=true,
  notes=excluded.notes,
  updated_at=now();

create or replace function public.sov_log_event(
  p_module text,
  p_action text,
  p_entity_table text default null,
  p_entity_id text default null,
  p_details jsonb default '{}'::jsonb
)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_id uuid;
  v_actor uuid;
  v_email text;
begin
  begin
    v_actor := auth.uid();
  exception when others then
    v_actor := null;
  end;
  begin
    v_email := coalesce(auth.jwt()->>'email', null);
  exception when others then
    v_email := null;
  end;
  insert into public.sov_audit_log(actor_id, actor_email, module, action, entity_table, entity_id, details)
  values (v_actor, v_email, coalesce(p_module,'system'), coalesce(p_action,'unknown'), p_entity_table, p_entity_id, coalesce(p_details,'{}'::jsonb))
  returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.sov_table_count_safe(p_table text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_exists boolean;
  v_count bigint;
begin
  select exists(
    select 1 from information_schema.tables
    where table_schema='public' and table_name=p_table
  ) into v_exists;
  if not v_exists then
    return jsonb_build_object('exists', false, 'count', null);
  end if;
  execute format('select count(*) from public.%I', p_table) into v_count;
  return jsonb_build_object('exists', true, 'count', v_count);
exception when others then
  return jsonb_build_object('exists', null, 'count', null, 'error', sqlerrm);
end;
$$;

create or replace function public.sov_routine_exists_safe(p_name text)
returns boolean
language sql
security definer
set search_path = public
as $$
  select exists(select 1 from information_schema.routines where routine_schema='public' and routine_name=p_name);
$$;

create or replace function public.sov_database_cleanup_health()
returns jsonb
language plpgsql
security definer
set search_path = public, storage
as $$
declare
  v_registry_count bigint;
  v_release_count bigint;
  v_taxonomy_count bigint;
  v_audit_count bigint;
  v_missing text[] := '{}';
  v_required text[] := array['profiles','sov_schema_registry','sov_release_registry','sov_audit_log','sov_armory_taxonomy'];
  v_name text;
  v_exists boolean;
  v_bucket_count bigint := 0;
begin
  foreach v_name in array v_required loop
    select exists(select 1 from information_schema.tables where table_schema='public' and table_name=v_name) into v_exists;
    if not v_exists then v_missing := array_append(v_missing, v_name); end if;
  end loop;

  select count(*) into v_registry_count from public.sov_schema_registry;
  select count(*) into v_release_count from public.sov_release_registry;
  select count(*) into v_taxonomy_count from public.sov_armory_taxonomy where is_active;
  select count(*) into v_audit_count from public.sov_audit_log;

  begin
    select count(*) into v_bucket_count from storage.buckets where id in ('sov-news','speleo-submissions','sov-trip-files','sov-trip-assets');
  exception when others then
    v_bucket_count := -1;
  end;

  return jsonb_build_object(
    'ok', array_length(v_missing,1) is null,
    'checked_at', now(),
    'build', '6.1.0',
    'missing_required', coalesce(to_jsonb(v_missing),'[]'::jsonb),
    'registry_objects', v_registry_count,
    'registered_releases', v_release_count,
    'armory_taxonomy_rows', v_taxonomy_count,
    'audit_rows', v_audit_count,
    'known_storage_buckets', v_bucket_count,
    'rpcs', jsonb_build_object(
      'sov_system_health', public.sov_routine_exists_safe('sov_system_health'),
      'sov_database_cleanup_health', public.sov_routine_exists_safe('sov_database_cleanup_health'),
      'sov_log_event', public.sov_routine_exists_safe('sov_log_event'),
      'sov_oruzarstvo_health', public.sov_routine_exists_safe('sov_oruzarstvo_health'),
      'sov_speleo_submissions_health', public.sov_routine_exists_safe('sov_speleo_submissions_health')
    ),
    'core_tables', jsonb_build_object(
      'profiles', public.sov_table_count_safe('profiles'),
      'speleo_object_submissions', public.sov_table_count_safe('speleo_object_submissions'),
      'speleo_object_submission_files', public.sov_table_count_safe('speleo_object_submission_files'),
      'sov_news', public.sov_table_count_safe('sov_news'),
      'speleo_objects_staging', public.sov_table_count_safe('speleo_objects_staging')
    ),
    'policy', 'safe additive cleanup: no drops, no renames, no destructive type changes'
  );
exception when others then
  return jsonb_build_object('ok', false, 'checked_at', now(), 'error', sqlerrm, 'build', '6.1.0');
end;
$$;

grant execute on function public.sov_database_cleanup_health() to authenticated;
grant execute on function public.sov_log_event(text,text,text,text,jsonb) to authenticated;
grant execute on function public.sov_table_count_safe(text) to authenticated;
grant execute on function public.sov_routine_exists_safe(text) to authenticated;

-- Keep registry visible for authenticated Webmaster/admin tools. RLS can be tightened later after role policy consolidation.
alter table public.sov_schema_registry enable row level security;
alter table public.sov_release_registry enable row level security;
alter table public.sov_armory_taxonomy enable row level security;
alter table public.sov_audit_log enable row level security;

do $$ begin
  create policy "sov_schema_registry_read_auth" on public.sov_schema_registry for select to authenticated using (true);
exception when duplicate_object then null; end $$;
do $$ begin
  create policy "sov_release_registry_read_auth" on public.sov_release_registry for select to authenticated using (true);
exception when duplicate_object then null; end $$;
do $$ begin
  create policy "sov_armory_taxonomy_read_auth" on public.sov_armory_taxonomy for select to authenticated using (true);
exception when duplicate_object then null; end $$;
do $$ begin
  create policy "sov_audit_log_insert_auth" on public.sov_audit_log for insert to authenticated with check (true);
exception when duplicate_object then null; end $$;
do $$ begin
  create policy "sov_audit_log_read_auth" on public.sov_audit_log for select to authenticated using (true);
exception when duplicate_object then null; end $$;

select public.sov_log_event('system','database_cleanup_phase_1','sov_release_registry','6.1.0', jsonb_build_object('build','sov-web-build-v6.1.0-database-cleanup-phase-1'));
