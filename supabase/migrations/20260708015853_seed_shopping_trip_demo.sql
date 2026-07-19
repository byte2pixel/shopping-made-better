create view public.shopping_trip_summaries with (security_invoker = on) as
 SELECT sl.id AS shopping_list_id,
    sl.name AS list_name,
    s.id AS store_id,
    s.name AS store_name,
    count(sli.id) AS item_count,
    COALESCE(sum(spp.price * sli.quantity), 0::numeric) AS total_cost
   FROM shopping_lists sl
     JOIN stores s ON s.id = sl.store_id
     LEFT JOIN shopping_list_items sli ON sli.shopping_list_id = sl.id
     LEFT JOIN store_product_pricing spp ON spp.product_id = sli.product_id AND spp.store_id = sl.store_id AND spp.is_current = true
  GROUP BY sl.id, sl.name, s.id, s.name;

