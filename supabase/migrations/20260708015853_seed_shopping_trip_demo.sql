-- === Shopping-trip demo data + summary view =================================
-- Fixed UUID so we can find/remove this user later. (Any valid uuid works.)
-- demo user id: 11111111-1111-1111-1111-111111111111

-- 1) Seed a confirmed auth user. Direct inserts into auth.* are unusual, but it's
--    the only way to get a user without going through the app. Password = 'password123'.
insert into auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  raw_app_meta_data, raw_user_meta_data
)
values (
  '00000000-0000-0000-0000-000000000000',
  '11111111-1111-1111-1111-111111111111',
  'authenticated', 'authenticated',
  'demo@shoppingmadebetter.test',
  crypt('password123', gen_salt('bf')),           -- pgcrypto is available on Supabase
  now(), now(), now(),
  '{"provider":"email","providers":["email"]}',
  '{"display_name":"Demo Shopper"}'               -- so the profile trigger has a name
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
  '{"sub":"11111111-1111-1111-1111-111111111111","email":"demo@shoppingmadebetter.test"}',
  'email', now(), now(), now()
)
on conflict do nothing;

-- 2) Ensure the profile row exists. The handle_new_user() trigger may already have
--    created it from the metadata above; this is a safe fallback either way.
insert into public.profiles (id, display_name)
values ('11111111-1111-1111-1111-111111111111', 'Demo Shopper')
on conflict (id) do nothing;

-- 3) One shopping list ("trip") per store, for two stores.
insert into public.shopping_lists (user_id, store_id, name)
select '11111111-1111-1111-1111-111111111111', s.id, s.name || ' Weekly'
from public.stores s
where s.name in ('ALDI', 'Publix');

-- 4) Put 5 real, currently-priced products on each list, so totals are non-zero.
--    LATERAL picks 5 products that actually have current pricing AT THAT STORE.
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

-- 5) The summary view your app reads: one row per trip, item count + total cost.
create view public.shopping_trip_summaries as
select
  sl.id   as shopping_list_id,
  sl.name as list_name,
  s.id    as store_id,
  s.name  as store_name,
  count(sli.id) as item_count,
  coalesce(sum(spp.price * sli.quantity), 0) as total_cost
from public.shopping_lists sl
join public.stores s on s.id = sl.store_id
left join public.shopping_list_items sli on sli.shopping_list_id = sl.id
left join public.store_product_pricing spp
       on spp.product_id = sli.product_id
      and spp.store_id   = sl.store_id
      and spp.is_current = true
group by sl.id, sl.name, s.id, s.name;

-- 6) Let the app's anon key READ the view (same idea as the stores grant migration
--    20260704120000_grant_public_read_stores.sql). The view exposes only the summary,
--    never the raw per-user rows.
grant select on public.shopping_trip_summaries to anon, authenticated;