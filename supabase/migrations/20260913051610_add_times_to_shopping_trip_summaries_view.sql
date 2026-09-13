DROP VIEW IF EXISTS public.shopping_trip_summaries;
create view public.shopping_trip_summaries as
select
  sl.id   as shopping_list_id,
  sl.name as list_name,
  sl.sort_order as sort_order,
  sl.created_at as created_at,
  sl.updated_at as updated_at,
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
group by sl.id, sl.name,sl.sort_order, sl.updated_at, sl.created_at, s.id, s.name;

-- Let the app's anon key READ the view (same idea as the stores grant migration
-- 20260704120000_grant_public_read_stores.sql).
grant select on public.shopping_trip_summaries to authenticated;
alter view public.shopping_trip_summaries set (security_invoker = true);
