-- SOV Speleo Runner SQL scores v1.4.29c
-- Sheet is no longer source of truth. Existing CSV results are imported once with ON CONFLICT DO NOTHING.

create table if not exists public.sov_runner_scores (
  id uuid primary key default gen_random_uuid(),
  player_name text not null,
  score integer not null check (score > 0),
  bats integer not null default 0 check (bats >= 0),
  date_text text,
  source text not null default 'apk',
  client_key text not null unique,
  created_at timestamptz not null default now(),
  played_at timestamptz not null default now(),
  created_by uuid,
  metadata jsonb not null default '{}'::jsonb
);

alter table public.sov_runner_scores enable row level security;

do $$
begin
  if not exists (select 1 from pg_policies where schemaname='public' and tablename='sov_runner_scores' and policyname='sov_runner_scores_public_read') then
    create policy sov_runner_scores_public_read on public.sov_runner_scores
      for select to anon, authenticated
      using (score > 0 and bats >= 0 and length(player_name) between 1 and 80);
  end if;

  if not exists (select 1 from pg_policies where schemaname='public' and tablename='sov_runner_scores' and policyname='sov_runner_scores_staff_write') then
    create policy sov_runner_scores_staff_write on public.sov_runner_scores
      for all to authenticated
      using (public.sov_rls_can_admin())
      with check (public.sov_rls_can_admin());
  end if;
end $$;

create or replace function public.sov_submit_runner_score(
  p_name text,
  p_score integer,
  p_bats integer default 0,
  p_date_text text default null,
  p_client_key text default null,
  p_source text default 'apk'
) returns jsonb
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

  insert into public.sov_runner_scores(player_name, score, bats, date_text, source, client_key, created_by, played_at, metadata)
  values (
    v_name,
    v_score,
    v_bats,
    v_date_text,
    v_source,
    v_key,
    auth.uid(),
    coalesce((nullif(v_date_text, '')::timestamp at time zone 'Europe/Zagreb'), now()),
    jsonb_build_object('storage', 'supabase', 'client', v_source)
  )
  on conflict (client_key) do update
    set player_name = excluded.player_name,
        score = greatest(public.sov_runner_scores.score, excluded.score),
        bats = greatest(public.sov_runner_scores.bats, excluded.bats),
        date_text = coalesce(public.sov_runner_scores.date_text, excluded.date_text),
        metadata = public.sov_runner_scores.metadata || excluded.metadata
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
  select s.player_name as name, s.score, s.bats, coalesce(nullif(s.date_text, ''), s.played_at::text, s.created_at::text) as date
  from public.sov_runner_scores s
  where s.score > 0 and s.bats >= 0
  order by s.score desc, s.bats desc, s.played_at asc, s.created_at asc
  limit greatest(1, least(coalesce(p_limit, 100), 500));
$$;

grant execute on function public.sov_submit_runner_score(text, integer, integer, text, text, text) to anon, authenticated;
grant execute on function public.sov_list_runner_leaderboard(integer) to anon, authenticated;

with seed(player_name, score, bats, date_text, client_key) as (
values
  ('Darko', 22, 3, '2026-05-13 22:20:07', 'legacy_sheet|darko|22|3|2026-05-13 22:20:07'),
  ('Darko', 21, 2, '2026-05-13 22:20:18', 'legacy_sheet|darko|21|2|2026-05-13 22:20:18'),
  ('Darko', 34, 7, '2026-05-13 22:50:56', 'legacy_sheet|darko|34|7|2026-05-13 22:50:56'),
  ('Darko', 47, 11, '2026-05-13 22:51:18', 'legacy_sheet|darko|47|11|2026-05-13 22:51:18'),
  ('Darko', 17, 0, '2026-05-13 22:51:33', 'legacy_sheet|darko|17|0|2026-05-13 22:51:33'),
  ('Darko', 17, 0, '2026-05-13 22:51:40', 'legacy_sheet|darko|17|0|2026-05-13 22:51:40'),
  ('Darko', 63, 15, '2026-05-13 22:52:10', 'legacy_sheet|darko|63|15|2026-05-13 22:52:10'),
  ('Darko', 61, 13, '2026-05-13 22:52:47', 'legacy_sheet|darko|61|13|2026-05-13 22:52:47'),
  ('Darko', 33, 7, '2026-05-13 22:53:04', 'legacy_sheet|darko|33|7|2026-05-13 22:53:04'),
  ('Darko', 161, 19, '2026-05-13 22:54:37', 'legacy_sheet|darko|161|19|2026-05-13 22:54:37'),
  ('Darko', 247, 53, '2026-05-20 19:54:16', 'legacy_sheet|darko|247|53|2026-05-20 19:54:16'),
  ('Darko', 247, 53, '2026-05-20 19:54:20', 'legacy_sheet|darko|247|53|2026-05-20 19:54:20'),
  ('Darko', 16, 0, '2026-05-20 19:54:26', 'legacy_sheet|darko|16|0|2026-05-20 19:54:26'),
  ('Darko', 22, 3, '2026-05-20 19:54:37', 'legacy_sheet|darko|22|3|2026-05-20 19:54:37'),
  ('Darko', 26, 4, '2026-05-20 19:54:52', 'legacy_sheet|darko|26|4|2026-05-20 19:54:52'),
  ('Darko', 26, 4, '2026-05-20 19:54:59', 'legacy_sheet|darko|26|4|2026-05-20 19:54:59'),
  ('Darko', 449, 91, '2026-05-20 20:37:01', 'legacy_sheet|darko|449|91|2026-05-20 20:37:01'),
  ('Darko', 21, 1, '2026-05-20 20:37:12', 'legacy_sheet|darko|21|1|2026-05-20 20:37:12'),
  ('Inass', 200, 30, '2026-05-20 21:53:37', 'legacy_sheet|inass|200|30|2026-05-20 21:53:37'),
  ('Inass', 62, 9, '2026-05-20 21:54:14', 'legacy_sheet|inass|62|9|2026-05-20 21:54:14'),
  ('Trogloniño', 23, 3, '2026-05-20 21:56:15', 'legacy_sheet|trogloniño|23|3|2026-05-20 21:56:15'),
  ('Darko', 28, 6, '2026-05-20 21:57:44', 'legacy_sheet|darko|28|6|2026-05-20 21:57:44'),
  ('Darko', 36, 7, '2026-05-20 21:58:05', 'legacy_sheet|darko|36|7|2026-05-20 21:58:05'),
  ('Trogloniño', 214, 47, '2026-05-20 21:58:38', 'legacy_sheet|trogloniño|214|47|2026-05-20 21:58:38'),
  ('Inass', 24, 1, '2026-05-20 21:59:07', 'legacy_sheet|inass|24|1|2026-05-20 21:59:07'),
  ('Darko', 135, 27, '2026-05-20 21:59:20', 'legacy_sheet|darko|135|27|2026-05-20 21:59:20'),
  ('Inass', 62, 18, '2026-05-20 21:59:39', 'legacy_sheet|inass|62|18|2026-05-20 21:59:39'),
  ('Inass', 54, 10, '2026-05-20 22:00:11', 'legacy_sheet|inass|54|10|2026-05-20 22:00:11'),
  ('Inass', 54, 10, '2026-05-20 22:00:18', 'legacy_sheet|inass|54|10|2026-05-20 22:00:18'),
  ('Inass', 29, 4, '2026-05-20 22:00:27', 'legacy_sheet|inass|29|4|2026-05-20 22:00:27'),
  ('Inass', 38, 7, '2026-05-20 22:00:46', 'legacy_sheet|inass|38|7|2026-05-20 22:00:46'),
  ('Inass', 46, 10, '2026-05-20 22:01:11', 'legacy_sheet|inass|46|10|2026-05-20 22:01:11'),
  ('Trogloniño', 162, 27, '2026-05-20 22:01:18', 'legacy_sheet|trogloniño|162|27|2026-05-20 22:01:18'),
  ('Inass', 40, 8, '2026-05-20 22:01:38', 'legacy_sheet|inass|40|8|2026-05-20 22:01:38'),
  ('Trogloniño', 32, 5, '2026-05-20 22:01:42', 'legacy_sheet|trogloniño|32|5|2026-05-20 22:01:42'),
  ('Inass', 33, 5, '2026-05-20 22:01:52', 'legacy_sheet|inass|33|5|2026-05-20 22:01:52'),
  ('Trogloniño', 32, 5, '2026-05-20 22:01:53', 'legacy_sheet|trogloniño|32|5|2026-05-20 22:01:53'),
  ('Trogloniño', 31, 4, '2026-05-20 22:02:05', 'legacy_sheet|trogloniño|31|4|2026-05-20 22:02:05'),
  ('Trogloniño', 32, 5, '2026-05-20 22:02:07', 'legacy_sheet|trogloniño|32|5|2026-05-20 22:02:07'),
  ('Trogloniño', 439, 82, '2026-05-20 22:06:08', 'legacy_sheet|trogloniño|439|82|2026-05-20 22:06:08'),
  ('Trogloniño', 756, 141, '2026-05-21 01:54:04', 'legacy_sheet|trogloniño|756|141|2026-05-21 01:54:04'),
  ('Pavel', 60, 9, '2026-05-21 09:42:01', 'legacy_sheet|pavel|60|9|2026-05-21 09:42:01'),
  ('Pavel', 60, 9, '2026-05-21 09:42:08', 'legacy_sheet|pavel|60|9|2026-05-21 09:42:08'),
  ('dora', 46, 8, '2026-05-21 13:39:12', 'legacy_sheet|dora|46|8|2026-05-21 13:39:12'),
  ('dora', 55, 14, '2026-05-21 13:40:20', 'legacy_sheet|dora|55|14|2026-05-21 13:40:20'),
  ('Darko', 193, 35, '2026-05-21 16:52:59', 'legacy_sheet|darko|193|35|2026-05-21 16:52:59'),
  ('Darko', 17, 1, '2026-05-21 16:53:07', 'legacy_sheet|darko|17|1|2026-05-21 16:53:07'),
  ('Darko', 180, 39, '2026-05-21 17:35:40', 'legacy_sheet|darko|180|39|2026-05-21 17:35:40'),
  ('Darko', 214, 45, '2026-05-21 17:37:39', 'legacy_sheet|darko|214|45|2026-05-21 17:37:39'),
  ('tirolka', 17, 0, '2026-05-22 09:43:06', 'legacy_sheet|tirolka|17|0|2026-05-22 09:43:06'),
  ('tirolka', 17, 0, '2026-05-22 09:43:19', 'legacy_sheet|tirolka|17|0|2026-05-22 09:43:19'),
  ('tirolka', 18, 0, '2026-05-22 09:43:31', 'legacy_sheet|tirolka|18|0|2026-05-22 09:43:31'),
  ('tirolka', 108, 22, '2026-05-22 09:44:29', 'legacy_sheet|tirolka|108|22|2026-05-22 09:44:29'),
  ('Trogloniño', 33, 7, '2026-05-22 11:49:47', 'legacy_sheet|trogloniño|33|7|2026-05-22 11:49:47'),
  ('Fero', 23, 2, '2026-05-22 11:56:33', 'legacy_sheet|fero|23|2|2026-05-22 11:56:33'),
  ('Fero', 17, 1, '2026-05-22 11:56:42', 'legacy_sheet|fero|17|1|2026-05-22 11:56:42'),
  ('Darko', 77, 11, '2026-05-22 19:05:53', 'legacy_sheet|darko|77|11|2026-05-22 19:05:53'),
  ('Trogloniño', 646, 144, '2026-05-22 23:30:39', 'legacy_sheet|trogloniño|646|144|2026-05-22 23:30:39'),
  ('Darko', 107, 25, '2026-05-22 23:53:36', 'legacy_sheet|darko|107|25|2026-05-22 23:53:36'),
  ('Darko', 379, 84, '2026-05-22 23:56:37', 'legacy_sheet|darko|379|84|2026-05-22 23:56:37'),
  ('Darko', 81, 17, '2026-05-22 23:57:20', 'legacy_sheet|darko|81|17|2026-05-22 23:57:20'),
  ('Darko', 512, 133, '2026-05-23 00:00:56', 'legacy_sheet|darko|512|133|2026-05-23 00:00:56'),
  ('Darko', 397, 86, '2026-05-23 00:04:36', 'legacy_sheet|darko|397|86|2026-05-23 00:04:36'),
  ('Darko', 16, 0, '2026-05-23 00:04:52', 'legacy_sheet|darko|16|0|2026-05-23 00:04:52'),
  ('Pavel', 60, 9, '2026-05-23 18:17:27', 'legacy_sheet|pavel|60|9|2026-05-23 18:17:27'),
  ('Pavel', 26, 2, '2026-05-23 18:17:41', 'legacy_sheet|pavel|26|2|2026-05-23 18:17:41'),
  ('Pavel', 48, 12, '2026-05-23 18:18:15', 'legacy_sheet|pavel|48|12|2026-05-23 18:18:15'),
  ('Mibombo', 50, 7, '2026-05-24 20:18:31', 'legacy_sheet|mibombo|50|7|2026-05-24 20:18:31'),
  ('Mibombo', 27, 5, '2026-05-24 20:18:47', 'legacy_sheet|mibombo|27|5|2026-05-24 20:18:47'),
  ('Mibombo', 18, 1, '2026-05-24 20:18:56', 'legacy_sheet|mibombo|18|1|2026-05-24 20:18:56'),
  ('Mibombo', 26, 3, '2026-05-24 20:19:09', 'legacy_sheet|mibombo|26|3|2026-05-24 20:19:09'),
  ('Mibombo', 33, 3, '2026-05-24 20:19:25', 'legacy_sheet|mibombo|33|3|2026-05-24 20:19:25'),
  ('Mibombo', 59, 11, '2026-05-25 13:29:42', 'legacy_sheet|mibombo|59|11|2026-05-25 13:29:42'),
  ('Darko', 18, 1, '2026-05-26 00:30:20', 'legacy_sheet|darko|18|1|2026-05-26 00:30:20'),
  ('Trogloniño', 221, 47, '2026-05-27 20:33:33', 'legacy_sheet|trogloniño|221|47|2026-05-27 20:33:33'),
  ('Maky', 37, 8, '2026-05-27 22:54:55', 'legacy_sheet|maky|37|8|2026-05-27 22:54:55'),
  ('tt', 29, 3, '2026-06-06 16:46:39', 'legacy_sheet|tt|29|3|2026-06-06 16:46:39'),
  ('Inass', 24, 3, '2026-06-07 18:50:55', 'legacy_sheet|inass|24|3|2026-06-07 18:50:55'),
  ('Trogloniño', 468, 101, '2026-06-07 19:19:49', 'legacy_sheet|trogloniño|468|101|2026-06-07 19:19:49'),
  ('Trogloniño', 441, 84, '2026-06-09 13:38:22', 'legacy_sheet|trogloniño|441|84|2026-06-09 13:38:22'),
  ('Mia', 17, 0, '2026-06-10 19:51:57', 'legacy_sheet|mia|17|0|2026-06-10 19:51:57'),
  ('Mia', 18, 0, '2026-06-10 19:52:08', 'legacy_sheet|mia|18|0|2026-06-10 19:52:08'),
  ('Mia', 18, 1, '2026-06-10 19:52:18', 'legacy_sheet|mia|18|1|2026-06-10 19:52:18'),
  ('Mia', 60, 14, '2026-06-10 19:52:48', 'legacy_sheet|mia|60|14|2026-06-10 19:52:48'),
  ('Vesna_S', 19, 1, '2026-06-10 19:53:19', 'legacy_sheet|vesna_s|19|1|2026-06-10 19:53:19'),
  ('Vesna_S', 30, 2, '2026-06-10 19:53:36', 'legacy_sheet|vesna_s|30|2|2026-06-10 19:53:36'),
  ('Vesna_S', 18, 0, '2026-06-10 19:53:45', 'legacy_sheet|vesna_s|18|0|2026-06-10 19:53:45'),
  ('Vesna_S', 19, 0, '2026-06-10 19:53:58', 'legacy_sheet|vesna_s|19|0|2026-06-10 19:53:58'),
  ('Vesna_S', 32, 5, '2026-06-10 19:54:15', 'legacy_sheet|vesna_s|32|5|2026-06-10 19:54:15'),
  ('Vesna_S', 47, 3, '2026-06-10 20:31:47', 'legacy_sheet|vesna_s|47|3|2026-06-10 20:31:47'),
  ('Vesna_S', 41, 9, '2026-06-10 20:32:05', 'legacy_sheet|vesna_s|41|9|2026-06-10 20:32:05'),
  ('Vesna_S', 34, 8, '2026-06-10 20:32:21', 'legacy_sheet|vesna_s|34|8|2026-06-10 20:32:21'),
  ('Vesna_S', 84, 22, '2026-06-10 20:33:03', 'legacy_sheet|vesna_s|84|22|2026-06-10 20:33:03'),
  ('Vesna_S', 57, 13, '2026-06-10 20:33:31', 'legacy_sheet|vesna_s|57|13|2026-06-10 20:33:31'),
  ('Vesna_S', 18, 0, '2026-06-10 20:33:41', 'legacy_sheet|vesna_s|18|0|2026-06-10 20:33:41'),
  ('Vesna_S', 33, 5, '2026-06-10 20:34:00', 'legacy_sheet|vesna_s|33|5|2026-06-10 20:34:00'),
  ('Vesna_S', 53, 5, '2026-06-10 20:34:30', 'legacy_sheet|vesna_s|53|5|2026-06-10 20:34:30'),
  ('Vesna_S', 24, 2, '2026-06-10 20:34:43', 'legacy_sheet|vesna_s|24|2|2026-06-10 20:34:43'),
  ('Vesna_S', 35, 6, '2026-06-10 20:35:00', 'legacy_sheet|vesna_s|35|6|2026-06-10 20:35:00'),
  ('Vesna_S', 37, 9, '2026-06-10 20:35:20', 'legacy_sheet|vesna_s|37|9|2026-06-10 20:35:20'),
  ('Vesna_S', 100, 16, '2026-06-10 20:36:12', 'legacy_sheet|vesna_s|100|16|2026-06-10 20:36:12'),
  ('Mibombo', 49, 9, '2026-06-10 21:08:01', 'legacy_sheet|mibombo|49|9|2026-06-10 21:08:01'),
  ('Mibombo', 20, 0, '2026-06-10 21:08:12', 'legacy_sheet|mibombo|20|0|2026-06-10 21:08:12'),
  ('Mibombo', 17, 0, '2026-06-10 21:08:22', 'legacy_sheet|mibombo|17|0|2026-06-10 21:08:22'),
  ('Mibombo', 81, 17, '2026-06-10 21:09:06', 'legacy_sheet|mibombo|81|17|2026-06-10 21:09:06'),
  ('Trogloniño', 32, 2, '2026-06-10 22:19:49', 'legacy_sheet|trogloniño|32|2|2026-06-10 22:19:49'),
  ('Trogloniño', 368, 82, '2026-06-10 22:23:12', 'legacy_sheet|trogloniño|368|82|2026-06-10 22:23:12'),
  ('Vesna_S', 48, 13, '2026-06-12 15:01:53', 'legacy_sheet|vesna_s|48|13|2026-06-12 15:01:53'),
  ('Vesna_S', 65, 6, '2026-06-12 15:02:28', 'legacy_sheet|vesna_s|65|6|2026-06-12 15:02:28'),
  ('Vesna_S', 118, 27, '2026-06-12 15:03:28', 'legacy_sheet|vesna_s|118|27|2026-06-12 15:03:28'),
  ('Ninova ruka', 115, 22, '2026-06-12 15:19:09', 'legacy_sheet|ninova ruka|115|22|2026-06-12 15:19:09'),
  ('Trogloniño', 39, 8, '2026-06-12 23:14:05', 'legacy_sheet|trogloniño|39|8|2026-06-12 23:14:05'),
  ('Trogloniño', 329, 75, '2026-06-12 23:17:15', 'legacy_sheet|trogloniño|329|75|2026-06-12 23:17:15'),
  ('Trogloniño', 590, 132, '2026-06-12 23:22:37', 'legacy_sheet|trogloniño|590|132|2026-06-12 23:22:37'),
  ('Trogloniño', 515, 123, '2026-06-15 10:43:05', 'legacy_sheet|trogloniño|515|123|2026-06-15 10:43:05'),
  ('Trogloniño', 371, 73, '2026-06-15 15:29:44', 'legacy_sheet|trogloniño|371|73|2026-06-15 15:29:44'),
  ('Trogloniño', 223, 50, '2026-06-15 16:36:20', 'legacy_sheet|trogloniño|223|50|2026-06-15 16:36:20'),
  ('Trogloniño', 1043, 213, '2026-06-21 07:47:02', 'legacy_sheet|trogloniño|1043|213|2026-06-21 07:47:02'),
  ('Trogloniño', 17, 0, '2026-06-21 17:28:29', 'legacy_sheet|trogloniño|17|0|2026-06-21 17:28:29'),
  ('Vesna_S', 139, 27, '2026-06-26 07:13:36', 'legacy_sheet|vesna_s|139|27|2026-06-26 07:13:36'),
  ('Trogloniño', 217, 47, '2026-06-27 19:45:36', 'legacy_sheet|trogloniño|217|47|2026-06-27 19:45:36'),
  ('Trogloniño', 17, 0, '2026-06-27 21:27:00', 'legacy_sheet|trogloniño|17|0|2026-06-27 21:27:00'),
  ('Trogloniño', 419, 67, '2026-06-27 21:30:32', 'legacy_sheet|trogloniño|419|67|2026-06-27 21:30:32'),
  ('Darko', 163, 30, '2026-06-30 01:28:05', 'legacy_sheet|darko|163|30|2026-06-30 01:28:05'),
  ('Darko', 60, 17, '2026-06-30 01:28:43', 'legacy_sheet|darko|60|17|2026-06-30 01:28:43')
)
insert into public.sov_runner_scores(player_name, score, bats, date_text, client_key, source, played_at, metadata)
select player_name, score, bats, date_text, client_key, 'legacy_sheet',
       (date_text::timestamp at time zone 'Europe/Zagreb'),
       jsonb_build_object('import', 'Spele run score - Sheet1.csv', 'seed', 'v1.4.29c')
from seed
on conflict (client_key) do nothing;
