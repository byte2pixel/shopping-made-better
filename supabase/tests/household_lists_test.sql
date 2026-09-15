-- ============================================================
-- pgTAP tests: household branch on shopping list policies and view (SCRUM-286)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back. o and m
-- share a household; x is outside it and holds a list of their own.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(29);

-- ------------------------------------------------------------
-- Fixtures. Clear the tables under test so seed rows cannot shift
-- counts. GoTrue token columns must be '' rather than NULL.
-- ------------------------------------------------------------
delete from public.inventory_adjustments;
delete from public.inventory_items;
delete from public.purchase_history;
delete from public.shopping_lists;

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
   'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01',
   'authenticated', 'authenticated', 'list-owner@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"List Owner"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02',
   'authenticated', 'authenticated', 'list-member@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"List Member"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb03',
   'authenticated', 'authenticated', 'list-outsider@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"List Outsider"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01', 'List Owner'),
  ('dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02', 'List Member'),
  ('dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb03', 'List Outsider')
on conflict (id) do nothing;

insert into public.households (id, name, invite_code)
values ('d0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d003', 'List House', 'LISTS001');

update public.profiles
   set household_id = 'd0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d003',
       is_household_head = (id = 'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01')
 where id in ('dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01',
              'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02');

-- The view inner-joins stores, so the lists need one of their own.
insert into public.stores (id, name, address, city, state, postal_code)
values ('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'Test Mart',
        '1 Test St', 'Orlando', 'FL', '32801');

-- shelf_life_days null keeps the expiry/location triggers inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e101', 'TEST-HLISTS-01', 9992401,
   'Household Eggs', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified'),
  ('e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e102', 'TEST-HLISTS-02', 9992402,
   'Household Rice', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

insert into public.store_product_pricing (store_id, product_id, price, display_price)
values
  ('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e101', 2.00, '$2.00'),
  ('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e102', 3.00, '$3.00');

-- L1 belongs to o, L2 to m, L3 to x.
insert into public.shopping_lists (id, user_id, store_id, name)
values
  ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101', 'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01',
   'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'Owner Weekly'),
  ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a102', 'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02',
   'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'Member Weekly'),
  ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a103', 'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb03',
   'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'Outsider Weekly');

-- I1 and I2 on L1 (I1 already checked), I3 on L2, I4 on L3.
insert into public.shopping_list_items
  (id, shopping_list_id, product_id, quantity, is_checked, add_to_inventory)
values
  ('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b101', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101',
   'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e101', 2, true, true),
  ('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b102', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101',
   'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e102', 1, false, true),
  ('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b103', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a102',
   'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e101', 1, false, true),
  ('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b104', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a103',
   'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e102', 1, false, true);

-- ------------------------------------------------------------
-- Shape
-- ------------------------------------------------------------
select results_eq(
  $$ select tablename::text collate "default", policyname::text collate "default",
            cmd::text collate "default"
       from pg_policies
      where schemaname = 'public'
        and tablename in ('shopping_lists', 'shopping_list_items')
      order by 1, 2 $$,
  $$ values ('shopping_list_items', 'Household members add list items', 'INSERT'),
            ('shopping_list_items', 'Household members delete list items', 'DELETE'),
            ('shopping_list_items', 'Household members read list items', 'SELECT'),
            ('shopping_list_items', 'Household members update list items', 'UPDATE'),
            ('shopping_lists', 'Household members read lists', 'SELECT'),
            ('shopping_lists', 'Individuals can delete their own shopping lists', 'DELETE'),
            ('shopping_lists', 'Individuals can only insert to their own shopping list', 'INSERT'),
            ('shopping_lists', 'Individuals can update their own shopping lists', 'UPDATE') $$,
  'list reads take the household branch; list writes stay with the creator');

select columns_are('public', 'shopping_trip_summaries',
  array['shopping_list_id', 'list_name', 'sort_order', 'created_at', 'updated_at',
        'store_id', 'store_name', 'item_count', 'total_cost', 'created_by', 'is_own'],
  'shopping_trip_summaries carries created_by and is_own');

select ok(
  (select c.reloptions @> array['security_invoker=true']
   from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public' and c.relname = 'shopping_trip_summaries'),
  'shopping_trip_summaries is security invoker');

select ok(
  has_table_privilege('authenticated', 'public.shopping_trip_summaries', 'SELECT')
  and not has_table_privilege('anon', 'public.shopping_trip_summaries', 'SELECT'),
  'shopping_trip_summaries is authenticated-only');

select ok(
  (select c.reloptions @> array['security_invoker=true']
   from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public' and c.relname = 'shopping_list_item_named_view'),
  'shopping_list_item_named_view is security invoker');

-- ------------------------------------------------------------
-- A member reads and writes the household's lists
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02","role":"authenticated"}';

select is(
  (select count(*)::int from public.shopping_trip_summaries),
  2,
  'a member sees every household list and no outsider list');

select is(
  (select string_agg(is_own::text || ':' || coalesce(created_by, '?'), ',' order by list_name)
   from public.shopping_trip_summaries),
  'true:List Member,false:List Owner',
  'is_own marks the caller''s lists and created_by names the housemate');

select is(
  (select item_count::text || '|' || total_cost::numeric(10,2)::text
   from public.shopping_trip_summaries
   where shopping_list_id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101'),
  '2|7.00',
  'a housemate''s list counts and totals at its store');

select is(
  (select count(*)::int from public.shopping_list_item_named_view
   where shopping_id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101'),
  2,
  'a member reads a housemate''s list items');

select lives_ok(
  $$ insert into public.shopping_list_items (id, shopping_list_id, product_id, quantity)
     values ('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b105',
             'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101',
             'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e102', 1) $$,
  'a member adds an item to the owner''s list');

update public.shopping_list_items set is_checked = true
 where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b102';

select is(
  (select sli.is_checked from public.shopping_list_items sli
   where sli.id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b102'),
  true,
  'a member checks an item on the owner''s list');

select throws_ok(
  $$ update public.shopping_list_items
        set shopping_list_id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a103'
      where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b102' $$,
  '42501', null,
  'a member cannot move an item to a list outside the household');

delete from public.shopping_list_items
 where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b105';

select is(
  (select count(*)::int from public.shopping_list_items
   where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b105'),
  0,
  'a member deletes an item from the owner''s list');

update public.shopping_lists set name = 'Nope'
 where id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101';

select is(
  (select sl.name from public.shopping_lists sl
   where sl.id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101'),
  'Owner Weekly',
  'a member''s rename of the owner''s list changes nothing');

delete from public.shopping_lists
 where id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101';

select is(
  (select count(*)::int from public.shopping_lists
   where id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101'),
  1,
  'a member''s delete of the owner''s list changes nothing');

select throws_ok(
  $$ insert into public.shopping_lists (user_id, store_id, name)
     values ('dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01',
             'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'Forged') $$,
  '42501', null,
  'a member cannot insert a list as the owner');

select lives_ok(
  $$ insert into public.shopping_lists (id, store_id, name)
     values ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a104',
             'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f101', 'Member Second') $$,
  'a member inserts a list with user_id omitted');

select is(
  (select sl.user_id from public.shopping_lists sl
   where sl.id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a104'),
  'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02'::uuid,
  'user_id defaults to the caller');

-- ------------------------------------------------------------
-- A member completes the owner's list (I1 and I2 are both checked)
-- ------------------------------------------------------------
select matches(
  set_config('household_lists_test.trip',
             public.complete_shopping_trip('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101')::text,
             true),
  '^[0-9a-f-]{36}$',
  'a member completes the owner''s list');

reset role;

select is(
  (select ph.user_id from public.purchase_history ph
   where ph.id = current_setting('household_lists_test.trip')::uuid),
  'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02'::uuid,
  'the trip belongs to the member who completed the list');

select is(
  (select count(*)::int from public.shopping_list_items
   where shopping_list_id = 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a101'),
  0,
  'the checked items are cleared from the owner''s list');

select is(
  (select ii.user_id from public.inventory_items ii
   where ii.product_id = 'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e101'),
  'dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02'::uuid,
  'the pantry lot belongs to the member');

-- ------------------------------------------------------------
-- An outsider sees and touches only their own list
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb03","role":"authenticated"}';

select is(
  (select string_agg(is_own::text || ':' || coalesce(created_by, '?'), ',' order by list_name)
   from public.shopping_trip_summaries),
  'true:List Outsider',
  'an outsider sees only their own list');

update public.shopping_list_items set is_checked = true
 where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b103';

delete from public.shopping_list_items
 where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b103';

select throws_ok(
  $$ insert into public.shopping_list_items (shopping_list_id, product_id, quantity)
     values ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a102',
             'e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e101', 1) $$,
  '42501', null,
  'an outsider cannot add an item to a household list');

select throws_ok(
  $$ select public.complete_shopping_trip('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a102') $$,
  'P0001', 'Shopping list a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a102 not found or not accessible',
  'an outsider cannot complete a household list');

reset role;

select is(
  (select count(*)::int from public.shopping_list_items
   where id = 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b103' and not is_checked),
  1,
  'an outsider''s update and delete touch no household item');

-- ------------------------------------------------------------
-- Leaving takes your lists with you
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb02","role":"authenticated"}';

select lives_ok(
  $$ select public.leave_household() $$,
  'the member leaves the household');

select is(
  (select string_agg(list_name, ',' order by list_name)
   from public.shopping_trip_summaries),
  'Member Second,Member Weekly',
  'after leaving, the member sees only their own lists');

set local request.jwt.claims =
  '{"sub":"dbdbdbdb-dbdb-dbdb-dbdb-dbdbdbdbdb01","role":"authenticated"}';

select is(
  (select count(*)::int from public.shopping_trip_summaries),
  1,
  'the owner no longer sees the departed member''s lists');

reset role;

select * from finish();
rollback;
