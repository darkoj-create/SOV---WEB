-- SOV v6.1.45av — stable Arhivar submission review endpoint
-- Removes the permanent 404/fallback path used by arhivar-submissions-review.js.

begin;

create or replace function public.sov_update_speleo_submission_review(
  p_submission_id uuid,
  p_status text,
  p_missing_categories text[] default '{}'::text[],
  p_archivist_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
declare
  v_status text := lower(trim(coalesce(p_status,'')));
  v_count integer := 0;
begin
  if auth.uid() is null then
    raise exception 'Nisi prijavljen.' using errcode = '42501';
  end if;
  if not public.sov_submissions_is_reviewer() then
    raise exception 'Samo admin/arhivar može ažurirati review predaje.' using errcode = '42501';
  end if;
  if v_status not in ('submitted','needs_changes','approved','rejected') then
    raise exception 'Nepodržan status predaje: %', p_status using errcode = '22023';
  end if;

  update public.speleo_object_submissions
  set status = v_status,
      missing_categories = coalesce(p_missing_categories, '{}'::text[]),
      archivist_note = p_archivist_note,
      reviewed_by = auth.uid(),
      reviewed_at = now(),
      updated_at = now()
  where id = p_submission_id;

  get diagnostics v_count = row_count;
  if v_count = 0 then
    raise exception 'Predaja ne postoji.' using errcode = 'P0002';
  end if;

  return jsonb_build_object(
    'ok', true,
    'submission_id', p_submission_id,
    'status', v_status,
    'missing_categories', coalesce(p_missing_categories, '{}'::text[])
  );
end;
$function$;

revoke all on function public.sov_update_speleo_submission_review(uuid,text,text[],text) from public;
revoke all on function public.sov_update_speleo_submission_review(uuid,text,text[],text) from anon;
grant execute on function public.sov_update_speleo_submission_review(uuid,text,text[],text) to authenticated;
grant execute on function public.sov_update_speleo_submission_review(uuid,text,text[],text) to service_role;

comment on function public.sov_update_speleo_submission_review(uuid,text,text[],text) is
  'Reviewer-only endpoint for submitted/needs_changes/approved/rejected workflow updates.';

commit;
