-- ============================================================
-- pgTAP tests: add_inventory_item()
-- ============================================================
-- Run with `npx supabase test db`. The transaction rolls back, so the
-- fixtures below never persist. Every assertion is keyed on the lot id the
-- function returned, so seeded rows cannot shift a result and the tables
-- need no clearing.
-- ------------------------------------------------------------
begin;
create extension if not exists pgtap with schema extensions;

select plan(12);

-- ------------------------------------------------------------
-- Fixtures. GoTrue token columns must be '' rather than NULL.
-- ------------------------------------------------------------
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
   'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a001',
   'authenticated', 'authenticated', 'add-inv-1@smb.test',
   crypt('password123', gen_salt('bf')), now(), now(), now(),
   '{"provider":"email","providers":["email"]}', '{"display_name":"Add Inv 1"}',
   '', '', '', '', '', '', '', '')
on conflict (id) do nothing;

insert into public.profiles (id, display_name)
values ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a001', 'Add Inv 1')
on conflict (id) do nothing;

-- Product A drives both triggers: milk_cream sends it to the fridge and a
-- 10-day shelf life gives it an expiry. Product B leaves both inert.
insert into public.products
  (id, source_product_id, article_number, title, package_sizing, uom,
   source_link, pricing_type, pricing_unit, shelf_life_days, shelf_life_category)
values
  ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b001', 'TEST-ADDINV-01', 9993301,
   'Add Inv Milk', '1 gal', 'ea', '', 'SOLD_BY_EACH', 'ea', 10, 'milk_cream'),
  ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b002', 'TEST-ADDINV-02', 9993302,
   'Add Inv Flour', '5 lb', 'lb', '', 'SOLD_BY_WEIGHT', 'lb', null, 'unclassified');

-- ------------------------------------------------------------
-- Function shape and privileges
-- ------------------------------------------------------------
select has_function('public', 'add_inventory_item',
  array['uuid', 'numeric', 'text']::name[],
  'add_inventory_item(uuid, numeric, text) exists');

select ok(
  (select not p.prosecdef
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'add_inventory_item'),
  'function is SECURITY INVOKER');

select ok(
  (select 'search_path=public' = any(p.proconfig)
   from pg_proc p join pg_namespace n on n.oid = p.pronamespace
   where n.nspname = 'public' and p.proname = 'add_inventory_item'),
  'function sets search_path = public');

select ok(
  has_function_privilege('authenticated',
    'public.add_inventory_item(uuid, numeric, text)', 'EXECUTE'),
  'authenticated can execute');

select ok(
  not has_function_privilege('anon',
    'public.add_inventory_item(uuid, numeric, text)', 'EXECUTE'),
  'anon cannot execute');

-- ------------------------------------------------------------
-- A signed-in user adds a lot to their own pantry
-- ------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a001","role":"authenticated"}';

select set_config('add_inv_test.lot_a',
  public.add_inventory_item('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b001', 2)::text, true);

select is(
  (select ii.user_id::text || '|' || ii.quantity::text || '|' || ii.unit || '|'
          || (ii.purchased_at = current_date)::text
   from public.inventory_items ii
   where ii.id = current_setting('add_inv_test.lot_a')::uuid),
  'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a001|2.000|ea|true',
  'the lot belongs to the caller, with the product unit and today as purchased_at');

select is(
  (select ii.location || '|' || ii.expires_at::text
   from public.inventory_items ii
   where ii.id = current_setting('add_inv_test.lot_a')::uuid),
  'fridge|' || (current_date + 10)::text,
  'no location asked for: the triggers derive fridge and a 10-day expiry');

select set_config('add_inv_test.lot_b',
  public.add_inventory_item('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b002', 1, 'freezer')::text, true);

select is(
  (select ii.location || '|' || coalesce(ii.expires_at::text, 'none')
   from public.inventory_items ii
   where ii.id = current_setting('add_inv_test.lot_b')::uuid),
  'freezer|none',
  'an explicit freezer is kept, and no shelf life means no expiry');

-- ------------------------------------------------------------
-- Rejections
-- ------------------------------------------------------------
select throws_ok(
  $$ select public.add_inventory_item('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b001', 0) $$,
  'P0001', 'quantity must be positive',
  'a quantity of zero is rejected');

select throws_ok(
  $$ select public.add_inventory_item('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b001', 1, 'garage') $$,
  'P0001', 'invalid location',
  'a location outside the CHECK constraint is rejected');

select throws_ok(
  $$ select public.add_inventory_item('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b099', 1) $$,
  'P0001', 'product not found',
  'an unknown product id is rejected');

-- ------------------------------------------------------------
-- No JWT, no insert. reset role leaves request.jwt.claims set, so clear it
-- before calling as postgres or auth.uid() still resolves.
-- ------------------------------------------------------------
reset role;
set local request.jwt.claims = '';

select throws_ok(
  $$ select public.add_inventory_item('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b001', 1) $$,
  'P0001', 'not signed in',
  'a call with no signed-in user is rejected');

select * from finish();
rollback;
