-- ============================================================
-- pgTAP tests: inventory_adjustments_detail (weekly digest, SCRUM-224)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the fixtures
-- and table clears below never persist. now() is frozen per transaction, so the
-- 6- and 8-day rows land either side of the view's 7-day window every run.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(22);

-- ------------------------------------------------------------
-- Fixtures. Clear the tables the view reads so seeded data cannot
-- shift the counts.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;
delete from public.user_product_stock_settings;
delete from public.user_product_consumption;

-- Two users so RLS can be exercised. GoTrue token columns must be ''
-- rather than NULL.
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
   'dadadada-dada-dada-dada-dadadadada01',
   'authenticated', 'authenticated', 'digest-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Digest 1"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'dadadada-dada-dada-dada-dadadadada02',
   'authenticated', 'authenticated', 'digest-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Digest 2"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('dadadada-dada-dada-dada-dadadadada01', 'Digest 1'),
  ('dadadada-dada-dada-dada-dadadadada02', 'Digest 2')
on conflict (id) do nothing;

-- shelf_life_days null keeps the expiry and location triggers inert.
-- P1 carries the field-mapping row, P2 the filter cases, P3/P4/P5 the stock
-- columns (each with its own product so productQuantity sums predictably).
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'TEST-DIGEST-01', 9993101,
   'Digest Item One', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'TEST-DIGEST-02', 9993102,
   'Digest Item Two', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03', 'TEST-DIGEST-03', 9993103,
   'Digest Item Three', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 'TEST-DIGEST-04', 9993104,
   'Digest Item Four', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 'TEST-DIGEST-05', 9993105,
   'Digest Item Five', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- Lots 01-08 belong to user 1, lot 90 to user 2.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  ('cccccccc-cccc-cccc-cccc-cccccccccc01', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 2, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc02', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc03', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc04', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc05', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc06', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc07', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc08', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 4, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc30', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03', 2, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc40', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 2, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc41', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 2, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc50', 'dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 0, 'ea', 'pantry', current_date - 10, null, null, 0),
  ('cccccccc-cccc-cccc-cccc-cccccccccc90', 'dadadada-dada-dada-dada-dadadadada02',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 1, 'ea', 'pantry', current_date - 10, null, null, 0);

-- Only P1 has an estimate source, so the mapping assertion has something to read.
insert into public.user_product_consumption
  (user_id, product_id, est_daily_rate, source, confidence)
values
  ('dadadada-dada-dada-dada-dadadadada01',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 0.0215, 'history', 0.8);

-- P4 and P5 carry a threshold; P3 deliberately has none.
insert into public.user_product_stock_settings (user_id, product_id, low_stock_threshold)
values
  ('dadadada-dada-dada-dada-dadadadada01', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04', 3),
  ('dadadada-dada-dada-dada-dadadadada01', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05', 3);

-- ------------------------------------------------------------
-- View shape and grants
-- ------------------------------------------------------------
select columns_are('public', 'inventory_adjustments_detail',
  array['adjustmentId', 'inventoryItemId', 'productId', 'productName', 'imageUrl',
        'delta', 'quantityNow', 'productQuantity', 'lowStockThreshold',
        'estimateSource', 'createdAtEpoch'],
  'view exposes exactly the columns the digest reads');

select ok(
  (select c.reloptions @> array['security_invoker=true']
   from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public' and c.relname = 'inventory_adjustments_detail'),
  'view is security invoker');

select ok(
  has_table_privilege('authenticated', 'public.inventory_adjustments_detail', 'SELECT'),
  'authenticated may read the view');

select ok(
  not has_table_privilege('anon', 'public.inventory_adjustments_detail', 'SELECT'),
  'anon may not read the view');

-- ------------------------------------------------------------
-- One auto row maps every field
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11',
   'cccccccc-cccc-cccc-cccc-cccccccccc01', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '1 hour');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "inventoryItemId" = 'cccccccc-cccc-cccc-cccc-cccccccccc01'),
  1,
  'a recent auto row is listed');

select is(
  (select delta || '|' || "quantityNow" || '|' || "productName" || '|' || "estimateSource"
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11'),
  '-1|2|Digest Item One|history',
  'delta, lot quantity, product name and estimate source all map');

select is(
  (select "createdAtEpoch" from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11'),
  extract(epoch from now() - interval '1 hour')::bigint,
  'createdAtEpoch is the row timestamp in epoch seconds');

reset role;

-- ------------------------------------------------------------
-- Only 'auto' rows are listed
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa21',
   'cccccccc-cccc-cccc-cccc-cccccccccc02', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'manual', now() - interval '2 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa22',
   'cccccccc-cccc-cccc-cccc-cccccccccc02', 'dadadada-dada-dada-dada-dadadadada01',
   0, 'confirmed', now() - interval '3 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa23',
   'cccccccc-cccc-cccc-cccc-cccccccccc02', 'dadadada-dada-dada-dada-dadadadada01',
   1, 'undo', now() - interval '4 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa24',
   'cccccccc-cccc-cccc-cccc-cccccccccc02', 'dadadada-dada-dada-dada-dadadadada01',
   0, 'dismissed', now() - interval '5 hours');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "inventoryItemId" = 'cccccccc-cccc-cccc-cccc-cccccccccc02'),
  0,
  'manual, confirmed, undo and dismissed rows are not digest rows');

reset role;

-- ------------------------------------------------------------
-- The seven-day window
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa31',
   'cccccccc-cccc-cccc-cccc-cccccccccc03', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '6 days'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa32',
   'cccccccc-cccc-cccc-cccc-cccccccccc03', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '8 days');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa31'),
  1,
  'an auto row six days old is inside the window');

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa32'),
  0,
  'an auto row eight days old has fallen out of the window');

reset role;

-- ------------------------------------------------------------
-- A reversed row drops out; a later un-reversed one on the same lot stays
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at, reverses)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12',
   'cccccccc-cccc-cccc-cccc-cccccccccc01', 'dadadada-dada-dada-dada-dadadadada01',
   1, 'undo', now() - interval '30 minutes', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11'),
  0,
  'an undone auto row is not listed');

reset role;

insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa13',
   'cccccccc-cccc-cccc-cccc-cccccccccc01', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '20 minutes');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select "adjustmentId" from public.inventory_adjustments_detail
   where "inventoryItemId" = 'cccccccc-cccc-cccc-cccc-cccccccccc01'),
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa13'::uuid,
  'a second, un-reversed auto row on the same lot is still listed');

reset role;

-- ------------------------------------------------------------
-- Supersede: a later confirmed or manual row retires the estimate,
-- dismissed and an older confirmed do not
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa41',
   'cccccccc-cccc-cccc-cccc-cccccccccc04', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '3 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa42',
   'cccccccc-cccc-cccc-cccc-cccccccccc04', 'dadadada-dada-dada-dada-dadadadada01',
   0, 'confirmed', now() - interval '1 hour'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa51',
   'cccccccc-cccc-cccc-cccc-cccccccccc05', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '3 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa52',
   'cccccccc-cccc-cccc-cccc-cccccccccc05', 'dadadada-dada-dada-dada-dadadadada01',
   -2, 'manual', now() - interval '1 hour'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa61',
   'cccccccc-cccc-cccc-cccc-cccccccccc06', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '3 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa62',
   'cccccccc-cccc-cccc-cccc-cccccccccc06', 'dadadada-dada-dada-dada-dadadadada01',
   0, 'dismissed', now() - interval '1 hour'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa71',
   'cccccccc-cccc-cccc-cccc-cccccccccc07', 'dadadada-dada-dada-dada-dadadadada01',
   0, 'confirmed', now() - interval '3 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa72',
   'cccccccc-cccc-cccc-cccc-cccccccccc07', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '1 hour');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa41'),
  0,
  'a later confirmed row on the lot supersedes the auto row');

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa51'),
  0,
  'a later manual row on the lot supersedes the auto row');

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa61'),
  1,
  'a later dismissed row does not supersede the auto row');

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa72'),
  1,
  'a confirmed row older than the auto row does not supersede it');

reset role;

-- ------------------------------------------------------------
-- Stock columns: the product's total across the caller's lots, and its
-- threshold. The client turns the pair into Out / Low / Ok.
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa81',
   'cccccccc-cccc-cccc-cccc-cccccccccc30', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '2 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa82',
   'cccccccc-cccc-cccc-cccc-cccccccccc40', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '2 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa83',
   'cccccccc-cccc-cccc-cccc-cccccccccc50', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '2 hours');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select "productQuantity" || '|' || coalesce("lowStockThreshold"::text, 'null')
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa81'),
  '2|null',
  'a product with no stock settings has a null threshold');

select is(
  (select "productQuantity" || '|' || coalesce("lowStockThreshold"::text, 'null')
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa82'),
  '4|3',
  'productQuantity sums the product across the caller''s lots');

select is(
  (select "productQuantity" || '|' || coalesce("lowStockThreshold"::text, 'null')
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa83'),
  '0|3',
  'a product the job drove to zero reports productQuantity 0');

reset role;

-- ------------------------------------------------------------
-- Newest first
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa91',
   'cccccccc-cccc-cccc-cccc-cccccccccc08', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '3 hours'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa92',
   'cccccccc-cccc-cccc-cccc-cccccccccc08', 'dadadada-dada-dada-dada-dadadadada01',
   -1, 'auto', now() - interval '1 hour');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada01","role":"authenticated"}';

select is(
  (select array_agg("adjustmentId") from public.inventory_adjustments_detail
   where "inventoryItemId" = 'cccccccc-cccc-cccc-cccc-cccccccccc08'),
  array['aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa92',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa91']::uuid[],
  'rows come back newest first');

reset role;

-- ------------------------------------------------------------
-- RLS scopes the view to the caller
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa99',
   'cccccccc-cccc-cccc-cccc-cccccccccc90', 'dadadada-dada-dada-dada-dadadadada02',
   -1, 'auto', now() - interval '1 hour');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dadadada-dada-dada-dada-dadadadada02","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "inventoryItemId" = 'cccccccc-cccc-cccc-cccc-cccccccccc01'),
  0,
  'another user''s adjustments are not visible through the view');

select is(
  (select count(*)::int from public.inventory_adjustments_detail
   where "adjustmentId" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa99'),
  1,
  'the caller''s own auto row is visible');

reset role;

select * from finish();
rollback;
