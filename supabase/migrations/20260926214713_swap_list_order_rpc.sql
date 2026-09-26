CREATE OR REPLACE function public.swap_list_order_rpc(list_id_1 uuid, sort_order_1 integer,  list_id_2 uuid, sort_order_2 integer)
returns void
language sql
security invoker
AS $$
    UPDATE shopping_lists
    SET sort_order = sort_order_2
    WHERE id = list_id_1
    AND user_id = auth.uid();

    UPDATE shopping_lists
    SET sort_order = sort_order_1
    WHERE id = list_id_2
    AND user_id = auth.uid();
 $$;
