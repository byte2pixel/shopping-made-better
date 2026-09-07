-- ============================================================
-- pgTAP tests: household branch on pantry policies and views (SCRUM-261)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back. o and m
-- share a household; x is outside it and holds a lot of their own.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(26);

-- ------------------------------------------------------------
-- Fixtures. Clear the tables under test so seed rows cannot shift
-- counts. GoTrue token columns must be '' rather than NULL.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;
delete from public.user_product_stock_settings;

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
   'cacacaca-caca-caca-caca-cacacacaca01',
   'authenticated', 'authenticated', 'pantry-owner@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Pantry Owner"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'cacacaca-caca-caca-caca-cacacacaca02',
   'authenticated', 'authenticated', 'pantry-member@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Pantry Member"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'cacacaca-caca-caca-caca-cacacacaca03',
   'authenticated', 'authenticated', 'pantry-outsider@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Pantry Outsider"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('cacacaca-caca-caca-caca-cacacacaca01', 'Pantry Owner'),
  ('cacacaca-caca-caca-caca-cacacacaca02', 'Pantry Member'),
  ('cacacaca-caca-caca-caca-cacacacaca03', 'Pantry Outsider')
on conflict (id) do nothing;

insert into public.households (id, name, invite_code)
values ('d0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d001', 'Pantry House', 'PANTRY01');

update public.profiles
   set household_id = 'd0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d001',
       is_household_head = (id = 'cacacaca-caca-caca-caca-cacacacaca01')
 where id in ('cacacaca-caca-caca-caca-cacacacaca01',
              'cacacaca-caca-caca-caca-cacacacaca02');

-- shelf_life_days null keeps the expiry/location triggers inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('efefefef-efef-efef-efef-efefefefef01', 'TEST-HPANTRY-01', 9992301,
   'Household Milk', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('efefefef-efef-efef-efef-efefefefef02', 'TEST-HPANTRY-02', 9992302,
   'Household Bread', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- c101 and c103 belong to o, c102 to m, c104 to x.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101', 'cacacaca-caca-caca-caca-cacacacaca01',
   'efefefef-efef-efef-efef-efefefefef01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c102', 'cacacaca-caca-caca-caca-cacacacaca02',
   'efefefef-efef-efef-efef-efefefefef01', 1, 'ea', 'pantry',
   current_date - 5, null, null, 0),
  ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103', 'cacacaca-caca-caca-caca-cacacacaca01',
   'efefefef-efef-efef-efef-efefefefef02', 1, 'ea', 'pantry',
   current_date - 3, null, null, 0),
  ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c104', 'cacacaca-caca-caca-caca-cacacacaca03',
   'efefefef-efef-efef-efef-efefefefef01', 5, 'ea', 'pantry',
   current_date - 1, null, null, 0);

insert into public.user_product_stock_settings (user_id, product_id, low_stock_threshold)
values
  ('cacacaca-caca-caca-caca-cacacacaca01', 'efefefef-efef-efef-efef-efefefefef01', 1),
  ('cacacaca-caca-caca-caca-cacacacaca02', 'efefefef-efef-efef-efef-efefefefef01', 3);

insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('adadadad-adad-adad-adad-adadadadad61',
   'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101', 'cacacaca-caca-caca-caca-cacacacaca01',
   -1, 'auto', now() - interval '1 hour');

-- ------------------------------------------------------------
-- Shape
-- ------------------------------------------------------------
select results_eq(
  $$ select tablename::text collate "default", policyname::text collate "default",
            cmd::text collate "default"
       from pg_policies
      where schemaname = 'public'
        and tablename in ('inventory_items', 'inventory_adjustments')
      order by 1, 2 $$,
  $$ values ('inventory_adjustments', 'Household members manage adjustments', 'ALL'),
            ('inventory_items', 'Household members delete lots', 'DELETE'),
            ('inventory_items', 'Household members read lots', 'SELECT'),
            ('inventory_items', 'Household members update lots', 'UPDATE'),
            ('inventory_items', 'Users insert their own lots', 'INSERT') $$,
  'owner-only policies are replaced by the household branch');

select ok(
  (select column_default like '%auth.uid()%'
   from information_schema.columns
   where table_schema = 'public' and table_name = 'inventory_items'
     and column_name = 'user_id'),
  'inventory_items.user_id defaults to auth.uid()');

select ok(
  has_table_privilege('authenticated', 'public.pantry_items_by_expire', 'SELECT')
  and not has_table_privilege('anon', 'public.pantry_items_by_expire', 'SELECT')
  and has_table_privilege('authenticated', 'public.product_details', 'SELECT')
  and not has_table_privilege('anon', 'public.product_details', 'SELECT'),
  'both recreated views are authenticated-only');

select ok(
  (select c.reloptions @> array['security_invoker=true']
   from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public' and c.relname = 'product_details'),
  'product_details is security invoker');

-- ------------------------------------------------------------
-- A member reads the household
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"cacacaca-caca-caca-caca-cacacacaca02","role":"authenticated"}';

select is(
  (select count(*)::int from public.pantry_items_by_expire),
  3,
  'a member sees every household lot and no outsider lot');

select is(
  (select string_agg("isOwn"::text || ':' || coalesce("addedBy", '?'), ',' order by id)
   from public.pantry_items_by_expire),
  'false:Pantry Owner,true:Pantry Member,false:Pantry Owner',
  'isOwn marks the caller''s lots and addedBy names the housemate');

select is(
  (select "lowStockThreshold"::int from public.pantry_items_by_expire
   where id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101'),
  3,
  'a housemate''s lot shows the viewer''s own threshold');

select is(
  (select "lastAdjustmentId" from public.pantry_items_by_expire
   where id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101'),
  'adadadad-adad-adad-adad-adadadadad61'::uuid,
  'a housemate''s lot exposes its latest adjustment');

select is(
  (select string_agg(quantity::text || '|' || coalesce("lowStockThreshold"::text, 'null'), ',')
   from public.product_details
   where id = 'efefefef-efef-efef-efef-efefefefef01'),
  '3|3',
  'product_details sums both members'' lots in one row with the viewer''s threshold');

-- ------------------------------------------------------------
-- An outsider sees and touches only their own lot
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"cacacaca-caca-caca-caca-cacacacaca03","role":"authenticated"}';

select is(
  (select string_agg(id::text, ',') from public.pantry_items_by_expire),
  'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c104',
  'an outsider sees only their own lot');

select is(
  (select quantity from public.product_details
   where id = 'efefefef-efef-efef-efef-efefefefef01'),
  5,
  'product_details counts only the outsider''s own lot');

update public.inventory_items set location = 'fridge'
 where id in ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101',
              'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c102',
              'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103');

delete from public.inventory_items
 where id in ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101',
              'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c102',
              'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103');

select throws_ok(
  $$ select public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad61') $$,
  'P0001', 'Adjustment adadadad-adad-adad-adad-adadadadad61 not found or not accessible',
  'an outsider cannot undo a household adjustment');

select throws_ok(
  $$ select public.apply_inventory_adjustment(
       'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101', -1, 'manual') $$,
  'P0001', 'Inventory item c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101 not found or not accessible',
  'an outsider cannot adjust a household lot');

reset role;

select is(
  (select count(*)::int from public.inventory_items
   where id in ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101',
                'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c102',
                'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103')
     and location = 'fridge'),
  0,
  'an outsider''s update touches no household lot');

select is(
  (select count(*)::int from public.inventory_items
   where id in ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101',
                'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c102',
                'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103')),
  3,
  'an outsider''s delete removes no household lot');

-- ------------------------------------------------------------
-- A member writes the owner's lots
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"cacacaca-caca-caca-caca-cacacacaca02","role":"authenticated"}';

update public.inventory_items set location = 'freezer'
 where id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101';

select is(
  (select ii.location from public.inventory_items ii
   where ii.id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101'),
  'freezer',
  'a member can update the owner''s lot');

select throws_ok(
  $$ update public.inventory_items
        set user_id = 'cacacaca-caca-caca-caca-cacacacaca03'
      where id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101' $$,
  '42501', null,
  'a member cannot move a lot outside the household');

select throws_ok(
  $$ insert into public.inventory_items (user_id, product_id, quantity, unit)
     values ('cacacaca-caca-caca-caca-cacacacaca01',
             'efefefef-efef-efef-efef-efefefefef02', 1, 'ea') $$,
  '42501', null,
  'a member cannot insert a lot as the owner');

select lives_ok(
  $$ insert into public.inventory_items (id, product_id, quantity, unit)
     values ('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c105',
             'efefefef-efef-efef-efef-efefefefef02', 1, 'ea') $$,
  'a member inserts with user_id omitted');

select is(
  (select ii.user_id from public.inventory_items ii
   where ii.id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c105'),
  'cacacaca-caca-caca-caca-cacacacaca02'::uuid,
  'user_id defaults to the caller');

select ok(
  (select r.new_quantity = 1
   from public.apply_inventory_adjustment(
     'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101', -1, 'confirmed') r),
  'a member can confirm the owner''s lot');

select is(
  (select a.user_id from public.inventory_adjustments a
   where a.inventory_item_id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101'
     and a.reason = 'confirmed'),
  'cacacaca-caca-caca-caca-cacacacaca01'::uuid,
  'the audit row keeps the lot owner''s id');

select ok(
  (select r.new_quantity = 2
   from public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad61') r),
  'a member can undo the owner''s automatic adjustment');

select is(
  (select a.reverses from public.inventory_adjustments a
   where a.inventory_item_id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c101'
     and a.reason = 'undo'),
  'adadadad-adad-adad-adad-adadadadad61'::uuid,
  'the undo row points at the reversed row');

select throws_ok(
  $$ select public.undo_inventory_adjustment('adadadad-adad-adad-adad-adadadadad61') $$,
  'P0001', 'Adjustment adadadad-adad-adad-adad-adadadadad61 already undone',
  'a second undo is refused');

delete from public.inventory_items
 where id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103';

reset role;

select is(
  (select count(*)::int from public.inventory_items
   where id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c103'),
  0,
  'a member can delete the owner''s lot');

select * from finish();
rollback;
