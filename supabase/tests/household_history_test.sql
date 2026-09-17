-- ============================================================
-- pgTAP tests: household branch on purchase history policies and views (SCRUM-288)
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back. o and m
-- share a household; x is outside it and holds a trip of their own.
-- Spend assertions group by "isOwn" only: a 20-day-old trip may or may
-- not fall in the current month.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(29);

-- ------------------------------------------------------------
-- Fixtures. Clear the tables under test so seed rows cannot shift
-- counts. GoTrue token columns must be '' rather than NULL.
-- ------------------------------------------------------------
delete from public.user_product_consumption;
delete from public.purchase_history;

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
   'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01',
   'authenticated', 'authenticated', 'history-owner@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"History Owner"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02',
   'authenticated', 'authenticated', 'history-member@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"History Member"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb03',
   'authenticated', 'authenticated', 'history-outsider@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"History Outsider"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01', 'History Owner'),
  ('fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02', 'History Member'),
  ('fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb03', 'History Outsider')
on conflict (id) do nothing;

insert into public.households (id, name, invite_code)
values ('d0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d004', 'History House', 'HISTRY01');

update public.profiles
   set household_id = 'd0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d004',
       is_household_head = (id = 'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01')
 where id in ('fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01',
              'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02');

insert into public.stores (id, name, address, city, state, postal_code)
values ('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', 'History Mart',
        '2 Test St', 'Orlando', 'FL', '32801');

-- shelf_life_days 30 so the estimator writes a row for every pair.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e201', 'TEST-HHIST-01', 9992501,
   'History Beans', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 30, 'dry_goods'),
  ('e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e202', 'TEST-HHIST-02', 9992502,
   'History Rice',  '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', 30, 'dry_goods');

insert into public.store_product_pricing (store_id, product_id, price, display_price)
values
  ('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e201', 1.00, '$1.00'),
  ('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e202', 2.00, '$2.00');

-- T1 and T2 belong to o, 20 days apart (the history branch for Beans);
-- T3 to m, T4 to x. T3 sits between T1 and T2 in time.
insert into public.purchase_history (id, user_id, store_id, purchased_at, total_amount)
values
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201', 'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01',
   'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', now() - interval '20 days', 10.00),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c202', 'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01',
   'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', now(), 12.50),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c203', 'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02',
   'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', now() - interval '10 days', 5.00),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c204', 'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb03',
   'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', now() - interval '5 days', 7.00);

-- Beans on T1 and T2, Rice on T2 and T3, Beans on T4.
insert into public.purchase_history_items (purchase_id, product_id, quantity, price_paid)
values
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e201', 3, 1.00),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c202', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e201', 3, 1.00),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c202', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e202', 1, 2.00),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c203', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e202', 2, 2.50),
  ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c204', 'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e201', 1, 7.00);

-- ------------------------------------------------------------
-- Shape
-- ------------------------------------------------------------
select results_eq(
  $$ select tablename::text collate "default", policyname::text collate "default",
            cmd::text collate "default"
       from pg_policies
      where schemaname = 'public'
        and tablename in ('purchase_history', 'purchase_history_items')
      order by 1, 2 $$,
  $$ values ('purchase_history', 'Household members read trips', 'SELECT'),
            ('purchase_history', 'Users delete their own trips', 'DELETE'),
            ('purchase_history', 'Users insert their own trips', 'INSERT'),
            ('purchase_history', 'Users update their own trips', 'UPDATE'),
            ('purchase_history_items', 'Household members read trip items', 'SELECT'),
            ('purchase_history_items', 'Users delete items in their own trips', 'DELETE'),
            ('purchase_history_items', 'Users insert items in their own trips', 'INSERT'),
            ('purchase_history_items', 'Users update items in their own trips', 'UPDATE') $$,
  'trip reads take the household branch; trip writes stay with the buyer');

select ok(
  (select column_default like '%auth.uid()%'
   from information_schema.columns
   where table_schema = 'public' and table_name = 'purchase_history'
     and column_name = 'user_id'),
  'purchase_history.user_id defaults to auth.uid()');

select columns_are('public', 'purchase_history_summary',
  array['id', 'purchasedOn', 'purchasedAtEpoch', 'storeId', 'storeName', 'totalAmount',
        'lineTotal', 'itemCount', 'productSearch', 'purchasedBy', 'isOwn'],
  'purchase_history_summary carries purchasedBy and isOwn');

select columns_are('public', 'purchase_history_detail',
  array['id', 'purchaseId', 'purchasedOn', 'purchasedAtEpoch', 'storeId', 'storeName',
        'totalAmount', 'productId', 'productName', 'brand', 'size', 'imageUrl', 'quantity',
        'pricePaid', 'addedToInventory', 'purchasedBy', 'isOwn'],
  'purchase_history_detail carries storeId, purchasedBy and isOwn');

select columns_are('public', 'purchase_spend_by_month_store',
  array['monthStart', 'storeId', 'storeName', 'isOwn', 'total', 'tripCount'],
  'purchase_spend_by_month_store carries isOwn');

select columns_are('public', 'purchase_trip_cost_by_store',
  array['purchaseId', 'purchasedOn', 'storeId', 'storeName', 'isOwn', 'costHere',
        'paidForSameItems', 'itemsPriced', 'itemsTotal'],
  'purchase_trip_cost_by_store carries isOwn');

select ok(
  (select bool_and(c.reloptions @> array['security_invoker=true'])
   from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public'
     and c.relname in ('purchase_history_summary', 'purchase_history_detail',
                       'purchase_spend_by_month_store', 'purchase_trip_cost_by_store')),
  'the four history views are security invoker');

select ok(
  (select bool_and(has_table_privilege('authenticated', 'public.' || v, 'SELECT')
              and not has_table_privilege('anon', 'public.' || v, 'SELECT'))
   from unnest(array['purchase_history_summary', 'purchase_history_detail',
                     'purchase_spend_by_month_store', 'purchase_trip_cost_by_store']) as v),
  'the four history views are authenticated-only');

-- ------------------------------------------------------------
-- A member reads the household's trips and writes only their own
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02","role":"authenticated"}';

select is(
  (select count(*)::int from public.purchase_history_summary),
  3,
  'a member sees every household trip and no outsider trip');

select is(
  (select string_agg("isOwn"::text || ':' || coalesce("purchasedBy", '?'), ','
                     order by "purchasedAtEpoch")
   from public.purchase_history_summary),
  'false:History Owner,true:History Member,false:History Owner',
  'isOwn marks the caller''s trips and purchasedBy names the housemate');

select is(
  (select count(*)::text || '|' || bool_and("purchasedBy" = 'History Owner' and not "isOwn")::text
   from public.purchase_history_detail
   where "purchaseId" = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c202'),
  '2|true',
  'a housemate''s trip detail rows carry the buyer');

select ok(
  (select "productSearch" like '%History Beans%' and "productSearch" like '%History Rice%'
   from public.purchase_history_summary
   where id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c202'),
  'productSearch covers a housemate''s items');

select is(
  (select string_agg(r, ',' order by r)
   from (select "isOwn"::text || ':' || sum(total)::text || ':' || sum("tripCount")::text as r
         from public.purchase_spend_by_month_store
         group by "isOwn") g),
  'false:22.50:2,true:5.00:1',
  'spend splits into the caller''s rows and the household''s');

select ok(
  (select count(*) > 0 and bool_and(not "isOwn")
   from public.purchase_trip_cost_by_store
   where "purchaseId" = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201'),
  'a housemate''s trip costs carry isOwn = false');

update public.purchase_history set total_amount = 99
 where id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201';

select is(
  (select ph.total_amount from public.purchase_history ph
   where ph.id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201'),
  10.00::numeric(10,2),
  'a member''s edit of the owner''s trip changes nothing');

delete from public.purchase_history
 where id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201';

select is(
  (select count(*)::int from public.purchase_history
   where id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201'),
  1,
  'a member''s delete of the owner''s trip changes nothing');

select throws_ok(
  $$ insert into public.purchase_history (user_id, store_id, total_amount)
     values ('fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01',
             'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', 1.00) $$,
  '42501', null,
  'a member cannot insert a trip as the owner');

select throws_ok(
  $$ insert into public.purchase_history_items (purchase_id, product_id, quantity, price_paid)
     values ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201',
             'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e202', 1, 2.00) $$,
  '42501', null,
  'a member cannot add an item to the owner''s trip');

select lives_ok(
  $$ insert into public.purchase_history (id, store_id, total_amount)
     values ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c205',
             'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f201', 3.00) $$,
  'a member inserts a trip with user_id omitted');

select is(
  (select ph.user_id from public.purchase_history ph
   where ph.id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c205'),
  'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02'::uuid,
  'user_id defaults to the caller');

-- ------------------------------------------------------------
-- Estimator guard
-- ------------------------------------------------------------
select is(public.estimate_consumption_rates(), 1,
  'a signed-in caller computes only their own pairs');

-- reset role leaves the claim set, and the guard reads auth.uid(), not
-- the role.
reset role;
set local request.jwt.claims = '';

select is(
  (select count(*)::text || '|' || bool_and(user_id = 'fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02')::text
   from public.user_product_consumption),
  '1|true',
  'only the caller''s consumption row was written');

select is(public.estimate_consumption_rates(), 4,
  'postgres computes every user');

-- ------------------------------------------------------------
-- An outsider sees and touches only their own trip
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb03","role":"authenticated"}';

select is(
  (select string_agg("isOwn"::text || ':' || coalesce("purchasedBy", '?'), ','
                     order by "purchasedAtEpoch")
   from public.purchase_history_summary),
  'true:History Outsider',
  'an outsider sees only their own trip');

select is(
  (select string_agg(r, ',' order by r)
   from (select "isOwn"::text || ':' || sum(total)::text || ':' || sum("tripCount")::text as r
         from public.purchase_spend_by_month_store
         group by "isOwn") g),
  'true:7.00:1',
  'an outsider''s spend is their own');

select throws_ok(
  $$ insert into public.purchase_history_items (purchase_id, product_id, quantity, price_paid)
     values ('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c201',
             'e2e2e2e2-e2e2-e2e2-e2e2-e2e2e2e2e202', 1, 2.00) $$,
  '42501', null,
  'an outsider cannot add an item to a household trip');

-- ------------------------------------------------------------
-- Leaving takes your trips with you
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb02","role":"authenticated"}';

select lives_ok(
  $$ select public.leave_household() $$,
  'the member leaves the household');

select is(
  (select count(*)::int from public.purchase_history_summary),
  2,
  'after leaving, the member sees only their own trips');

set local request.jwt.claims =
  '{"sub":"fbfbfbfb-fbfb-fbfb-fbfb-fbfbfbfbfb01","role":"authenticated"}';

select is(
  (select count(*)::int from public.purchase_history_summary),
  2,
  'the owner no longer sees the departed member''s trips');

reset role;

select * from finish();
rollback;
