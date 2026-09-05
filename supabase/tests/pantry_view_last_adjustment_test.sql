-- ============================================================
-- pgTAP tests: pantry_items_by_expire latest-adjustment columns
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the
-- fixtures and table clears below never persist. now() is frozen per
-- transaction; audit rows get explicit created_at values so the
-- latest-row-per-lot ordering is deterministic.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(9);

-- ------------------------------------------------------------
-- Fixtures. Clear the tables the view reads so seeded data cannot
-- shift the counts.
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
   'dddddddd-dddd-dddd-dddd-dddddddddd01',
   'authenticated', 'authenticated', 'pantry-view-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Pantry View 1"}',
   '', '', '', '', '', '', '', ''),
  ('00000000-0000-0000-0000-000000000000',
   'dddddddd-dddd-dddd-dddd-dddddddddd02',
   'authenticated', 'authenticated', 'pantry-view-2@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Pantry View 2"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values
  ('dddddddd-dddd-dddd-dddd-dddddddddd01', 'Pantry View 1'),
  ('dddddddd-dddd-dddd-dddd-dddddddddd02', 'Pantry View 2')
on conflict (id) do nothing;

-- One product; shelf_life_days null keeps the expiry/location triggers inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'TEST-PVIEW-01', 9992101,
   'Pantry View Item', '1 ea', 'ea', '', 'SOLD_BY_EACH', 'ea', null, 'unclassified');

-- Lot cd01 belongs to user 1, cd02 to user 2.
insert into public.inventory_items
  (id, user_id, product_id, quantity, unit, location,
   purchased_at, expires_at, last_auto_adjusted_at, pending_fraction)
values
  ('cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01', 'dddddddd-dddd-dddd-dddd-dddddddddd01',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 3, 'ea', 'pantry',
   current_date - 10, null, null, 0),
  ('cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd02', 'dddddddd-dddd-dddd-dddd-dddddddddd02',
   'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 2, 'ea', 'pantry',
   current_date - 10, null, null, 0);

-- ------------------------------------------------------------
-- View shape
-- ------------------------------------------------------------
select columns_are('public', 'pantry_items_by_expire',
  array['id', 'productId', 'name', 'brand', 'description', 'size', 'quantity',
        'imageUrl', 'expiryDate', 'location', 'lowStockThreshold',
        'lastAutoAdjustedAtEpoch', 'estimateSource',
        'lastAdjustmentReason', 'lastAdjustedAtEpoch', 'lastAdjustmentId'],
  'view keeps every existing column and adds lastAdjustmentId');

select ok(
  (select c.reloptions @> array['security_invoker=true']
   from pg_class c join pg_namespace n on n.oid = c.relnamespace
   where n.nspname = 'public' and c.relname = 'pantry_items_by_expire'),
  'view is security invoker');

-- ------------------------------------------------------------
-- No audit rows → null reason and epoch (read as the lot's owner)
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dddddddd-dddd-dddd-dddd-dddddddddd01","role":"authenticated"}';

select ok(
  (select "lastAdjustmentReason" is null and "lastAdjustedAtEpoch" is null
      and "lastAdjustmentId" is null
   from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01'),
  'lot with no audit rows has null reason, epoch and id');

reset role;

-- ------------------------------------------------------------
-- Latest audit row wins: an auto row marks the lot, a later
-- confirmed row supersedes it.
-- ------------------------------------------------------------
insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('adadadad-adad-adad-adad-adadadadad11',
   'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01', 'dddddddd-dddd-dddd-dddd-dddddddddd01',
   -1, 'auto', now() - interval '1 hour');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dddddddd-dddd-dddd-dddd-dddddddddd01","role":"authenticated"}';

select is(
  (select "lastAdjustmentReason" || '|'
          || ("lastAdjustedAtEpoch"
              = extract(epoch from now() - interval '1 hour')::bigint)::text
   from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01'),
  'auto|true',
  'auto row surfaces as lastAdjustmentReason with its epoch');

select is(
  (select "lastAdjustmentId"
   from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01'),
  'adadadad-adad-adad-adad-adadadadad11'::uuid,
  'auto row surfaces as lastAdjustmentId');

reset role;

insert into public.inventory_adjustments
  (id, inventory_item_id, user_id, delta, reason, created_at)
values
  ('adadadad-adad-adad-adad-adadadadad12',
   'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01', 'dddddddd-dddd-dddd-dddd-dddddddddd01',
   0, 'confirmed', now());

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"dddddddd-dddd-dddd-dddd-dddddddddd01","role":"authenticated"}';

select is(
  (select "lastAdjustmentReason"
   from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01'),
  'confirmed',
  'later confirmed row supersedes the auto row');

select is(
  (select "lastAdjustmentId"
   from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01'),
  'adadadad-adad-adad-adad-adadadadad12'::uuid,
  'lastAdjustmentId follows the newest row');

-- ------------------------------------------------------------
-- RLS: user 2 sees only their own lot
-- ------------------------------------------------------------
set local request.jwt.claims =
  '{"sub":"dddddddd-dddd-dddd-dddd-dddddddddd02","role":"authenticated"}';

select is(
  (select count(*)::int from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01'),
  0,
  'another user''s lot is not visible through the view');

select ok(
  (select "lastAdjustmentReason" is null
   from public.pantry_items_by_expire
   where id = 'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd02'),
  'own lot is visible with no adjustment history');

reset role;

select * from finish();
rollback;
