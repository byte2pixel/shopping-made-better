-- === Shopping-trip summary view ============================================
-- Schema only. The demo user and the demo shopping-trip DATA that this view
-- summarizes now live in supabase/dummy_account.sql (loaded from seed.sql),
-- so they can be removed without touching schema -- and so their SELECTs run
-- AFTER seed.sql has populated stores/products/pricing (during the migration
-- phase those tables are still empty, so the inserts matched zero rows here).

-- One row per trip: item count + total cost. The view exposes only the
-- summary, never the raw per-user rows.
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

-- Let the app's anon key READ the view (same idea as the stores grant migration
-- 20260704120000_grant_public_read_stores.sql).
grant select on public.shopping_trip_summaries to anon, authenticated;