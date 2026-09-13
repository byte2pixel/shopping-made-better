ALTER TABLE public.shopping_lists
ADD COLUMN sort_order integer NOT NULL DEFAULT 0;

WITH numberedlist AS (
    SELECT id, ROW_NUMBER() OVER(
        ORDER BY created_at DESC, id
    ) -1 AS new_order
    FROM public.shopping_lists
)
UPDATE public.shopping_lists list
SET sort_order = numberedlist.new_order
FROM numberedlist
WHERE list.id = numberedlist.id