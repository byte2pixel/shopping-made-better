-- ============================================================
-- Dummy account + mock inventory  (REMOVABLE)
-- ============================================================
-- Everything in this file is throwaway local test data. To remove it
-- entirely: delete this file and remove "./dummy_account.sql" from the
-- [db.seed] sql_paths list in config.toml, then `supabase db reset`.
-- It is loaded after seed.sql (see sql_paths order) so its inserts can
-- reference the seeded stores/products.
--
-- This file is self-contained and idempotent:
--   * (Re)creates a confirmed demo auth user you can sign in with.
--   * Ensures the profile row exists.
--   * Replaces the demo user's inventory with a fresh mock pantry.
--   * Replaces the demo user's shopping trips (summarized by the
--     shopping_trip_summaries view from migration 20260708015853).
--
-- Sign-in credentials (email confirmations are disabled in config.toml,
-- so this works immediately from the app's sign-in screen):
--     email:    demo@shoppingmadebetter.test
--     password: password123
-- ------------------------------------------------------------

-- Fixed UUID so the account + its data are easy to find/remove.
--   demo user id: 11111111-1111-1111-1111-111111111111

-- 1) Confirmed auth user. Direct inserts into auth.* are unusual, but
--    it's the only way to create a user without going through the app.
insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data,
  -- GoTrue scans these token columns as non-null strings when finding a
  -- user; leaving them NULL makes sign-in fail with a 500 ("converting NULL
  -- to string is unsupported"). They must be '' rather than NULL.
  confirmation_token, recovery_token,
  email_change, email_change_token_new, email_change_token_current,
  phone_change, phone_change_token, reauthentication_token
)
values (
  '00000000-0000-0000-0000-000000000000',
  '11111111-1111-1111-1111-111111111111',
  'authenticated', 'authenticated',
  'demo@smb.test',
  crypt('password123', gen_salt('bf')),           -- pgcrypto is available on Supabase
  now(), now(), now(),
  '{"provider":"email","providers":["email"]}',
  '{"display_name":"Demo Shopper"}',              -- so the profile trigger has a name
  '', '',
  '', '', '',
  '', '', ''
)
on conflict (id) do nothing;

insert into auth.identities (
  id, user_id, provider_id, identity_data, provider,
  last_sign_in_at, created_at, updated_at
)
values (
  gen_random_uuid(),
  '11111111-1111-1111-1111-111111111111',
  '11111111-1111-1111-1111-111111111111',
  '{"sub":"11111111-1111-1111-1111-111111111111","email":"demo@smb.test"}',
  'email', now(), now(), now()
)
on conflict do nothing;

-- 2) Ensure the profile row exists (the handle_new_user() trigger may
--    already have created it from the metadata above).
insert into public.profiles (id, display_name)
values ('11111111-1111-1111-1111-111111111111', 'Demo Shopper')
on conflict (id) do nothing;

-- 3) Mock pantry. Clear first so re-running this file is idempotent, then
--    insert. Products are matched by their stable source_product_id, so
--    this survives `db reset` even though product rows are re-created.
delete from public.inventory_items
where user_id = '11111111-1111-1111-1111-111111111111';

insert into public.inventory_items (
  user_id, product_id, quantity, unit, location, purchased_at, expires_at
)
select
  '11111111-1111-1111-1111-111111111111',
  p.id,
  v.quantity, v.unit, v.location, v.purchased_at, v.expires_at
from (values
  -- source_product_id, quantity, unit, location, purchased_at, expires_at
  ('20962518_EA',    1, 'carton', 'fridge',  current_date - 2, current_date + 5),   -- Milk, 2%
  ('21397475_EA',    1, 'tub',    'fridge',  current_date - 5, current_date + 10),  -- Cream Cheese Spread
  ('20324557_EA',    1, 'tub',    'fridge',  current_date - 3, current_date + 7),   -- Cottage Cheese
  ('20162840001_EA', 1, 'bunch',  'fridge',  current_date - 1, current_date + 3),   -- Spinach, Bunched
  ('20143381001_KG', 6, 'ea',     'fridge',  current_date - 1, current_date + 6),   -- Roma Tomatoes
  ('20091825001_EA', 1, 'bunch',  'fridge',  current_date - 1, current_date + 4),   -- Cilantro
  ('20179038001_KG', 1, 'ea',     'fridge',  current_date - 4, current_date + 21),  -- Ginger
  ('21191828_EA',    2, 'bag',    'freezer', current_date - 7, current_date + 120), -- Chicken Strips
  ('21470667_EA',    1, 'bag',    'freezer', current_date - 7, current_date + 90),  -- Gnocchi
  ('20788443_EA',    1, 'loaf',   'pantry',  current_date - 1, current_date + 4),   -- Bread, French
  ('20811362001_EA', 1, 'bag',    'pantry',  current_date - 3, current_date + 14),  -- Red Onions, 3 lb
  ('21219491_EA',    1, 'jar',    'pantry',  current_date - 10, current_date + 200),-- Peanut Butter Chocolatey
  ('21535597_EA',    3, 'pouch',  'pantry',  current_date - 10, current_date + 300),-- Jasmine Rice pouch
  ('21125083_EA',    2, 'jar',    'pantry',  current_date - 10, current_date + 250),-- Mac & Cheese Sauce
  ('21169553_EA',    1, 'loaf',   'pantry',  current_date - 2, current_date + 6)    -- 100% Whole Grain Bread
) as v(source_product_id, quantity, unit, location, purchased_at, expires_at)
join public.products p on p.source_product_id = v.source_product_id;

-- 4) Demo shopping trips (moved here from migration 20260708015853). These
--    SELECT from stores / store_product_pricing, which are only populated by
--    seed.sql -- so they must run in the seed phase, not the migration phase.
--    Clear first for idempotent re-runs (list items cascade with the lists).
delete from public.shopping_lists
where user_id = '11111111-1111-1111-1111-111111111111';

-- One shopping list ("trip") per store, for two stores.
insert into public.shopping_lists (user_id, store_id, name)
select '11111111-1111-1111-1111-111111111111', s.id, s.name || ' Weekly'
from public.stores s
where s.name in ('ALDI', 'Publix');

-- Put 5 real, currently-priced products on each list, so totals are non-zero.
-- LATERAL picks 5 products that actually have current pricing AT THAT STORE.
insert into public.shopping_list_items (shopping_list_id, product_id, quantity)
select sl.id, picked.product_id, 1
from public.shopping_lists sl
join lateral (
  select spp.product_id
  from public.store_product_pricing spp
  where spp.store_id = sl.store_id
    and spp.is_current = true
  order by spp.product_id
  limit 5
) picked on true
where sl.user_id = '11111111-1111-1111-1111-111111111111';