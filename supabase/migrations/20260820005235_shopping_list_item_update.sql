ALTER TABLE public.shopping_list_items ENABLE ROW LEVEL SECURITY;
GRANT UPDATE ON public.shopping_list_items TO authenticated;
CREATE POLICY "User can update their shopping list items"
ON public.shopping_list_items
FOR UPDATE TO authenticated
USING (
    EXISTS(
        SELECT 1
        FROM public.shopping_lists sl
        WHERE sl.id = shopping_list_items.shopping_list_id
        AND sl.user_id = auth.uid()
        ))
WITH CHECK (
    EXISTS(
        SELECT 1
        FROM public.shopping_lists sl
        WHERE sl.id = shopping_list_items.shopping_list_id
        AND sl.user_id = auth.uid()
))