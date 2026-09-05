-- ============================================================
-- pgTAP tests: apply_inventory_adjustment()
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the
-- fixtures and table clears below never persist. now() is frozen per
-- transaction, so a stamp equal to now() proves the call stamped the lot.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(30);

-- ------------------------------------------------------------
-- Fixtures. Clear the function's output so seeded data cannot shift
-- the counts.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;

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
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'authenticated', 'authenticated', 'apply-adj-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Apply Adj 1"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
   'authenticated', 'authenticated', 'apply-adj-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Apply Adj 2"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'Apply Adj 1'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'Apply Adj 2')
on conflict (id) do nothing;

-- One product; shelf_life_days null keeps the expiry/location triggers inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('ffffffff-ffff-ffff-ffff-ffffffffff01', 'TEST-APPLY-01', 9992001,
   'Apply Adj Item', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- Lots cc01..cc06 and cc08 belong to user 1, cc07 to user 2.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  -- confirmed -1: pending reset, stamped
  ('cccccccc-cccc-cccc-cccc-cccccccccc01', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 3, 'ea', 'pantry',
   current_date - 10, null, now() - interval '5 days', 0.6000),
  -- undo +1: pending kept
  ('cccccccc-cccc-cccc-cccc-cccccccccc02', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 2, 'ea', 'pantry',
   current_date - 10, null, now() - interval '5 days', 0.4000),
  -- confirmed -5: floors at zero
  ('cccccccc-cccc-cccc-cccc-cccccccccc03', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- confirmed 0: audit row only
  ('cccccccc-cccc-cccc-cccc-cccccccccc04', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 1, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- manual -1 twice: no once-per-day guard
  ('cccccccc-cccc-cccc-cccc-cccccccccc05', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 4, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- rejected calls: must stay untouched
  ('cccccccc-cccc-cccc-cccc-cccccccccc06', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 1, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- user 2's lot
  ('cccccccc-cccc-cccc-cccc-cccccccccc07', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  -- dismissed 0 on an empty lot: audit row only, pending kept, stamped
  ('cccccccc-cccc-cccc-cccc-cccccccccc08', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01',
   'ffffffff-ffff-ffff-ffff-ffffffffff01', 0, 'ea', 'pantry',
   current_date - 10, null, now() - interval '5 days', 0.2500);

-- ------------------------------------------------------------
-- Function shape
-- ------------------------------------------------------------
select has_function('public', 'apply_inventory_adjustment',
  array['uuid', 'numeric', 'text']::name[],
  'apply_inventory_adjustment(uuid, numeric, text) exists');

select ok(
  (select not p.prosecdef
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'apply_inventory_adjustment'),
  'function is SECURITY INVOKER');

select ok(
  (select 'search_path=public' = any(p.proconfig)
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'apply_inventory_adjustment'),
  'function sets search_path = public');

select ok(
  has_function_privilege('authenticated',
    'public.apply_inventory_adjustment(uuid, numeric, text)', 'EXECUTE'),
  'authenticated can execute');

-- ------------------------------------------------------------
-- confirmed -1: quantity, stamp, pending reset, audit
-- ------------------------------------------------------------
select ok(
  (select r.delta = -1 and r.new_quantity = 2
   from public.apply_inventory_adjustment(
     'cccccccc-cccc-cccc-cccc-cccccccccc01', -1, 'confirmed') r),
  'confirmed -1 on 3 returns delta -1 and new quantity 2');

select is(
  (select ii.quantity::text || '|' || ii.pending_fraction::text || '|'
          || (ii.last_auto_adjusted_at = now())::text
   from public.inventory_items ii
   where ii.id = 'cccccccc-cccc-cccc-cccc-cccccccccc01'),
  '2.000|0.0000|true',
  'confirmed: quantity 2, pending fraction reset, lot stamped');

select is(
  (select count(*)::int from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc01'),
  1,
  'confirmed: exactly one audit row');

select is(
  (select a.delta::text || '|' || a.reason from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc01'),
  '-1.000|confirmed',
  'confirmed: audit row records -1 under the reason given');

-- ------------------------------------------------------------
-- undo +1: restores, keeps pending fraction
-- ------------------------------------------------------------
select ok(
  (select r.new_quantity = 3
   from public.apply_inventory_adjustment(
     'cccccccc-cccc-cccc-cccc-cccccccccc02', 1, 'undo') r),
  'undo +1 on 2 returns new quantity 3');

select is(
  (select ii.quantity::text || '|' || ii.pending_fraction::text || '|'
          || (ii.last_auto_adjusted_at = now())::text
   from public.inventory_items ii
   where ii.id = 'cccccccc-cccc-cccc-cccc-cccccccccc02'),
  '3.000|0.4000|true',
  'undo: quantity 3, pending fraction untouched, lot stamped');

select is(
  (select a.delta::text || '|' || a.reason from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc02'),
  '1.000|undo',
  'undo: audit row records +1');

-- ------------------------------------------------------------
-- floor at zero
-- ------------------------------------------------------------
select ok(
  (select r.delta = -2 and r.new_quantity = 0
   from public.apply_inventory_adjustment(
     'cccccccc-cccc-cccc-cccc-cccccccccc03', -5, 'confirmed') r),
  'confirmed -5 on 2 clamps to the effective delta -2');

select is(
  (select ii.quantity::text from public.inventory_items ii
   where ii.id = 'cccccccc-cccc-cccc-cccc-cccccccccc03'),
  '0.000',
  'floored lot survives at zero');

select is(
  (select a.delta::text from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc03'),
  '-2.000',
  'audit records the effective delta, not the requested one');

-- ------------------------------------------------------------
-- zero effective delta still audits
-- ------------------------------------------------------------
select ok(
  (select r.delta = 0 and r.new_quantity = 1
   from public.apply_inventory_adjustment(
     'cccccccc-cccc-cccc-cccc-cccccccccc04', 0, 'confirmed') r),
  'confirmed 0 leaves the quantity at 1');

-- Checked in its own statement: a scalar subquery alongside the call
-- reads the pre-call snapshot and cannot see the row it inserted.
select ok(
  (select count(*) = 1 from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc04'
     and a.reason = 'confirmed' and a.delta = 0),
  'confirmed 0 still writes one zero-delta audit row');

-- ------------------------------------------------------------
-- dismissed 0: "not now" on the zero-stock gate audits without changing the lot
-- ------------------------------------------------------------
select ok(
  (select r.delta = 0 and r.new_quantity = 0
   from public.apply_inventory_adjustment(
     'cccccccc-cccc-cccc-cccc-cccccccccc08', 0, 'dismissed') r),
  'dismissed 0 on an empty lot returns delta 0 and quantity 0');

select is(
  (select ii.quantity::text || '|' || ii.pending_fraction::text || '|'
          || (ii.last_auto_adjusted_at = now())::text
   from public.inventory_items ii
   where ii.id = 'cccccccc-cccc-cccc-cccc-cccccccccc08'),
  '0.000|0.2500|true',
  'dismissed keeps quantity and pending fraction, stamps the lot');

select ok(
  (select count(*) = 1 from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc08'
     and a.reason = 'dismissed' and a.delta = 0),
  'dismissed 0 writes one zero-delta audit row');

-- ------------------------------------------------------------
-- No once-per-day guard: explicit deltas apply every time
-- ------------------------------------------------------------
select lives_ok(
  $$ select public.apply_inventory_adjustment(
       'cccccccc-cccc-cccc-cccc-cccccccccc05', -1, 'manual') $$,
  'manual -1: first call');

select lives_ok(
  $$ select public.apply_inventory_adjustment(
       'cccccccc-cccc-cccc-cccc-cccccccccc05', -1, 'manual') $$,
  'manual -1: immediate second call');

select ok(
  (select ii.quantity = 2 from public.inventory_items ii
   where ii.id = 'cccccccc-cccc-cccc-cccc-cccccccccc05')
  and (select count(*) = 2 from public.inventory_adjustments a
       where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc05'
         and a.reason = 'manual'),
  'two -1 calls remove 2 and leave 2 audit rows');

-- ------------------------------------------------------------
-- Rejected input
-- ------------------------------------------------------------
select throws_ok(
  $$ select public.apply_inventory_adjustment(
       'cccccccc-cccc-cccc-cccc-cccccccccc06', -1, 'bogus') $$,
  '23514', null,
  'unknown reason is rejected by the CHECK constraint');

select throws_ok(
  $$ select public.apply_inventory_adjustment(
       'cccccccc-cccc-cccc-cccc-cccccccccc06', null, 'confirmed') $$,
  'P0001', 'p_delta must not be null',
  'null delta is rejected');

select ok(
  (select ii.quantity = 1 and ii.last_auto_adjusted_at is null
   from public.inventory_items ii
   where ii.id = 'cccccccc-cccc-cccc-cccc-cccccccccc06')
  and (select count(*) = 0 from public.inventory_adjustments a
       where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc06'),
  'rejected calls leave the lot untouched and unaudited');

-- ------------------------------------------------------------
-- RLS: run as user 2
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02","role":"authenticated"}';

select throws_ok(
  $$ select public.apply_inventory_adjustment(
       'cccccccc-cccc-cccc-cccc-cccccccccc01', -1, 'confirmed') $$,
  'P0001',
  'Inventory item cccccccc-cccc-cccc-cccc-cccccccccc01 not found or not accessible',
  'another user''s lot is not accessible');

select ok(
  (select r.new_quantity = 1
   from public.apply_inventory_adjustment(
     'cccccccc-cccc-cccc-cccc-cccccccccc07', -1, 'confirmed') r),
  'a user can adjust their own lot under RLS');

reset role;

select is(
  (select a.user_id::text || '|' || a.reason from public.inventory_adjustments a
   where a.inventory_item_id = 'cccccccc-cccc-cccc-cccc-cccccccccc07'),
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02|confirmed',
  'audit row belongs to the lot owner');

-- ------------------------------------------------------------
-- Totals
-- ------------------------------------------------------------
select is(
  (select count(*)::int from public.inventory_adjustments),
  8,
  'one audit row per successful call');

select is(
  (select count(*)::int from public.inventory_items),
  8,
  'no lot was deleted');

select * from finish();
rollback;
