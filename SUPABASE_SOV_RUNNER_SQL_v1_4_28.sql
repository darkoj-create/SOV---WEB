-- SOV Admin APK 1.4.28 — Speleo Runner SQL storage
-- Applied on live Supabase project ncomefzkuixyfixisrhi during build work.

create table if not exists public.sov_runner_scores (
  id uuid primary key default gen_random_uuid(),
  player_name text not null,
  score integer not null,
  bats integer not null default 0,
  date_text text,
  source text not null default 'apk',
  client_key text not null unique,
  created_at timestamptz not null default now()
);

alter table public.sov_runner_scores add column if not exists played_at timestamptz not null default now();
alter table public.sov_runner_scores add column if not exists created_by uuid;
alter table public.sov_runner_scores add column if not exists metadata jsonb not null default '{}'::jsonb;

create or replace view public.sov_runner_leaderboard as
select player_name as name, score, bats, date_text, source, created_at
from public.sov_runner_scores
where score > 0 and bats >= 0;

create or replace function public.sov_submit_runner_score(
  p_name text,
  p_score integer,
  p_bats integer default 0,
  p_date_text text default null,
  p_client_key text default null,
  p_source text default 'apk'
)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $$
declare
  v_name text := left(replace(replace(replace(coalesce(p_name, ''), chr(10), ' '), chr(13), ' '), chr(9), ' '), 80);
  v_score integer := coalesce(p_score, 0);
  v_bats integer := greatest(coalesce(p_bats, 0), 0);
  v_date_text text := nullif(left(coalesce(p_date_text, ''), 64), '');
  v_source text := left(coalesce(nullif(p_source, ''), 'apk'), 40);
  v_key text;
  v_id uuid;
begin
  v_name := btrim(v_name);
  if v_name = '' then v_name := 'Anon'; end if;
  if v_score <= 0 then raise exception 'Score must be positive'; end if;

  v_key := nullif(left(coalesce(p_client_key, ''), 220), '');
  if v_key is null then
    v_key := md5(lower(v_source) || '|' || lower(v_name) || '|' || v_score::text || '|' || v_bats::text || '|' || coalesce(v_date_text, ''));
  end if;

  insert into public.sov_runner_scores(player_name, score, bats, date_text, source, client_key, created_by)
  values (v_name, v_score, v_bats, v_date_text, v_source, v_key, auth.uid())
  on conflict (client_key) do update
    set player_name = excluded.player_name,
        score = greatest(public.sov_runner_scores.score, excluded.score),
        bats = greatest(public.sov_runner_scores.bats, excluded.bats),
        date_text = coalesce(public.sov_runner_scores.date_text, excluded.date_text)
  returning id into v_id;

  return jsonb_build_object('ok', true, 'id', v_id, 'client_key', v_key);
end;
$$;

create or replace function public.sov_list_runner_leaderboard(p_limit integer default 100)
returns table(name text, score integer, bats integer, date text)
language sql
stable
security definer
set search_path = pg_catalog, public
as $$
  select s.player_name as name, s.score, s.bats, coalesce(nullif(s.date_text, ''), s.created_at::text) as date
  from public.sov_runner_scores s
  where s.score > 0 and s.bats >= 0
  order by s.score desc, s.bats desc, s.created_at asc
  limit greatest(1, least(coalesce(p_limit, 100), 500));
$$;

alter table public.sov_runner_scores enable row level security;

drop policy if exists sov_runner_scores_public_read on public.sov_runner_scores;
create policy sov_runner_scores_public_read
on public.sov_runner_scores
for select
to anon, authenticated
using (score > 0 and bats >= 0 and length(player_name) between 1 and 80);

drop policy if exists sov_runner_scores_staff_write on public.sov_runner_scores;
create policy sov_runner_scores_staff_write
on public.sov_runner_scores
for all
to authenticated
using (public.sov_rls_can_admin())
with check (public.sov_rls_can_admin());

grant select on public.sov_runner_scores to anon, authenticated;
grant select on public.sov_runner_leaderboard to anon, authenticated;
grant execute on function public.sov_submit_runner_score(text, integer, integer, text, text, text) to anon, authenticated;
grant execute on function public.sov_list_runner_leaderboard(integer) to anon, authenticated;
