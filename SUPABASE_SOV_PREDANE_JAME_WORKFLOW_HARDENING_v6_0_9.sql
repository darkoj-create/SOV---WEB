
-- SOV web v6.0.9 — Predane jame workflow hardening
-- Safe/idempotent patch. It does not delete or rename existing data.

alter table if exists public.speleo_object_submissions
  add column if not exists status text default 'submitted',
  add column if not exists missing_categories text[] default array[]::text[],
  add column if not exists archivist_note text,
  add column if not exists reviewed_at timestamptz,
  add column if not exists reviewed_by uuid,
  add column if not exists approved_object_id text,
  add column if not exists source text,
  add column if not exists metadata jsonb default '{}'::jsonb,
  add column if not exists updated_at timestamptz default now();

create index if not exists idx_sov_submissions_status_created on public.speleo_object_submissions(status, created_at desc);
create index if not exists idx_sov_submissions_object_name on public.speleo_object_submissions(object_name);

create or replace function public.sov_update_speleo_submission_review(
  p_submission_id uuid,
  p_status text,
  p_missing_categories text[] default array[]::text[],
  p_archivist_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path=public
as $$
declare
  v_email text := auth.email();
  v_role text;
  v_out jsonb;
begin
  select coalesce(role::text,'') into v_role from public.profiles where id = auth.uid();
  if auth.uid() is null then
    raise exception 'Not authenticated';
  end if;
  if lower(coalesce(v_role,'')) not in ('webmaster','admin','arhivar') then
    raise exception 'Not allowed: arhivar role required';
  end if;
  if p_status not in ('submitted','needs_changes','approved','rejected') then
    raise exception 'Invalid status: %', p_status;
  end if;
  update public.speleo_object_submissions
     set status = p_status,
         missing_categories = coalesce(p_missing_categories,array[]::text[]),
         archivist_note = p_archivist_note,
         reviewed_at = case when p_status='submitted' then reviewed_at else now() end,
         reviewed_by = case when p_status='submitted' then reviewed_by else auth.uid() end,
         updated_at = now(),
         metadata = coalesce(metadata,'{}'::jsonb) || jsonb_build_object('last_review_action',p_status,'last_review_by',v_email,'last_review_at',now())
   where id = p_submission_id
   returning jsonb_build_object('id',id,'status',status,'missing_categories',missing_categories,'reviewed_at',reviewed_at) into v_out;
  if v_out is null then
    raise exception 'Submission not found: %', p_submission_id;
  end if;
  return v_out;
end;
$$;

grant execute on function public.sov_update_speleo_submission_review(uuid,text,text[],text) to authenticated;

create or replace function public.sov_predane_jame_workflow_health()
returns jsonb
language plpgsql
security definer
set search_path=public
as $$
declare
  v jsonb;
begin
  select jsonb_build_object(
    'ok', true,
    'build','6.0.9',
    'submissions', (select count(*) from public.speleo_object_submissions),
    'submitted', (select count(*) from public.speleo_object_submissions where status='submitted'),
    'needs_changes', (select count(*) from public.speleo_object_submissions where status='needs_changes'),
    'approved', (select count(*) from public.speleo_object_submissions where status='approved'),
    'rejected', (select count(*) from public.speleo_object_submissions where status='rejected'),
    'files', (select count(*) from public.speleo_object_submission_files)
  ) into v;
  return v;
exception when others then
  return jsonb_build_object('ok',false,'build','6.0.9','error',sqlerrm);
end;
$$;

grant execute on function public.sov_predane_jame_workflow_health() to authenticated;
