CREATE OR REPLACE FUNCTION public.sortListByUpdatedAt()
RETURNS void
LANGUAGE sql
AS $$
WITH numberedlist AS (
    SELECT id, ROW_NUMBER() OVER(
        ORDER BY updated_at DESC, id
    ) -1 AS new_order
    FROM public.shopping_lists
    WHERE user_id = auth.uid()
)
UPDATE public.shopping_lists list
SET sort_order = numberedlist.new_order
FROM numberedlist
WHERE list.id = numberedlist.id
$$;
