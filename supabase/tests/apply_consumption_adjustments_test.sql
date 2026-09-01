-- ============================================================
-- pgTAP tests: apply_consumption_adjustments()
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the
-- fixtures and table clears below never persist. now() is frozen per
-- transaction, so elapsed time comes from backdated stamps (exact) or
-- purchased_at dates (adds a fraction of a day; asserted as ranges).
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(27);

-- ------------------------------------------------------------
-- Fixtures. Clear the function's input/output so seeded data cannot
-- shift the counts.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;
delete from public.user_product_consumption;

-- Two users: aa01 opted in, aa02 opted out. GoTrue token columns must be
-- '' rather than NULL.
insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data,
  confirmation_token, recovery_token,
  email_change, email_change_token_new, email_change_token_current,
  phone_change, phone_change_token, reauthentication_token
)
values
  ('00000000-0000-0000-0000-000000000000',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'authenticated', 'authenticated', 'apply-test-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Apply Test 1"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02',
   'authenticated', 'authenticated', 'apply-test-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Apply Test 2"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'Apply Test 1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', 'Apply Test 2')
on conflict (id) do nothing;

update public.profiles set auto_adjust_enabled = true
where id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01';

-- Products: shelf_life_days null keeps the expiry/location triggers inert.
-- ee06 is weight-priced and must be skipped.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'TEST-ADJ-01', 9991001,
   'Adj Whole Unit',    '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 'TEST-ADJ-02', 9991002,
   'Adj Dry Good 90d',  '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 'TEST-ADJ-03', 9991003,
   'Adj Dry Good 500d', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', 'TEST-ADJ-04', 9991004,
   'Adj Dormant 400d',  '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 'TEST-ADJ-05', 9991005,
   'Adj FEFO Multi',    '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee06', 'TEST-ADJ-06', 9991006,
   'Adj Weight Priced', '1 kg', 'kg', '', 'SOLD_BY_WEIGHT', 'kg', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee07', 'TEST-ADJ-07', 9991007,
   'Adj No Rate',       '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee08', 'TEST-ADJ-08', 9991008,
   'Adj Opted Out',     '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee09', 'TEST-ADJ-09', 9991009,
   'Adj Zero Qty',      '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- Rates inserted directly; the estimator is not under test here.
insert into public.user_product_consumption
  (user_id, product_id, est_daily_rate, source, confidence, last_computed_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 0.2500, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 0.0027, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 0.0027, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', 0.0100, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 0.3300, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee06', 1.0000, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee09', 0.5000, 'history', 0.20, now()),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee08', 1.0000, 'history', 0.20, now());

-- Lots. Stamped lots give exact elapsed days; date-anchored lots add a
-- fraction of a day.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  -- whole-unit removal: budget 0.2 + 0.25 * 10 = 2.7000
  ('dddddddd-dddd-dddd-dddd-dddddddddd01', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 3, 'ea', 'pantry',
   current_date - 30, null, now() - interval '10 days', 0.2000),
  -- 90-day dry good: budget ~0.243, nothing removed
  ('dddddddd-dddd-dddd-dddd-dddddddddd02', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 2, 'ea', 'pantry',
   current_date - 90, null, null, 0),
  -- 500-day dry good: budget ~1.35, one unit removed
  ('dddddddd-dddd-dddd-dddd-dddddddddd03', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 2, 'ea', 'pantry',
   current_date - 500, null, null, 0),
  -- 400-day dormant: budget ~4.0, clamps to on-hand
  ('dddddddd-dddd-dddd-dddd-dddddddddd04', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', 2, 'ea', 'pantry',
   current_date - 400, null, null, 0),
  -- FEFO trio: budget 0.1 + 0.33 * 10 = 3.4000 across dd05..dd07
  ('dddddddd-dddd-dddd-dddd-dddddddddd05', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 1, 'ea', 'pantry',
   current_date - 20, current_date + 1, now() - interval '10 days', 0),
  ('dddddddd-dddd-dddd-dddd-dddddddddd06', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 2, 'ea', 'pantry',
   current_date - 15, current_date + 5, now() - interval '10 days', 0.1000),
  ('dddddddd-dddd-dddd-dddd-dddddddddd07', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 5, 'ea', 'pantry',
   current_date - 10, null, now() - interval '10 days', 0),
  -- weight-priced: skipped
  ('dddddddd-dddd-dddd-dddd-dddddddddd08', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee06', 5, 'ea', 'fridge',
   current_date - 30, null, now() - interval '10 days', 0),
  -- no consumption rate: skipped
  ('dddddddd-dddd-dddd-dddd-dddddddddd09', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee07', 4, 'ea', 'pantry',
   current_date - 30, null, null, 0),
  -- opted-out user: skipped
  ('dddddddd-dddd-dddd-dddd-dddddddddd10', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee08', 4, 'ea', 'pantry',
   current_date - 50, null, null, 0),
  -- zero quantity, zero pending: not processed
  ('dddddddd-dddd-dddd-dddd-dddddddddd11', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee09', 0, 'ea', 'pantry',
   current_date - 120, null, now() - interval '100 days', 0);

-- ------------------------------------------------------------
-- Function shape
-- ------------------------------------------------------------
select has_function('public', 'apply_consumption_adjustments', '{}'::name[],
  'apply_consumption_adjustments() exists');

select ok(
  (select not p.prosecdef
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'apply_consumption_adjustments'),
  'function is SECURITY INVOKER');

select ok(
  (select 'search_path=public' = any(p.proconfig)
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'apply_consumption_adjustments'),
  'function sets search_path = public');

select ok(
  not has_function_privilege('authenticated',
    'public.apply_consumption_adjustments()', 'EXECUTE'),
  'authenticated cannot execute (cron-only)');

select ok(
  not has_function_privilege('anon',
    'public.apply_consumption_adjustments()', 'EXECUTE'),
  'anon cannot execute (cron-only)');

-- ------------------------------------------------------------
-- First run
-- ------------------------------------------------------------
create temp table run1 as select * from public.apply_consumption_adjustments();

select is((select count(*) from run1), 5::bigint,
  'first run reports 5 adjusted lots');

select ok(
  (select r.delta = -2 and r.new_quantity = 1 from run1 r
   where r.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd01'),
  'summary row matches the whole-unit removal');

select is(
  (select ii.quantity::text || '|' || ii.pending_fraction::text || '|'
          || (ii.last_auto_adjusted_at = now())::text
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd01'),
  '1.000|0.7000|true',
  'whole-unit removal: 3 - 2 = 1, remainder 0.7 kept, lot stamped');

select is(
  (select a.delta::text from public.inventory_adjustments a
   where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd01'
     and a.reason = 'auto'),
  '-2.000',
  'audit row records the -2 auto adjustment');

select ok(
  (select ii.quantity = 2 from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd02')
  and (select count(*) = 0 from public.inventory_adjustments a
       where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd02'),
  '90-day dry good removes nothing (0.0027 * 90 < 1 is correct, not a bug)');

select ok(
  (select ii.pending_fraction >= 0.2430 and ii.pending_fraction < 0.2470
          and ii.last_auto_adjusted_at = now()
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd02'),
  '90-day dry good accrues ~0.243 pending and is stamped');

select ok(
  (select ii.quantity = 1 from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd03')
  and (select a.delta = -1.000 from public.inventory_adjustments a
       where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd03'),
  '500-day dry good crosses 1.0 and removes one unit');

select ok(
  (select ii.pending_fraction >= 0.3500 and ii.pending_fraction < 0.3540
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd03'),
  '500-day dry good keeps the ~0.35 remainder');

select is(
  (select ii.quantity::text || '|' || ii.pending_fraction::text
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd04'),
  '0.000|0.0000',
  'dormant lot clamps to on-hand, debt discarded, row survives');

select is(
  (select a.delta::text from public.inventory_adjustments a
   where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd04'),
  '-2.000',
  'dormant lot audit records only the on-hand amount');

select ok(
  (select ii.quantity = 0 and ii.pending_fraction = 0
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd05')
  and (select a.delta = -1.000 from public.inventory_adjustments a
       where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd05'),
  'FEFO: soonest-expiring lot drains first');

select ok(
  (select ii.quantity = 0 and ii.pending_fraction = 0
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd06')
  and (select a.delta = -2.000 from public.inventory_adjustments a
       where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd06'),
  'FEFO: spill drains the next lot and consolidates its old pending');

select ok(
  (select ii.quantity = 5 and ii.pending_fraction = 0.4000
          and ii.last_auto_adjusted_at = now()
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd07')
  and (select count(*) = 0 from public.inventory_adjustments a
       where a.inventory_item_id = 'dddddddd-dddd-dddd-dddd-dddddddddd07'),
  'FEFO: NULLS-LAST survivor keeps the 0.4 remainder, no audit row');

select is(
  (select count(*)::int from public.inventory_adjustments a where a.reason = 'auto'),
  5,
  'exactly 5 auto audit rows');

select ok(
  (select ii.quantity = 5 and ii.last_auto_adjusted_at = now() - interval '10 days'
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd08'),
  'weight-priced lot untouched, stamp unchanged');

select ok(
  (select ii.quantity = 4 and ii.last_auto_adjusted_at is null
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd09'),
  'lot without a consumption rate untouched');

select ok(
  (select ii.quantity = 4 and ii.last_auto_adjusted_at is null
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd10'),
  'opted-out user untouched');

select ok(
  (select ii.last_auto_adjusted_at = now() - interval '100 days'
   from public.inventory_items ii
   where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd11'),
  'zero-quantity zero-pending lot not processed');

-- ------------------------------------------------------------
-- Idempotency: stamps equal the frozen now(), so elapsed is zero
-- ------------------------------------------------------------
select is((select count(*) from public.apply_consumption_adjustments()), 0::bigint,
  'immediate second run removes nothing');

select ok(
  (select count(*) = 5 from public.inventory_adjustments a where a.reason = 'auto')
  and (select ii.pending_fraction = 0.4000 from public.inventory_items ii
       where ii.id = 'dddddddd-dddd-dddd-dddd-dddddddddd07'),
  'second run adds no audit rows and leaves pendings intact');

-- ------------------------------------------------------------
-- Cron job
-- ------------------------------------------------------------
select is(
  (select count(*)::int from cron.job where jobname = 'auto-adjust-consumption-daily'),
  1,
  'nightly cron job is scheduled');

select ok(
  (select j.schedule = '10 3 * * *'
          and j.command like '%estimate_consumption_rates%'
          and j.command like '%apply_consumption_adjustments%'
   from cron.job j where j.jobname = 'auto-adjust-consumption-daily'),
  'cron job refreshes rates then applies them at 03:10 UTC');

select * from finish();
rollback;
