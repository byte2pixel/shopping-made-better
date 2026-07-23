GRANT DELETE ON TABLE public.shopping_list_items TO authenticated;
CREATE POLICY "Individuals can delete view their own shopping list items"
  ON public.shopping_list_items
  FOR DELETE
  TO authenticated
  USING (EXISTS(
            SELECT *
            FROM public.shopping_lists
            WHERE shopping_lists.id = shopping_list_items.shopping_list_id
            AND shopping_lists.user_id = auth.uid()
        ))