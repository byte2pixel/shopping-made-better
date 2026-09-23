-- ============================================================
-- pgTAP tests: household digest (SCRUM-291)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back. o and m share
-- a household; x is outside it. The view is read as m, o, postgres and x,
-- then the RPCs stamp actor_id as m and leave it NULL as postgres.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(13);

-- ------------------------------------------------------------
-- Fixtures. Clear the tables the view reads so seed rows cannot shift
-- counts. GoTrue token columns must be '' rather than NULL.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;
delete from public.user_product_stock_settings;
delete from public.user_product_consumption;

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
   'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101',
   'authenticated', 'authenticated', 'digest-owner@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Digest Owner"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102',
   'authenticated', 'authenticated', 'digest-member@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Digest Member"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d103',
   'authenticated', 'authenticated', 'digest-outsider@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Digest Outsider"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101', 'Digest Owner'),
  ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102', 'Digest Member'),
  ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d103', 'Digest Outsider')
on conflict (id) do nothing;

insert into public.households (id, name, invite_code)
values ('d2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d201', 'Digest House', 'DIGEST01');

update public.profiles
   set household_id = 'd2d2d2d2-d2d2-d2d2-d2d2-d2d2d2d2d201',
       is_household_head = (id = 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101')
 where id in ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101',
              'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102');

-- shelf_life_days null keeps the expiry and location triggers inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('d3d3d3d3-d3d3-d3d3-d3d3-d3d3d3d3d301', 'TEST-HDIGEST-01', 9992901,
   'Household Oats', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- d401 is o's lot (2 left after the auto row below), d402 is m's.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d401', 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101',
   'd3d3d3d3-d3d3-d3d3-d3d3-d3d3d3d3d301', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d402', 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102',
   'd3d3d3d3-d3d3-d3d3-d3d3-d3d3d3d3d301', 1, 'ea', 'pantry',
   current_date - 5, null, null, 0);

-- o's threshold is 1, m's is 3, so the view shows whose it read.
insert into public.user_product_stock_settings (user_id, product_id, low_stock_threshold)
values
  ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101', 'd3d3d3d3-d3d3-d3d3-d3d3-d3d3d3d3d301', 1),
  ('d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102', 'd3d3d3d3-d3d3-d3d3-d3d3-d3d3d3d3d301', 3);

insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('d5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501',
   'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d401', 'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101',
   -1, 'auto', now() - interval '1 hour');

-- ------------------------------------------------------------
-- Shape
-- ------------------------------------------------------------
select has_column('public', 'inventory_adjustments', 'actor_id',
  'inventory_adjustments has actor_id');

-- ------------------------------------------------------------
-- The view as a housemate: the owner's row with the viewer's threshold,
-- the household total and the owner's name
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102","role":"authenticated"}';

select is(
  (select "lotOwner" || '|' || "isOwn"
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  'Digest Owner|false',
  'a housemate sees the owner''s row with their name and isOwn false');

select is(
  (select "lowStockThreshold" from public.inventory_adjustments_detail
   where "adjustmentId" = 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  3,
  'the threshold is the viewer''s, not the lot owner''s');

select is(
  (select "productQuantity" from public.inventory_adjustments_detail
   where "adjustmentId" = 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  3,
  'productQuantity sums the household''s lots');

reset role;

-- ------------------------------------------------------------
-- The view as the owner
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101","role":"authenticated"}';

select is(
  (select "isOwn" || '|' || "lowStockThreshold"
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  'true|1',
  'the owner''s own row is isOwn with their own threshold');

reset role;
set local request.jwt.claims = '';

-- ------------------------------------------------------------
-- The view with no JWT: the owner's threshold and lots, no name, as
-- before this migration (the seed reads it this way)
-- ------------------------------------------------------------
select is(
  (select "lowStockThreshold" || '|' || "productQuantity" || '|'
          || coalesce("lotOwner", 'null') || '|' || "isOwn"
   from public.inventory_adjustments_detail
   where "adjustmentId" = 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  '1|2|null|false',
  'with no JWT the row falls back to the owner''s threshold and lots');

-- ------------------------------------------------------------
-- The view as an outsider
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d103","role":"authenticated"}';

select is(
  (select count(*)::int from public.inventory_adjustments_detail),
  0,
  'an outsider sees nothing');

reset role;

-- ------------------------------------------------------------
-- The RPCs as a housemate stamp actor_id; user_id stays the owner
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102","role":"authenticated"}';

select lives_ok(
  $$ select * from public.undo_inventory_adjustment(
       'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501') $$,
  'a housemate can undo the owner''s auto row');

select lives_ok(
  $$ select * from public.apply_inventory_adjustment(
       'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d401', -1, 'confirmed') $$,
  'a housemate can confirm the owner''s lot');

reset role;

select is(
  (select a.user_id || '|' || a.actor_id
   from public.inventory_adjustments a
   where a.reverses = 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101|d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102',
  'the undo row keeps the owner as user_id and records the housemate as actor');

select is(
  (select a.user_id || '|' || a.actor_id
   from public.inventory_adjustments a
   where a.inventory_item_id = 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d401'
     and a.reason = 'confirmed'),
  'd1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d101|d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d102',
  'the confirmed row keeps the owner as user_id and records the housemate as actor');

-- ------------------------------------------------------------
-- The RPC as postgres, the way the cron and the seed run it
-- ------------------------------------------------------------
set local request.jwt.claims = '';

select lives_ok(
  $$ select * from public.apply_inventory_adjustment(
       'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d401', -1, 'auto') $$,
  'postgres can still write an auto row');

select is(
  (select a.actor_id::text
   from public.inventory_adjustments a
   where a.inventory_item_id = 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d401'
     and a.reason = 'auto'
     and a.id <> 'd5d5d5d5-d5d5-d5d5-d5d5-d5d5d5d5d501'),
  null,
  'a row written with no JWT has a NULL actor');

select * from finish();
rollback;
