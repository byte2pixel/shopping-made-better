create view public.shopping_list_item_named_view with (security_invoker = on) as
 SELECT li.id,
    li.shopping_list_id AS shopping_id,
    li.product_id,
    li.quantity::integer AS quantity,
    li.is_checked,
    p.title
   FROM shopping_list_items li
     JOIN products p ON p.id = li.product_id;