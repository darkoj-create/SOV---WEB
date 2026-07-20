-- SOV v6.1.45av — Spelo Runner leaderboard security hardening
-- Keep public high-score reads while removing view-owner privilege bypass and
-- unnecessary anonymous write grants.

begin;

create or replace view public.sov_runner_leaderboard
with (security_invoker = true)
as
select
  player_name as name,
  score,
  bats,
  date_text,
  source,
  played_at,
  created_at
from public.sov_runner_scores
where score > 0
  and bats >= 0
  and length(player_name) between 1 and 80;

revoke all on public.sov_runner_leaderboard from public;
revoke all on public.sov_runner_leaderboard from anon;
revoke all on public.sov_runner_leaderboard from authenticated;
grant select on public.sov_runner_leaderboard to anon, authenticated, service_role;

-- Public users only need leaderboard reads. Authenticated staff retain table
-- DML grants, with the existing staff RLS policy deciding who may write.
revoke insert, update, delete, truncate, references, trigger
  on public.sov_runner_scores from anon;
grant select on public.sov_runner_scores to anon;

comment on view public.sov_runner_leaderboard is
  'Public Spelo Runner leaderboard; security_invoker enforces sov_runner_scores RLS.';

commit;
