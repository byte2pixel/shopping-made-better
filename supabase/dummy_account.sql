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
--   * Replaces the demo user's purchase history with two completed trips.
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

-- expires_at is intentionally NOT set here: the trg_inventory_set_expiry trigger
-- (migration 20260723120000) fills it as purchased_at + products.shelf_life_days.
-- We seed only purchased_at, as a randomized slice of each product's shelf life
-- BEFORE today, so expiry always lands fresh relative to *now* on every
-- `db reset`. used_lo/used_hi bound how much of the shelf life has already
-- elapsed at seed time:  used ~1 -> expiring now / just expired,  used ~0 -> just
-- bought. The bands are hand-tuned to give a realistic spread the pantry
-- dashboard can show off (a few expired, several expiring soon, the rest fresh).
insert into public.inventory_items (
  user_id, product_id, quantity, unit, location, purchased_at
)
select
  '11111111-1111-1111-1111-111111111111',
  p.id,
  v.quantity, v.unit, v.location,
  current_date - round(
    coalesce(p.shelf_life_days, 30)
    * (v.used_lo + random() * (v.used_hi - v.used_lo))
  )::int
from (values
  -- source_product_id, quantity, unit, location, used_lo, used_hi  (shelf life, expiry band)
  ('20962518_EA',    1, 'carton', 'fridge',  1.00, 1.20),  -- Milk, 2%            (10d)  expired / at expiry
  ('20091825001_EA', 1, 'bunch',  'fridge',  0.85, 1.05),  -- Cilantro            (7d)   just expired -> ~1d left
  ('20162840001_EA', 1, 'bunch',  'fridge',  0.80, 1.00),  -- Spinach, Bunched    (7d)   expiring very soon
  ('20788443_EA',    1, 'loaf',   'pantry',  1.10, 1.35),  -- Bread, French       (7d)   expired
  ('21169553_EA',    1, 'loaf',   'pantry',  0.70, 0.95),  -- Whole Grain Bread   (7d)   expiring soon
  ('21397475_EA',    1, 'tub',    'fridge',  0.60, 0.85),  -- Cream Cheese Spread (10d)  expiring soon
  ('20143381001_KG', 6, 'ea',     'fridge',  0.55, 0.75),  -- Roma Tomatoes       (14d)  ~1 week left
  ('20324557_EA',    1, 'tub',    'fridge',  0.55, 0.75),  -- Cottage Cheese      (30d)  a week or two left
  ('20811362001_EA', 1, 'bag',    'pantry',  0.35, 0.55),  -- Red Onions, 3 lb    (30d)  a couple weeks left
  ('20179038001_KG', 1, 'ea',     'fridge',  0.30, 0.50),  -- Ginger              (30d)  a couple weeks left
  ('21125083_EA',    2, 'jar',    'pantry',  0.10, 0.35),  -- Mac & Cheese Sauce  (365d) fresh
  ('21219491_EA',    1, 'jar',    'pantry',  0.10, 0.30),  -- Peanut Butter       (365d) fresh
  ('21535597_EA',    3, 'pouch',  'pantry',  0.05, 0.25),  -- Jasmine Rice pouch  (365d) fresh
  ('21469394_EA',    2, 'bag',    'freezer', 0.10, 0.30),  -- Jerk Chicken Wings  (180d) fresh (frozen)
  ('21496426_EA',    1, 'bag',    'freezer', 0.05, 0.25)   -- Frozen Red Raspberries (180d) fresh (frozen)
) as v(source_product_id, quantity, unit, location, used_lo, used_hi)
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

-- 5) Demo purchase history: two completed trips, so the History tab has
--    something to show on a fresh reset.
--    Clear first for idempotent re-runs (items cascade with the header row).
delete from public.purchase_history
where user_id = '11111111-1111-1111-1111-111111111111';

-- One trip per store, 4 currently-priced products each, dated relative to now()
-- so they always read as recent and land newest-first.
with trip_spec as (
  select * from (values
    -- store,     days ago, product offset, qty per line
    ('ALDI',      3,        0,              2),
    ('Publix',    10,       4,              1)
  ) as t(store_name, days_ago, pick_offset, quantity)
),
picked as (
  select
    ts.days_ago,
    ts.quantity,
    s.id  as store_id,
    pick.product_id,
    pick.price
  from trip_spec ts
  join public.stores s on s.name = ts.store_name
  join lateral (
    select spp.product_id, spp.price
    from public.store_product_pricing spp
    where spp.store_id = s.id
      and spp.is_current = true
    order by spp.product_id
    offset ts.pick_offset
    limit 4
  ) pick on true
),
trip as (
  insert into public.purchase_history (user_id, store_id, purchased_at, total_amount)
  select
    '11111111-1111-1111-1111-111111111111',
    p.store_id,
    now() - make_interval(days => p.days_ago),
    sum(p.quantity * p.price)
  from picked p
  group by p.store_id, p.days_ago
  returning id, store_id
)
insert into public.purchase_history_items (purchase_id, product_id, quantity, price_paid)
select t.id, p.product_id, p.quantity, p.price
from trip t
join picked p on p.store_id = t.store_id;
