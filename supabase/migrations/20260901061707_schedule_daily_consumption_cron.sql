-- ============================================================
-- Migration: nightly consumption cron job
-- ============================================================
-- pg_cron ships preloaded in the Supabase image (cron.database_name =
-- postgres); creating the extension activates it. The job runs as the
-- scheduling role (postgres), which bypasses RLS, so both functions --
-- which derive user_id from rows rather than auth.uid() -- cover all
-- users.
--
-- Both statements run in order over one libpq round trip, in a single
-- implicit transaction: rates refresh before they are applied, and a
-- failure rolls both back for a clean retry the next night. Run history
-- is in cron.job_run_details. cron.schedule() upserts by job name.
-- ------------------------------------------------------------
create extension if not exists pg_cron;

select cron.schedule(
  'auto-adjust-consumption-daily',
  '10 3 * * *',
  $job$
    select public.estimate_consumption_rates();
    select public.apply_consumption_adjustments();
  $job$
);
