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
--   * Replaces the demo user's purchase history with 12 completed trips
--     spread over the last ~6 months.
--
-- Sign-in credentials (email confirmations are disabled in config.toml,
-- so this works immediately from the app's sign-in screen):
--     email:    demo@smb.test
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

-- Opt the demo account into automatic pantry adjustment. The column default is
-- false (real users opt in from settings), so flipping it here is what makes the
-- feature exercisable straight after a `db reset`.
update public.profiles
   set auto_adjust_enabled = true
 where id = '11111111-1111-1111-1111-111111111111';

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
  ('21496426_EA',    2, 'bag',    'freezer', 0.05, 0.25)   -- Frozen Red Raspberries (180d) fresh (frozen)
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

-- 5) Demo purchase history: completed trips, so the History tab and its spend
--    insights have something to show on a fresh reset.
--    Clear first for idempotent re-runs (items cascade with the header row).
delete from public.purchase_history
where user_id = '11111111-1111-1111-1111-111111111111';

-- Trips across all three stores, spread over the last ~6 months and dated relative
-- to now(), so the History insights have month-over-month and per-store spending to
-- aggregate on every reset. Offsets and quantities vary so no two months total the
-- same and no month is empty.
with trip_spec as (
  select * from (values
    -- store,        days ago, product offset, qty per line
    ('ALDI',         3,        0,              2),
    ('Publix',       10,       4,              1),
    ('Whole Foods',  16,       8,              1),
    ('ALDI',         24,       12,             3),
    ('Publix',       38,       0,              2),
    ('ALDI',         52,       16,             1),
    ('Whole Foods',  67,       4,              2),
    ('Publix',       81,       20,             1),
    ('ALDI',         96,       8,              2),
    ('Whole Foods',  112,      12,             1),
    ('Publix',       134,      16,             3),
    ('ALDI',         158,      20,             1)
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
  returning id, store_id, purchased_at
)
-- added_to_inventory stands in for the per-item choice the real completion flow
-- copies from shopping_list_items.add_to_inventory: shelf-stable goods get tracked
-- into the pantry, short-life perishables and non-food (NULL shelf life) do not.
insert into public.purchase_history_items
  (purchase_id, product_id, quantity, price_paid, added_to_inventory)
select t.id, p.product_id, p.quantity, p.price, coalesce(pr.shelf_life_days, 0) >= 30
from trip t
-- Keyed on the date as well as the store: a store now has several trips, and
-- matching on store alone would give each of them every other trip's items.
-- now() is transaction-stable, so both sides compute the same timestamp.
join picked p
  on p.store_id = t.store_id
 and now() - make_interval(days => p.days_ago) = t.purchased_at
join public.products pr on pr.id = p.product_id;

-- 6) Pantry <-> history alignment. Sections 3 and 5 pick disjoint product
--    sets, so no pantry lot would get a consumption rate and the nightly
--    adjustment job would have nothing visible to do. Add five pantry
--    staples to the two ALDI trips 93 days apart; the estimator then derives
--    a history rate of (4 - 2) / 93 = 0.0215/day for each, and the apply run
--    visibly adjusts those lots. Idempotent because section 5 recreates
--    purchase_history (items cascade) on every run.
insert into public.purchase_history_items
  (purchase_id, product_id, quantity, price_paid, added_to_inventory)
select ph.id, p.id, v.qty, v.price, true
from (values
  ('21125083_EA',  3, 2, 2.49),  -- Mac & Cheese Sauce
  ('21125083_EA', 96, 2, 2.49),
  ('21219491_EA',  3, 2, 4.99),  -- Peanut Butter
  ('21219491_EA', 96, 2, 4.99),
  ('21535597_EA',  3, 2, 3.29),  -- Jasmine Rice pouch
  ('21535597_EA', 96, 2, 3.29),
  ('21469394_EA',  3, 2, 8.99),  -- Jerk Chicken Wings
  ('21469394_EA', 96, 2, 8.99),
  ('21496426_EA',  3, 2, 3.79),  -- Frozen Red Raspberries
  ('21496426_EA', 96, 2, 3.79)
) as v(source_product_id, days_ago, qty, price)
join public.products p on p.source_product_id = v.source_product_id
join public.purchase_history ph
  on ph.user_id = '11111111-1111-1111-1111-111111111111'
 and ph.purchased_at::date = current_date - v.days_ago;

-- 7) A week of automatic adjustments, so the digest (SCRUM-224) has something to
--    show after a reset. Rates come from section 6; the 50-day stamp gives each
--    staple a budget of 1.075, so the job removes one unit per lot. Peanut Butter
--    (1) reaches zero for the zero-stock gate, Jasmine Rice (3 -> 2) lands on its
--    low-stock threshold, and the Mac & Cheese row is undone, so the digest lists
--    four of the five.
select public.estimate_consumption_rates();

update public.inventory_items ii
   set last_auto_adjusted_at = now() - interval '50 days'
  from public.products p
 where p.id = ii.product_id
   and ii.user_id = '11111111-1111-1111-1111-111111111111'
   and p.source_product_id in
       ('21125083_EA', '21219491_EA', '21535597_EA', '21469394_EA', '21496426_EA');

select public.apply_consumption_adjustments();

-- Spread the new rows over the last five days so the digest is not all one day.
with ranked as (
  select a.id, row_number() over (order by a.inventory_item_id) as rn
    from public.inventory_adjustments a
   where a.user_id = '11111111-1111-1111-1111-111111111111'
     and a.reason = 'auto'
)
update public.inventory_adjustments a
   set created_at = now() - ranked.rn * interval '1 day'
  from ranked
 where a.id = ranked.id;

-- Jasmine Rice sits at its threshold after the adjustment, so its row reads "Low".
insert into public.user_product_stock_settings (user_id, product_id, low_stock_threshold)
select '11111111-1111-1111-1111-111111111111', p.id, 2
  from public.products p
 where p.source_product_id = '21535597_EA'
on conflict (user_id, product_id) do update
  set low_stock_threshold = excluded.low_stock_threshold;

-- One row already undone, so the digest and the pantry disagree by one on purpose.
select * from public.undo_inventory_adjustment((
  select a.id
    from public.inventory_adjustments a
    join public.inventory_items ii on ii.id = a.inventory_item_id
    join public.products p on p.id = ii.product_id
   where ii.user_id = '11111111-1111-1111-1111-111111111111'
     and p.source_product_id = '21125083_EA'
     and a.reason = 'auto'
   limit 1));
