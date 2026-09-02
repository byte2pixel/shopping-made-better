-- ============================================================
-- pgTAP tests: estimate_consumption_rates()
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the
-- fixtures and table clears below never persist.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(15);

-- ------------------------------------------------------------
-- Fixtures. Clear estimator input/output so seeded data cannot shift
-- the counts.
-- ------------------------------------------------------------
delete from public.user_product_consumption;
delete from public.purchase_history;  -- cascades to purchase_history_items

-- Test user. GoTrue token columns must be '' rather than NULL.
insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data,
  confirmation_token, recovery_token,
  email_change, email_change_token_new, email_change_token_current,
  phone_change, phone_change_token, reauthentication_token
)
values (
  '00000000-0000-0000-0000-000000000000',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  'authenticated', 'authenticated',
  'estimator-test@smb.test',
  crypt('password123', gen_salt('bf')),
  now(), now(), now(),
  '{"provider":"email","providers":["email"]}',
  '{"display_name":"Estimator Test"}',
  '', '', '', '', '', '', '', ''
)
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Estimator Test')
on conflict (id) do nothing;

-- Products: one per branch under test.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'TEST-EST-01', 9990001,
   'Test History Dry Good',    '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 365, 'dry_goods'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'TEST-EST-02', 9990002,
   'Test Fallback Bread',      '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 7,   'bakery_bread'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03', 'TEST-EST-03', 9990003,
   'Test Single Purchase',     '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 20,  'deli_prepared'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 'TEST-EST-04', 9990004,
   'Test Floor Bread',         '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 7,   'bakery_bread'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 'TEST-EST-05', 9990005,
   'Test No Shelf Life Short', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06', 'TEST-EST-06', 9990006,
   'Test No Shelf Life Hist',  '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07', 'TEST-EST-07', 9990007,
   'Test Weight Priced',       '1 kg', 'kg', '', 'SOLD_BY_WEIGHT', 'kg', 365, 'dry_goods'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08', 'TEST-EST-08', 9990008,
   'Test Clamp',               '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09', 'TEST-EST-09', 9990009,
   'Test Manual Preserved',    '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 30,  'cheese');

-- Trips at controlled day offsets (store_id null is allowed).
insert into public.purchase_history (id, user_id, store_id, purchased_at, total_amount)
values
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', null, now(),                      10.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc05', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', null, now() - interval '5 days',  10.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc14', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', null, now() - interval '14 days', 10.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc20', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', null, now() - interval '20 days', 10.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc88', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', null, now() - interval '88 days', 10.00);

insert into public.purchase_history_items (purchase_id, product_id, quantity, price_paid)
values
  -- history branch: 2 trips, 20-day span, (6 - 3) / 20 = 0.1500
  ('cccccccc-cccc-cccc-cccc-cccccccccc20', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 3, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 3, 1.00),
  -- fallback: 2 trips but 5-day span < 14 -> 1/7
  ('cccccccc-cccc-cccc-cccc-cccccccccc05', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 1, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 1, 1.00),
  -- single purchase -> 1/20
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03', 1, 1.00),
  -- perishable floor: (4 - 2) / 88 = 0.0227 raw, floored to 1/7
  ('cccccccc-cccc-cccc-cccc-cccccccccc88', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 2, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 2, 1.00),
  -- no shelf life + failed guard -> skipped
  ('cccccccc-cccc-cccc-cccc-cccccccccc05', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 1, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 1, 1.00),
  -- no shelf life + valid history -> (4 - 2) / 20 = 0.1000
  ('cccccccc-cccc-cccc-cccc-cccccccccc20', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06', 2, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06', 2, 1.00),
  -- weight-priced -> skipped despite valid history
  ('cccccccc-cccc-cccc-cccc-cccccccccc20', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07', 3, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07', 3, 1.00),
  -- clamp: (100 - 50) / 14 = 3.5714 -> 1.0000
  ('cccccccc-cccc-cccc-cccc-cccccccccc14', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08', 50, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08', 50, 1.00),
  -- manual guard: history would compute 0.1500 but the manual row must win
  ('cccccccc-cccc-cccc-cccc-cccccccccc20', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09', 3, 1.00),
  ('cccccccc-cccc-cccc-cccc-cccccccccc00', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09', 3, 1.00);

-- Manual row the estimator must not overwrite.
insert into public.user_product_consumption
  (user_id, product_id, est_daily_rate, source, confidence, last_computed_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09',
   0.5000, 'manual', 1.00, now());

-- ------------------------------------------------------------
-- Function shape
-- ------------------------------------------------------------
select has_function('public', 'estimate_consumption_rates', '{}'::name[],
  'estimate_consumption_rates() exists');

select ok(
  (select not p.prosecdef
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'estimate_consumption_rates'),
  'estimator is SECURITY INVOKER');

select ok(
  (select 'search_path=public' = any(p.proconfig)
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'estimate_consumption_rates'),
  'estimator sets search_path = public');

-- ------------------------------------------------------------
-- Branch behaviour
-- ------------------------------------------------------------
select is(public.estimate_consumption_rates(), 6,
  'first run upserts 6 rows (manual + skips excluded)');

select is(
  (select est_daily_rate::text || '|' || source || '|' || confidence::text
   from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01'),
  '0.1500|history|0.20',
  'history branch fires for a 20-day span: (6-3)/20, sample-size confidence');

select is(
  (select est_daily_rate::text || '|' || source || '|' || confidence::text
   from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02'),
  '0.1429|shelf_life|0.10',
  'shelf-life fallback fires for a 5-day span: 1/7');

select is(
  (select est_daily_rate::text || '|' || source || '|' || confidence::text
   from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03'),
  '0.0500|shelf_life|0.10',
  'single purchase falls back to shelf life: 1/20');

select is(
  (select est_daily_rate::text || '|' || source || '|' || confidence::text
   from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04'),
  '0.1429|history|0.20',
  'perishable floor: 88-day bread gap floored from 0.0227 to 1/7');

select is(
  (select count(*)::int from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05'),
  0,
  'no shelf life + failed guard writes no row');

select is(
  (select est_daily_rate::text || '|' || source || '|' || confidence::text
   from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06'),
  '0.1000|history|0.20',
  'valid history rescues a product with no shelf life');

select is(
  (select count(*)::int from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07'),
  0,
  'weight-priced (kg) product is skipped');

select is(
  (select est_daily_rate::text from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08'),
  '1.0000',
  'rate is clamped to 1.0/day');

select is(
  (select est_daily_rate::text || '|' || source || '|' || confidence::text
   from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
     and product_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09'),
  '0.5000|manual|1.00',
  'manual row is never overwritten');

-- ------------------------------------------------------------
-- Idempotency
-- ------------------------------------------------------------
select is(public.estimate_consumption_rates(), 6,
  'second run updates the same 6 rows');

select is(
  (select count(*)::int from public.user_product_consumption
   where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
  7,
  'row count unchanged after rerun (6 estimated + 1 manual)');

select * from finish();
rollback;
